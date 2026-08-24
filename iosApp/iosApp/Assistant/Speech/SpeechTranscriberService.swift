import AVFoundation
import Foundation
import Speech

/// Abstraction over the on-device speech-to-text engine so the dictation
/// capture flow stays testable and can fall back between engines.
///
/// Implementations stream live partial transcripts through [partialHandler]
/// while recording, and return the full finalized transcript from `finish()`.
@MainActor
protocol SpeechTranscribing: AnyObject {
    /// Called on the main actor with the cumulative transcript so far,
    /// including volatile (non-final) segments.
    var partialHandler: ((String) -> Void)? { get set }

    /// Called when the engine fails mid-recording.
    var failureHandler: ((String) -> Void)? { get set }

    /// Prepares the session, starts the audio pipeline and begins emitting
    /// partials. Throws when the engine cannot start (permissions are checked
    /// by the caller beforehand).
    func start() async throws

    /// Ends input, waits for the final transcript and stops the audio
    /// pipeline. Returns the complete finalized transcript.
    func finish() async throws -> String

    /// Aborts recognition and releases the microphone without producing a
    /// final transcript.
    func cancel() async
}

/// Result of the pt-PT asset pre-flight for the iOS 26 SpeechAnalyzer path.
enum SpeechAssetPreparation: Equatable {
    /// Assets installed; SpeechAnalyzer usable.
    case ready
    /// The pt-PT model exists for this device but still needs downloading.
    case needsDownload
    /// The device cannot run SpeechAnalyzer for pt-PT at all.
    case localeUnsupported
    /// Download or reservation failed; carries the reason.
    case failed(String)

    /// Human-readable hint shown by the capture screen.
    var userHint: String? {
        switch self {
        case .ready: return nil
        case .needsDownload:
            return "The Português (Portugal) speech model still needs to be downloaded."
        case .localeUnsupported:
            return "This device does not support on-device speech recognition for Português (Portugal)."
        case .failed(let reason): return reason
        }
    }
}

/// Factory choosing the best available transcriber engine.
///
/// Prefers the iOS 26 SpeechAnalyzer pipeline (on-device, streaming volatile
/// results) whenever the device supports pt-PT there; otherwise falls back to
/// the classic `SFSpeechRecognizer` pipeline (`DictationTranscriber`).
enum SpeechTranscriberService {
    /// Locale dictated sessions are captured in.
    static let dictationLocale = Locale(identifier: "pt-PT")

    /// Runs the AssetInventory pre-flight for the dictation locale.
    ///
    /// When the model is present-but-not-installed this reserves the locale
    /// and downloads it, surfacing download progress through `onProgress`.
    @MainActor
    static func prepareAssets(
        onProgress: ((Double) -> Void)? = nil
    ) async -> SpeechAssetPreparation {
        guard #available(iOS 26.0, *) else {
            return .localeUnsupported
        }
        guard
            (try? await SpeechTranscriber.supportedLocale(equivalentTo: dictationLocale)) != nil
        else {
            return .localeUnsupported
        }
        let transcriber = SpeechTranscriber(
            locale: dictationLocale,
            transcriptionOptions: [],
            reportingOptions: [.volatileResults],
            attributeOptions: []
        )
        let status = try await AssetInventory.status(forModules: [transcriber])
        if status == .installed {
            return .ready
        }
        if status == .unsupported {
            return .localeUnsupported
        }
        do {
            _ = try await AssetInventory.reserve(locale: dictationLocale)
            guard
                let request = try await AssetInventory.assetInstallationRequest(supporting: [transcriber])
            else {
                return .ready
            }
            let observer = ProgressObserver(progress: request.progress) { fraction in
                onProgress?(fraction)
            }
            defer { observer.invalidate() }
            try await request.downloadAndInstall()
            return .ready
        } catch {
            return .failed(error.localizedDescription)
        }
    }

    /// Builds the concrete transcriber for this device.
    @MainActor
    static func make() async -> any SpeechTranscribing {
        if #available(iOS 26.0, *) {
            let preparation = await prepareAssets()
            if preparation == .ready {
                return SpeechAnalyzerTranscriber(locale: dictationLocale)
            }
        }
        return DictationTranscriber(localeIdentifier: dictationLocale.identifier)
    }
}

// MARK: - iOS 26 SpeechAnalyzer pipeline

/// iOS 26 `SpeechAnalyzer` + `SpeechTranscriber` implementation.
///
/// Streams microphone buffers straight into the analyzer and forwards both
/// volatile (in-progress) and final results. `finalizeAndFinishThroughEndOfInput`
/// flushes trailing audio so the last words are never cut off.
@available(iOS 26.0, *)
@MainActor
final class SpeechAnalyzerTranscriber: SpeechTranscribing {
    private let locale: Locale

    private var analyzer: SpeechAnalyzer?
    private var transcriber: SpeechTranscriber?
    private let engine = AVAudioEngine()
    private var bufferStream: AsyncStream<AVAudioPCMBuffer>.Continuation?
    private var pumpTask: Task<Void, Never>?
    private var resultsTask: Task<Void, Never>?

    private var confirmedText = ""
    private(set) var finalText = ""

    var partialHandler: ((String) -> Void)?
    var failureHandler: ((String) -> Void)?

    init(locale: Locale) {
        self.locale = locale
    }

    func start() async throws {
        let transcriber = SpeechTranscriber(
            locale: locale,
            transcriptionOptions: [],
            reportingOptions: [.volatileResults],
            attributeOptions: []
        )
        self.transcriber = transcriber

        let analyzer = SpeechAnalyzer(modules: [transcriber], options: nil)
        self.analyzer = analyzer

        configureAudioSession()

        var continuation: AsyncStream<AVAudioPCMBuffer>.Continuation!
        let stream = AsyncStream(
            AVAudioPCMBuffer.self,
            bufferingPolicy: .bufferingNewest(64)
        ) { continuation = $0 }
        self.bufferStream = continuation

        // Pump microphone buffers into the analyzer until the stream ends.
        pumpTask = Task { [analyzer] in
            let mapped = stream.map { Speech.AnalyzerInput(buffer: $0) }
            try? await analyzer.start(inputSequence: mapped)
        }

        resultsTask = Task { [weak self] in
            guard let self else { return }
            do {
                for try await result in transcriber.results {
                    try Task.checkCancellation()
                    let text = String(result.text.characters)
                    if result.isFinal {
                        self.confirmedText += text
                    }
                    self.partialHandler?(self.confirmedText + (result.isFinal ? "" : text))
                }
                self.finalText = self.confirmedText.trimmingCharacters(in: .whitespacesAndNewlines)
            } catch is CancellationError {
                // Cancelled by the user; nothing to report.
            } catch {
                self.failureHandler?(error.localizedDescription)
            }
        }

        let input = engine.inputNode
        let format = input.outputFormat(forBus: 0)
        input.installTap(onBus: 0, bufferSize: 4096, format: format) { [continuation] buffer, _ in
            if let pcmBuffer = buffer as? AVAudioPCMBuffer {
                continuation?.yield(pcmBuffer)
            }
        }
        engine.prepare()
        try engine.start()
    }

    func finish() async throws -> String {
        defer { teardownAudioPipeline() }
        bufferStream?.finish()
        try await analyzer?.finalizeAndFinishThroughEndOfInput()
        if let resultsTask {
            await resultsTask.value
        }
        return finalText.isEmpty
            ? confirmedText.trimmingCharacters(in: .whitespacesAndNewlines)
            : finalText
    }

    func cancel() async {
        teardownAudioPipeline()
        await analyzer?.cancelAndFinishNow()
        resultsTask?.cancel()
        resultsTask = nil
        pumpTask?.cancel()
        pumpTask = nil
    }

    private func configureAudioSession() {
        let session = AVAudioSession.sharedInstance()
        try? session.setCategory(.playAndRecord, mode: .measurement, options: [.defaultToSpeaker])
        try? session.setActive(true)
    }

    private func teardownAudioPipeline() {
        engine.inputNode.removeTap(onBus: 0)
        engine.stop()
        bufferStream?.finish()
        bufferStream = nil
        analyzer = nil
        transcriber = nil
        try? AVAudioSession.sharedInstance().setActive(false, options: .notifyOthersOnDeactivation)
    }
}

/// Forwards `Progress.fractionCompleted` snapshots to a main-actor closure.
private final class ProgressObserver {
    private let progress: Progress
    private let handler: (Double) -> Void
    private var observation: NSKeyValueObservation?

    init(progress: Progress, handler: @escaping (Double) -> Void) {
        self.progress = progress
        self.handler = handler
        observation = progress.observe(\.fractionCompleted) { observed, _ in
            handler(observed.fractionCompleted)
        }
    }

    func invalidate() {
        observation?.invalidate()
        observation = nil
    }

    deinit {
        observation?.invalidate()
    }
}

// MARK: - Legacy SFSpeechRecognizer fallback ("DictationTranscriber")

/// Classic `SFSpeechRecognizer` pipeline used when SpeechAnalyzer is
/// unavailable (pre-iOS 26 devices or missing pt-PT support).
@MainActor
final class DictationTranscriber: NSObject, SpeechTranscribing {
    private let localeIdentifier: String

    private var recognizer: SFSpeechRecognizer?
    private var request: SFSpeechAudioBufferRecognitionRequest?
    private var task: SFSpeechRecognitionTask?
    private let engine = AVAudioEngine()
    private var finishContinuation: CheckedContinuation<Void, Never>?

    private var latestTranscript = ""
    private(set) var finalText = ""

    var partialHandler: ((String) -> Void)?
    var failureHandler: ((String) -> Void)?

    init(localeIdentifier: String) {
        self.localeIdentifier = localeIdentifier
        super.init()
    }

    func start() async throws {
        configureAudioSession()

        let recognizer = SFSpeechRecognizer(locale: Locale(identifier: localeIdentifier))
        guard let recognizer, recognizer.isAvailable else {
            throw DictationTranscriberError.recognizerUnavailable
        }
        self.recognizer = recognizer

        let request = SFSpeechAudioBufferRecognitionRequest()
        request.shouldReportPartialResults = true
        self.request = request

        let input = engine.inputNode
        let format = input.outputFormat(forBus: 0)
        input.installTap(onBus: 0, bufferSize: 4096, format: format) { [request] buffer, _ in
            request.append(buffer)
        }

        engine.prepare()
        try engine.start()

        task = recognizer.recognitionTask(with: request) { [weak self] result, error in
            MainActor.assumeIsolated {
                guard let self else { return }
                if let result {
                    let text = result.bestTranscription.formattedString
                    self.latestTranscript = text
                    self.partialHandler?(text)
                    if result.isFinal {
                        self.finalText = text.trimmingCharacters(in: .whitespacesAndNewlines)
                        self.resumeFinishWaiter()
                    }
                }
                if error != nil {
                    self.failureHandler?(error?.localizedDescription ?? "Speech recognition failed")
                    self.resumeFinishWaiter()
                }
            }
        }
    }

    func finish() async throws -> String {
        defer { teardownAudioPipeline() }
        request?.endAudio()
        engine.stop()
        if task?.state != .completed && task?.state != .canceling {
            await withCheckedContinuation { continuation in
                self.finishContinuation = continuation
                // Safety net: never hang forever on a stuck recognizer.
                DispatchQueue.main.asyncAfter(deadline: .now() + 5) { [weak self] in
                    self?.resumeFinishWaiter()
                }
            }
        }
        return finalText.isEmpty
            ? latestTranscript.trimmingCharacters(in: .whitespacesAndNewlines)
            : finalText
    }

    func cancel() async {
        task?.cancel()
        teardownAudioPipeline()
    }

    private func resumeFinishWaiter() {
        finishContinuation?.resume()
        finishContinuation = nil
    }

    private func configureAudioSession() {
        let session = AVAudioSession.sharedInstance()
        try? session.setCategory(.playAndRecord, mode: .measurement, options: [.defaultToSpeaker])
        try? session.setActive(true)
    }

    private func teardownAudioPipeline() {
        engine.inputNode.removeTap(onBus: 0)
        engine.stop()
        request = nil
        task = nil
        try? AVAudioSession.sharedInstance().setActive(false, options: .notifyOthersOnDeactivation)
    }
}

enum DictationTranscriberError: LocalizedError {
    case recognizerUnavailable
    case microphoneDenied
    case speechPermissionDenied

    var errorDescription: String? {
        switch self {
        case .recognizerUnavailable: return "Speech recognition is not available for this language."
        case .microphoneDenied: return "Microphone access is denied. Enable it in Settings."
        case .speechPermissionDenied: return "Speech recognition is denied. Enable it in Settings."
        }
    }
}

// MARK: - Permissions

/// Single entry point requesting both permissions the dictation flow needs.
enum SpeechAuthService {
    /// Requests microphone + speech-recognition authorization.
    /// Returns `nil` when both granted, otherwise a descriptive error.
    @MainActor
    static func requestAuthorization() async -> Error? {
        let micGranted =
            if #available(iOS 17.0, *) {
                await AVAudioApplication.requestRecordPermission()
            } else {
                await withCheckedContinuation { continuation in
                    AVAudioSession.sharedInstance().requestRecordPermission { granted in
                        continuation.resume(returning: granted)
                    }
                }
            }
        guard micGranted else { return DictationTranscriberError.microphoneDenied }

        let speechStatus = await withCheckedContinuation { continuation in
            SFSpeechRecognizer.requestAuthorization { status in
                continuation.resume(returning: status)
            }
        }
        guard speechStatus == .authorized else {
            return DictationTranscriberError.speechPermissionDenied
        }
        return nil
    }
}
