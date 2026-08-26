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

    /// Absolute path of the completed CAF recording, when the audio writer
    /// was available. Aborted recordings return `nil`.
    var recordingFilePath: String? { get }

    /// Duration of the completed recording in milliseconds, when measurable.
    var recordingDurationMillis: Int64? { get }

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

private struct DictationAudioFileResult {
    let path: String
    let durationMillis: Int64?
}

/// Thread-safe PCM writer used by the audio tap. Speech recognition remains
/// the source of truth for transcription; this writer is a best-effort copy
/// of the same microphone buffers for later review.
private final class DictationAudioFileWriter: @unchecked Sendable {
    private let lock = NSLock()
    private var file: AVAudioFile?
    private var path: String?
    private var sampleRate: Double = 0
    private var frameCount: AVAudioFramePosition = 0

    func start(format: AVAudioFormat) {
        lock.lock()
        defer { lock.unlock() }

        guard
            let documents = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first
        else {
            return
        }
        let directory = documents.appendingPathComponent("dictations", isDirectory: true)
        do {
            try FileManager.default.createDirectory(
                at: directory,
                withIntermediateDirectories: true,
                attributes: nil
            )
            let url = directory.appendingPathComponent("\(UUID().uuidString).caf")
            file = try AVAudioFile(forWriting: url, settings: format.settings)
            path = url.path
            sampleRate = format.sampleRate
            frameCount = 0
        } catch {
            file = nil
            path = nil
        }
    }

    func append(_ buffer: AVAudioPCMBuffer) {
        lock.lock()
        defer { lock.unlock() }
        guard let file else { return }
        do {
            try file.write(from: buffer)
            frameCount += AVAudioFramePosition(buffer.frameLength)
        } catch {
            // A speech session should still work when the optional archive
            // writer cannot accept a buffer.
        }
    }

    func close() -> DictationAudioFileResult? {
        lock.lock()
        let filePath = path
        let frames = frameCount
        let rate = sampleRate
        file = nil
        path = nil
        frameCount = 0
        sampleRate = 0
        lock.unlock()

        guard let filePath, FileManager.default.fileExists(atPath: filePath) else {
            return nil
        }
        let duration = rate > 0 ? Int64((Double(frames) / rate * 1000).rounded()) : nil
        return DictationAudioFileResult(path: filePath, durationMillis: duration)
    }

    func discard() {
        guard let result = close() else { return }
        try? FileManager.default.removeItem(atPath: result.path)
    }
}

/// A ready-to-use transcriber together with the locale it recognizes in.
struct ResolvedDictationEngine {
    let transcriber: any SpeechTranscribing

    /// BCP-47 identifier of the locale the engine actually recognizes,
    /// e.g. "pt-PT" or a degraded fallback like "en-US".
    let localeIdentifier: String

    /// True when recognition runs in a language other than the preferred
    /// dictation locale (graceful degradation).
    var usesFallbackLocale: Bool {
        localeIdentifier != SpeechTranscriberService.dictationLocale.identifier
    }

    /// Human-readable language name for hints, e.g. "English".
    var localeDisplayName: String {
        let languageCode =
            Locale(identifier: localeIdentifier).language.languageCode?.identifier
            ?? localeIdentifier
        return Locale(identifier: languageCode).localizedString(forIdentifier: languageCode)
            ?? localeIdentifier
    }
}

/// Factory choosing the best available transcriber engine.
///
/// Preference order, degrading gracefully — dictation is never blocked:
/// 1. iOS 26 SpeechAnalyzer in the preferred pt-PT locale, downloading its
///    speech assets first when the platform offers that.
/// 2. iOS 26 SpeechAnalyzer in the best installed alternative locale
///    (en-US preferred, then any other supported locale).
/// 3. Classic `SFSpeechRecognizer` pipeline (`DictationTranscriber`), whose
///    locale support is broader than the SpeechAnalyzer asset catalog.
enum SpeechTranscriberService {
    /// Locale dictated sessions are captured in.
    static let dictationLocale = Locale(identifier: "pt-PT")

    /// User-facing explanation when neither native speech pipeline can start.
    static let unavailableMessage =
        "Speech recognition is unavailable right now. Check Speech Recognition permissions and make sure a supported language is downloaded."

    /// Resolves the best usable dictation engine for this device.
    @MainActor
    static func resolve() async -> ResolvedDictationEngine? {
        if DictationTestConfiguration.isEnabled {
            return ResolvedDictationEngine(
                transcriber: MockSpeechTranscriber(),
                localeIdentifier: dictationLocale.identifier
            )
        }
        if #available(iOS 26.0, *) {
            if let engine = await analyzerEngine(for: dictationLocale, allowDownload: true) {
                return engine
            }
            for candidate in await fallbackAnalyzerLocales()
            where candidate.identifier != dictationLocale.identifier {
                if let engine = await analyzerEngine(for: candidate, allowDownload: false) {
                    return engine
                }
            }
        }
        return legacyEngine()
    }

    /// Builds a SpeechAnalyzer engine for `locale` when its speech assets are
    /// usable, optionally downloading them first. Returns nil when the locale
    /// is unsupported or its assets cannot be made available, so the caller
    /// can degrade to the next option.
    @available(iOS 26.0, *)
    @MainActor
    private static func analyzerEngine(
        for locale: Locale,
        allowDownload: Bool
    ) async -> ResolvedDictationEngine? {
        guard
            let supported = await SpeechTranscriber.supportedLocale(equivalentTo: locale)
        else {
            return nil
        }
        let transcriber = SpeechTranscriber(
            locale: supported,
            transcriptionOptions: [],
            reportingOptions: [.volatileResults],
            attributeOptions: []
        )
        let status = await AssetInventory.status(forModules: [transcriber])
        if status == .unsupported {
            return nil
        }

        func engine() -> ResolvedDictationEngine {
            ResolvedDictationEngine(
                transcriber: SpeechAnalyzerTranscriber(locale: supported),
                localeIdentifier: supported.identifier
            )
        }

        if status == .installed {
            return engine()
        }
        // Only the preferred locale gets an asset download; fallback locales
        // must already be installed.
        guard allowDownload else { return nil }

        do {
            _ = try await AssetInventory.reserve(locale: supported)
            guard
                let request = try await AssetInventory.assetInstallationRequest(supporting: [transcriber])
            else {
                return engine()
            }
            try await request.downloadAndInstall()
            return engine()
        } catch {
            // Download failed or unavailable → degrade silently.
            return nil
        }
    }

    /// Alternative SpeechAnalyzer locales to try, best first: en-US, then
    /// every other supported locale in deterministic order.
    @available(iOS 26.0, *)
    private static func fallbackAnalyzerLocales() async -> [Locale] {
        let englishUS = Locale(identifier: "en-US")
        let rest = await SpeechTranscriber.supportedLocales
            .filter { $0 != englishUS }
            .sorted { $0.identifier < $1.identifier }
        return [englishUS] + rest
    }

    /// Classic `SFSpeechRecognizer` fallback with broader locale coverage:
    /// the preferred locale when recognized, else en-US, else whatever the
    /// system lists first.
    @MainActor
    private static func legacyEngine() -> ResolvedDictationEngine? {
        let supportedIdentifiers = SFSpeechRecognizer.supportedLocales().map(\.identifier)
        let preferred = dictationLocale.identifier
        let candidates =
            [preferred, "en-US"] + supportedIdentifiers.filter { $0 != preferred && $0 != "en-US" }
        for identifier in candidates {
            guard
                let recognizer = SFSpeechRecognizer(locale: Locale(identifier: identifier)),
                recognizer.isAvailable
            else {
                continue
            }
            return ResolvedDictationEngine(
                transcriber: DictationTranscriber(localeIdentifier: identifier),
                localeIdentifier: identifier
            )
        }
        return nil
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
    private var audioWriter: DictationAudioFileWriter?
    private var keepCompletedRecording = false

    private(set) var recordingFilePath: String?
    private(set) var recordingDurationMillis: Int64?

    var partialHandler: ((String) -> Void)?
    var failureHandler: ((String) -> Void)?

    init(locale: Locale) {
        self.locale = locale
    }

    func start() async throws {
        recordingFilePath = nil
        recordingDurationMillis = nil
        keepCompletedRecording = false
        audioWriter = DictationAudioFileWriter()
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
        pumpTask = Task { [weak self, analyzer] in
            do {
                let mapped = stream.map { Speech.AnalyzerInput(buffer: $0) }
                try await analyzer.start(inputSequence: mapped)
            } catch is CancellationError {
                // Expected when the user cancels or finishes the recording.
            } catch {
                self?.failureHandler?(error.localizedDescription)
            }
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
        audioWriter?.start(format: format)
        let audioWriter = self.audioWriter
        input.installTap(onBus: 0, bufferSize: 4096, format: format) { [continuation, audioWriter] buffer, _ in
            audioWriter?.append(buffer)
            if let pcmBuffer = buffer as? AVAudioPCMBuffer {
                continuation?.yield(pcmBuffer)
            }
        }
        engine.prepare()
        do {
            try engine.start()
        } catch {
            teardownAudioPipeline()
            throw error
        }
    }

    func finish() async throws -> String {
        keepCompletedRecording = true
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
        let analyzer = self.analyzer
        resultsTask?.cancel()
        pumpTask?.cancel()
        await analyzer?.cancelAndFinishNow()
        resultsTask = nil
        pumpTask = nil
        teardownAudioPipeline()
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
        if keepCompletedRecording {
            if let result = audioWriter?.close() {
                recordingFilePath = result.path
                recordingDurationMillis = result.durationMillis
            }
        } else {
            audioWriter?.discard()
        }
        audioWriter = nil
        keepCompletedRecording = false
        try? AVAudioSession.sharedInstance().setActive(false, options: .notifyOthersOnDeactivation)
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
    private var audioWriter: DictationAudioFileWriter?
    private var keepCompletedRecording = false

    private(set) var recordingFilePath: String?
    private(set) var recordingDurationMillis: Int64?

    var partialHandler: ((String) -> Void)?
    var failureHandler: ((String) -> Void)?

    init(localeIdentifier: String) {
        self.localeIdentifier = localeIdentifier
        super.init()
    }

    func start() async throws {
        recordingFilePath = nil
        recordingDurationMillis = nil
        keepCompletedRecording = false
        audioWriter = DictationAudioFileWriter()
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
        audioWriter?.start(format: format)
        let audioWriter = self.audioWriter
        input.installTap(onBus: 0, bufferSize: 4096, format: format) { [request, audioWriter] buffer, _ in
            audioWriter?.append(buffer)
            request.append(buffer)
        }

        engine.prepare()
        do {
            try engine.start()
        } catch {
            teardownAudioPipeline()
            throw error
        }

        task = recognizer.recognitionTask(with: request) { [weak self] result, error in
            // SFSpeechRecognizer does not guarantee which queue invokes this
            // callback. Hop explicitly to the actor instead of assuming the
            // callback is already on the main actor.
            Task { @MainActor [weak self] in
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
        keepCompletedRecording = true
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
        if keepCompletedRecording {
            if let result = audioWriter?.close() {
                recordingFilePath = result.path
                recordingDurationMillis = result.durationMillis
            }
        } else {
            audioWriter?.discard()
        }
        audioWriter = nil
        keepCompletedRecording = false
        try? AVAudioSession.sharedInstance().setActive(false, options: .notifyOthersOnDeactivation)
    }
}

// MARK: - Deterministic UI-test transcriber

/// Microphone-free transcriber used only when the UI test launch argument is
/// present. It exercises the real capture/review/extraction/save orchestration
/// without depending on simulator audio routing or speech assets.
@MainActor
final class MockSpeechTranscriber: SpeechTranscribing {
    private let transcript: String
    private var isRecording = false

    let recordingFilePath: String? = nil
    let recordingDurationMillis: Int64? = nil

    var partialHandler: ((String) -> Void)?
    var failureHandler: ((String) -> Void)?

    init(transcript: String = "Registar o peso do Thunder e uma desparasitação. Depois fazer uma ecografia à Fantasma Inexistente.") {
        self.transcript = transcript
    }

    func start() async throws {
        isRecording = true
        partialHandler?(transcript)
    }

    func finish() async throws -> String {
        guard isRecording else { return "" }
        isRecording = false
        return transcript
    }

    func cancel() async {
        isRecording = false
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
        if DictationTestConfiguration.isEnabled {
            return nil
        }
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
