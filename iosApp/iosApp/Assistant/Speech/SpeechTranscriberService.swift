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
    private var writeFailed = false

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
            writeFailed = false
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
            writeFailed = true
        }
    }

    func close() -> DictationAudioFileResult? {
        lock.lock()
        let filePath = path
        let frames = frameCount
        let rate = sampleRate
        let failed = writeFailed
        file = nil
        path = nil
        frameCount = 0
        sampleRate = 0
        writeFailed = false
        lock.unlock()

        guard
            let filePath,
            !failed,
            frames > 0,
            FileManager.default.fileExists(atPath: filePath),
            let readableFile = try? AVAudioFile(forReading: URL(fileURLWithPath: filePath)),
            readableFile.length > 0
        else {
            if let filePath {
                try? FileManager.default.removeItem(atPath: filePath)
            }
            return nil
        }
        let readableRate = readableFile.processingFormat.sampleRate
        let durationRate = readableRate > 0 ? readableRate : rate
        let duration = durationRate > 0
            ? Int64((Double(readableFile.length) / durationRate * 1000).rounded())
            : nil
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

    /// BCP-47 identifier requested by the user for this capture.
    let preferredLocaleIdentifier: String

    /// True when recognition runs in a language other than the preferred
    /// dictation locale (graceful degradation).
    var usesFallbackLocale: Bool {
        localeIdentifier != preferredLocaleIdentifier
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
/// Preference order, degrading gracefully when a supported local engine exists:
/// 1. iOS 26 SpeechAnalyzer in the selected language, downloading its speech
///    assets when the platform offers that.
/// 2. Classic `SFSpeechRecognizer` in the selected language or a matching
///    regional variant, restricted to on-device recognition.
///
/// The resolver never silently changes an English request into another
/// language. When SpeechAnalyzer is available, its legacy engine is retained
/// as a startup/runtime fallback for devices whose microphone route cannot be
/// converted to the analyzer format.
enum SpeechTranscriberService {
    /// Locale dictated sessions are captured in.
    static let dictationLocale = Locale(identifier: "pt-PT")

    /// User-facing explanation when neither native speech pipeline can start.
    static let unavailableMessage =
        "Speech recognition is unavailable right now. Check Speech Recognition permissions and make sure a supported language is downloaded."

    /// Resolves the best usable dictation engine for this device.
    @MainActor
    static func resolve(
        preferredLocale: Locale = dictationLocale
    ) async -> ResolvedDictationEngine? {
        if DictationTestConfiguration.isEnabled {
            return ResolvedDictationEngine(
                transcriber: MockSpeechTranscriber(),
                localeIdentifier: preferredLocale.identifier,
                preferredLocaleIdentifier: preferredLocale.identifier,
            )
        }
        let legacy = legacyEngine(for: preferredLocale)
        if #available(iOS 26.0, *) {
            for (index, candidate) in await analyzerLocales(for: preferredLocale).enumerated() {
                if let engine = await analyzerEngine(for: candidate, allowDownload: false) {
                    return withLegacyFallback(
                        analyzer: engine,
                        legacy: legacy,
                        preferredLocale: preferredLocale,
                    )
                }
                if index == 0, let engine = await analyzerEngine(for: candidate, allowDownload: true) {
                    return withLegacyFallback(
                        analyzer: engine,
                        legacy: legacy,
                        preferredLocale: preferredLocale,
                    )
                }
            }
        }
        return legacy
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
                localeIdentifier: supported.identifier,
                preferredLocaleIdentifier: locale.identifier,
            )
        }

        if status == .installed {
            // SpeechAnalyzer does not convert microphone audio for us. If the
            // installed assets cannot provide a compatible analyzer format,
            // let the resolver continue to the legacy engine instead of
            // starting a pipeline that will fail when its first buffer arrives.
            guard await SpeechAnalyzer.bestAvailableAudioFormat(compatibleWith: [transcriber]) != nil else {
                return nil
            }
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
                guard await SpeechAnalyzer.bestAvailableAudioFormat(compatibleWith: [transcriber]) != nil else {
                    return nil
                }
                return engine()
            }
            try await request.downloadAndInstall()
            guard await SpeechAnalyzer.bestAvailableAudioFormat(compatibleWith: [transcriber]) != nil else {
                return nil
            }
            return engine()
        } catch {
            // Download failed or unavailable → degrade silently.
            return nil
        }
    }

    /// SpeechAnalyzer locales matching the selected language, with the exact
    /// requested locale first and regional variants as installed fallbacks.
    @available(iOS 26.0, *)
    private static func analyzerLocales(for preferred: Locale) async -> [Locale] {
        let languageCode = preferred.language.languageCode?.identifier
        let exact = preferred.identifier
        let rest = await SpeechTranscriber.supportedLocales
            .filter {
                $0.identifier != exact &&
                    (languageCode == nil || $0.language.languageCode?.identifier == languageCode)
            }
            .sorted { $0.identifier < $1.identifier }
        return [preferred] + rest
    }

    /// Classic `SFSpeechRecognizer` fallback with broader locale coverage:
    /// the preferred locale when recognized, else a matching regional variant.
    @MainActor
    private static func legacyEngine(for preferredLocale: Locale) -> ResolvedDictationEngine? {
        let supportedIdentifiers = SFSpeechRecognizer.supportedLocales().map(\.identifier)
        let preferred = preferredLocale.identifier
        let languageCode = preferredLocale.language.languageCode?.identifier
        let candidates =
            [preferred] +
            supportedIdentifiers
                .filter { identifier -> Bool in
                    identifier != preferred &&
                        (languageCode == nil || Locale(identifier: identifier).language.languageCode?.identifier == languageCode)
                }
                .sorted()
        for identifier in candidates {
            guard
                let recognizer = SFSpeechRecognizer(locale: Locale(identifier: identifier)),
                recognizer.isAvailable,
                recognizer.supportsOnDeviceRecognition
            else {
                continue
            }
            return ResolvedDictationEngine(
                transcriber: DictationTranscriber(localeIdentifier: identifier),
                localeIdentifier: identifier,
                preferredLocaleIdentifier: preferred,
            )
        }
        return nil
    }

    @MainActor
    private static func withLegacyFallback(
        analyzer: ResolvedDictationEngine,
        legacy: ResolvedDictationEngine?,
        preferredLocale: Locale,
    ) -> ResolvedDictationEngine {
        guard let legacy else { return analyzer }
        return ResolvedDictationEngine(
            transcriber: ResilientSpeechTranscriber(
                primary: analyzer.transcriber,
                fallback: { legacy.transcriber },
            ),
            localeIdentifier: analyzer.localeIdentifier,
            preferredLocaleIdentifier: preferredLocale.identifier,
        )
    }
}

// MARK: - iOS 26 SpeechAnalyzer pipeline

private enum SpeechAnalyzerTranscriberError: LocalizedError {
    case analyzerFormatUnavailable
    case audioConverterUnavailable
    case audioConversionFailed

    var errorDescription: String? {
        switch self {
        case .analyzerFormatUnavailable:
            return "Speech recognition cannot find a compatible audio format on this device."
        case .audioConverterUnavailable, .audioConversionFailed:
            return "Speech recognition could not prepare the microphone audio."
        }
    }
}

/// Keeps a legacy recognizer ready when the iOS 26 analyzer cannot start or
/// loses its audio conversion pipeline. The fallback is deliberately scoped
/// to the same requested language; changing languages is a user choice, not a
/// hidden recovery side effect.
@MainActor
final class ResilientSpeechTranscriber: SpeechTranscribing {
    private let primary: any SpeechTranscribing
    private let fallbackFactory: () -> (any SpeechTranscribing)?
    private var active: (any SpeechTranscribing)?
    private var isSwitching = false
    private var hasFinished = false

    var partialHandler: ((String) -> Void)?
    var failureHandler: ((String) -> Void)?

    var recordingFilePath: String? { active?.recordingFilePath }
    var recordingDurationMillis: Int64? { active?.recordingDurationMillis }

    init(
        primary: any SpeechTranscribing,
        fallback: @escaping () -> (any SpeechTranscribing)?,
    ) {
        self.primary = primary
        self.fallbackFactory = fallback
    }

    func start() async throws {
        hasFinished = false
        isSwitching = false
        active = primary
        bindHandlers(to: primary)
        do {
            try await primary.start()
        } catch {
            await primary.cancel()
            try await startFallback(orThrow: error)
        }
    }

    func finish() async throws -> String {
        hasFinished = true
        guard let active else { return "" }
        return try await active.finish()
    }

    func cancel() async {
        hasFinished = true
        isSwitching = false
        await active?.cancel()
    }

    private func bindHandlers(to transcriber: any SpeechTranscribing) {
        transcriber.partialHandler = { [weak self] text in
            self?.partialHandler?(text)
        }
        transcriber.failureHandler = { [weak self] message in
            self?.handleFailure(message)
        }
    }

    private func handleFailure(_ message: String) {
        guard isPrimaryActive, !isSwitching, !hasFinished else {
            failureHandler?(message)
            return
        }
        isSwitching = true
        Task { @MainActor [weak self] in
            guard let self, !self.hasFinished else { return }
            await self.primary.cancel()
            do {
                try await self.startFallback(orThrow: nil)
            } catch {
                self.failureHandler?(error.localizedDescription)
            }
            self.isSwitching = false
        }
    }

    private func startFallback(orThrow originalError: Error?) async throws {
        guard let fallback = fallbackFactory() else {
            if let originalError { throw originalError }
            throw DictationTranscriberError.recognizerUnavailable
        }
        active = fallback
        bindHandlers(to: fallback)
        try await fallback.start()
    }

    private var isPrimaryActive: Bool {
        guard let active else { return false }
        return ObjectIdentifier(active as AnyObject) == ObjectIdentifier(primary as AnyObject)
    }
}

/// Converts the hardware microphone format to the format supported by the
/// active SpeechAnalyzer module. The wrapper is intentionally sendable because
/// its only cross-thread operation is protected by a lock; the audio tap can
/// run independently of the main actor that owns the transcriber.
@available(iOS 26.0, *)
private final class SpeechAnalyzerAudioConverter: @unchecked Sendable {
    private let converter: AVAudioConverter
    private let analyzerFormat: AVAudioFormat
    private let lock = NSLock()

    init?(inputFormat: AVAudioFormat, analyzerFormat: AVAudioFormat) {
        guard let converter = AVAudioConverter(from: inputFormat, to: analyzerFormat) else {
            return nil
        }
        self.converter = converter
        self.analyzerFormat = analyzerFormat
        converter.primeMethod = .none
    }

    func convert(_ buffer: AVAudioPCMBuffer) throws -> AVAudioPCMBuffer {
        lock.lock()
        defer { lock.unlock() }

        let sampleRateRatio = analyzerFormat.sampleRate / buffer.format.sampleRate
        guard sampleRateRatio > 0, buffer.frameLength > 0 else {
            throw SpeechAnalyzerTranscriberError.audioConversionFailed
        }

        let outputCapacity = max(
            AVAudioFrameCount(1),
            AVAudioFrameCount(ceil(Double(buffer.frameLength) * sampleRateRatio) + 32),
        )
        guard let converted = AVAudioPCMBuffer(
            pcmFormat: analyzerFormat,
            frameCapacity: outputCapacity
        ) else {
            throw SpeechAnalyzerTranscriberError.audioConversionFailed
        }

        var conversionError: NSError?
        var hasProvidedInput = false
        let status = converter.convert(to: converted, error: &conversionError) { _, status in
            guard !hasProvidedInput else {
                status.pointee = .endOfStream
                return nil
            }
            hasProvidedInput = true
            status.pointee = .haveData
            return buffer
        }
        guard
            (status == .haveData || status == .inputRanDry),
            conversionError == nil,
            converted.frameLength > 0
        else {
            throw SpeechAnalyzerTranscriberError.audioConversionFailed
        }
        return converted
    }
}

/// iOS 26 `SpeechAnalyzer` + `SpeechTranscriber` implementation.
///
/// Converts microphone buffers to the analyzer's supported format, then
/// forwards both volatile (in-progress) and final results.
/// `finalizeAndFinishThroughEndOfInput` flushes trailing audio so the last
/// words are never cut off.
@available(iOS 26.0, *)
@MainActor
final class SpeechAnalyzerTranscriber: SpeechTranscribing {
    private let locale: Locale

    private var analyzer: SpeechAnalyzer?
    private var transcriber: SpeechTranscriber?
    private let engine = AVAudioEngine()
    private var bufferStream: AsyncStream<Speech.AnalyzerInput>.Continuation?
    private var audioConverter: SpeechAnalyzerAudioConverter?
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
        confirmedText = ""
        finalText = ""
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

        do {
            try configureAudioSession()
        } catch {
            teardownAudioPipeline()
            throw error
        }

        let input = engine.inputNode
        let inputFormat = input.outputFormat(forBus: 0)
        guard inputFormat.channelCount > 0, inputFormat.sampleRate > 0 else {
            teardownAudioPipeline()
            throw DictationTranscriberError.microphoneUnavailable
        }
        guard
            let analyzerFormat =
                await SpeechAnalyzer.bestAvailableAudioFormat(
                    compatibleWith: [transcriber],
                    considering: inputFormat,
                )
        else {
            teardownAudioPipeline()
            throw SpeechAnalyzerTranscriberError.analyzerFormatUnavailable
        }
        guard let audioConverter = SpeechAnalyzerAudioConverter(
            inputFormat: inputFormat,
            analyzerFormat: analyzerFormat
        ) else {
            teardownAudioPipeline()
            throw SpeechAnalyzerTranscriberError.audioConverterUnavailable
        }
        self.audioConverter = audioConverter

        // Prepare before the first buffer arrives. The old implementation
        // relied on lazy setup and passed the hardware format directly into
        // AnalyzerInput, which triggers an assertion on some iPhones.
        do {
            try await analyzer.prepareToAnalyze(in: analyzerFormat)
        } catch {
            teardownAudioPipeline()
            throw error
        }

        var continuation: AsyncStream<Speech.AnalyzerInput>.Continuation!
        let stream = AsyncStream(
            Speech.AnalyzerInput.self,
            bufferingPolicy: .bufferingNewest(64)
        ) { continuation = $0 }
        self.bufferStream = continuation

        // Pump microphone buffers into the analyzer until the stream ends.
        pumpTask = Task { [weak self, analyzer] in
            do {
                try await analyzer.start(inputSequence: stream)
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

        audioWriter?.start(format: inputFormat)
        let audioWriter = self.audioWriter
        input.installTap(onBus: 0, bufferSize: 4096, format: inputFormat) {
            [weak self, continuation, audioWriter, audioConverter] buffer, when in
            guard buffer.frameLength > 0 else { return }
            audioWriter?.append(buffer)
            do {
                let converted = try audioConverter.convert(buffer)
                let startTime: CMTime?
                if when.isSampleTimeValid, when.sampleRate > 0 {
                    startTime = CMTime(
                        seconds: Double(when.sampleTime) / when.sampleRate,
                        preferredTimescale: 1_000_000
                    )
                } else {
                    startTime = nil
                }
                continuation?.yield(
                    Speech.AnalyzerInput(buffer: converted, bufferStartTime: startTime)
                )
            } catch {
                // Never throw from an audio tap. Finishing the input stream
                // lets the analyzer unwind normally, while the view receives
                // an actionable failure message on the main actor.
                continuation?.finish()
                Task { @MainActor [weak self] in
                    self?.failureHandler?(error.localizedDescription)
                }
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

        // Stop production before ending the sequence so no tap callback can
        // race the final input. The custom converter is synchronous and has no
        // pending frames once its callback returns.
        stopAudioInput()
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
        stopAudioInput()
        await analyzer?.cancelAndFinishNow()
        resultsTask = nil
        pumpTask = nil
        teardownAudioPipeline()
    }

    private func configureAudioSession() throws {
        let session = AVAudioSession.sharedInstance()
        try session.setCategory(.record, mode: .measurement, options: [.allowBluetoothHFP])
        try? session.setPreferredSampleRate(44_100)
        try? session.setPreferredIOBufferDuration(0.02)
        try session.setActive(true, options: .notifyOthersOnDeactivation)
        guard session.isInputAvailable, !session.currentRoute.inputs.isEmpty else {
            throw DictationTranscriberError.microphoneUnavailable
        }
    }

    private func stopAudioInput() {
        engine.inputNode.removeTap(onBus: 0)
        engine.stop()
    }

    private func teardownAudioPipeline() {
        stopAudioInput()
        pumpTask?.cancel()
        resultsTask?.cancel()
        pumpTask = nil
        resultsTask = nil
        bufferStream?.finish()
        bufferStream = nil
        audioConverter = nil
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
        latestTranscript = ""
        finalText = ""
        audioWriter = DictationAudioFileWriter()
        do {
            try configureAudioSession()
        } catch {
            teardownAudioPipeline()
            throw error
        }

        let recognizer = SFSpeechRecognizer(locale: Locale(identifier: localeIdentifier))
        guard
            let recognizer,
            recognizer.isAvailable,
            recognizer.supportsOnDeviceRecognition
        else {
            teardownAudioPipeline()
            throw DictationTranscriberError.recognizerUnavailable
        }
        self.recognizer = recognizer

        let request = SFSpeechAudioBufferRecognitionRequest()
        request.shouldReportPartialResults = true
        request.requiresOnDeviceRecognition = true
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

    private func configureAudioSession() throws {
        let session = AVAudioSession.sharedInstance()
        try session.setCategory(.record, mode: .measurement, options: [.allowBluetoothHFP])
        try? session.setPreferredSampleRate(44_100)
        try? session.setPreferredIOBufferDuration(0.02)
        try session.setActive(true, options: .notifyOthersOnDeactivation)
        guard session.isInputAvailable, !session.currentRoute.inputs.isEmpty else {
            throw DictationTranscriberError.microphoneUnavailable
        }
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

    private(set) var recordingFilePath: String?
    private(set) var recordingDurationMillis: Int64?

    var partialHandler: ((String) -> Void)?
    var failureHandler: ((String) -> Void)?

    init(transcript: String = "Registar o peso do Lua do Pinhal e uma desparasitação. Depois fazer uma ecografia à Fantasma Inexistente.") {
        self.transcript = transcript
    }

    func start() async throws {
        isRecording = true
        recordingFilePath = nil
        recordingDurationMillis = nil
        partialHandler?(transcript)
    }

    func finish() async throws -> String {
        guard isRecording else { return "" }
        isRecording = false
        createDeterministicRecording()
        return transcript
    }

    func cancel() async {
        isRecording = false
    }

    private func createDeterministicRecording() {
        guard
            let format = AVAudioFormat(standardFormatWithSampleRate: 44_100, channels: 1),
            let buffer = AVAudioPCMBuffer(pcmFormat: format, frameCapacity: 44_100 * 30)
        else { return }
        buffer.frameLength = buffer.frameCapacity
        buffer.floatChannelData?[0].initialize(repeating: 0, count: Int(buffer.frameLength))

        let writer = DictationAudioFileWriter()
        writer.start(format: format)
        writer.append(buffer)
        guard let result = writer.close() else { return }
        recordingFilePath = result.path
        recordingDurationMillis = result.durationMillis
    }
}

enum DictationTranscriberError: LocalizedError {
    case recognizerUnavailable
    case microphoneUnavailable
    case microphoneDenied
    case speechPermissionDenied

    var errorDescription: String? {
        switch self {
        case .recognizerUnavailable: return "Speech recognition is not available for this language."
        case .microphoneUnavailable: return "The microphone is unavailable. Check the audio route and try again."
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
