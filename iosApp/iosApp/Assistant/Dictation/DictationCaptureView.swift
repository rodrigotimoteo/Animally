import AVFoundation
import Shared
import SwiftUI

/// Voice-dictation capture sheet.
///
/// Flow: idle → recording (waveform + live transcript) → reviewing-transcript
/// (editable ASR text) → transcribing (extraction) → reviewing. Cancel at any
/// point discards everything.
struct DictationCaptureView: View {
    let onFinished: () -> Void

    private enum Phase {
        case idle
        case recording
        case reviewingTranscript
        case transcribing
        case reviewing
    }

    @ObservedObject var reviewViewModel: DictationReviewViewModel
    @State private var phase: Phase = .idle
    @State private var liveTranscript = ""
    @State private var editableTranscript = ""
    @State private var selectedLanguage = DictationLanguage.deviceDefault
    @State private var errorMessage: String?
    @State private var fallbackLocaleHint: String?
    @State private var disambiguatedPatients: [Int: Patient] = [:]
    @State private var isPreparingEngine = true
    @State private var engineUnavailableMessage: String?
    @State private var operationTask: Task<Void, Never>?

    @State private var transcriber: (any SpeechTranscribing)?
    @State private var extractor: (any DictationExtracting)?

    init(
        viewModel: DictationReviewViewModel,
        onFinished: @escaping () -> Void
    ) {
        self.reviewViewModel = viewModel
        self.onFinished = onFinished
    }

    var body: some View {
        NavigationStack {
            Group {
                switch phase {
                case .idle:
                    idleView
                case .recording:
                    recordingView
                case .reviewingTranscript:
                    transcriptReviewView
                case .transcribing:
                    transcribingView
                case .reviewing:
                    SuggestionReviewView(
                        viewModel: reviewViewModel,
                        disambiguatedPatients: $disambiguatedPatients,
                        onFinished: onFinished
                    )
                    .accessibilityIdentifier("dictation_review")
                }
            }
            .navigationTitle("Dictate records")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button("Cancel") { cancelAndDismiss() }
                }
            }
            .overlay(alignment: .bottom) {
                if let errorMessage {
                    errorBanner(message: errorMessage)
                        .padding(.bottom, 90)
                }
            }
        }
        .interactiveDismissDisabled(phase == .recording || phase == .reviewingTranscript || phase == .transcribing)
        .onDisappear {
            operationTask?.cancel()
            operationTask = nil
            Task { await transcriber?.cancel() }
        }
    }

    // MARK: Idle

    private var idleView: some View {
        VStack(spacing: 24) {
            Text("Describe the records you want to save — weights, ultrasounds, deworming.")
                .font(.subheadline)
                .foregroundStyle(Theme.textSecondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 32)

            Picker("Dictation language", selection: $selectedLanguage) {
                ForEach(DictationLanguage.allCases) { language in
                    Text(language.displayName).tag(language)
                }
            }
            .pickerStyle(.segmented)
            .padding(.horizontal, 24)
            .accessibilityIdentifier("dictation_language")

            Button {
                startRecording()
            } label: {
                Label("Start dictating", systemImage: "mic.fill")
                    .font(.headline)
                    .foregroundStyle(.white)
                    .padding(.horizontal, 28)
                    .padding(.vertical, 14)
                    .background(Theme.forestGreen)
                    .clipShape(Capsule())
            }
            .disabled(isPreparingEngine)
            .accessibilityIdentifier("dictation_start")

            if isPreparingEngine {
                ProgressView("Preparing dictation…")
                    .font(.caption)
                    .foregroundStyle(Theme.textSecondary)
            } else if let engineUnavailableMessage {
                Label(engineUnavailableMessage, systemImage: "info.circle")
                    .font(.caption)
                    .foregroundStyle(Theme.textSecondary)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 28)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .task(id: selectedLanguage) {
            await prepareEngines(for: selectedLanguage)
        }
    }

    // MARK: Recording

    private var recordingView: some View {
        VStack(spacing: 28) {
            WaveformIndicator()
                .frame(height: 48)

            if let fallbackLocaleHint {
                Text(fallbackLocaleHint)
                    .font(.caption2)
                    .foregroundStyle(Theme.textSecondary)
            }

            ScrollView {
                Text(liveTranscript.isEmpty ? "Listening…" : liveTranscript)
                    .font(.body)
                    .foregroundStyle(liveTranscript.isEmpty ? Theme.textTertiary : Theme.textPrimary)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(.horizontal, 24)
            }
            .accessibilityIdentifier("dictation_live_transcript")

            Button {
                stopRecording()
            } label: {
                ZStack {
                    Circle()
                        .fill(Color.red)
                        .frame(width: 84, height: 84)
                        .shadow(color: .red.opacity(0.35), radius: 10, y: 4)
                    RoundedRectangle(cornerRadius: 6)
                        .fill(.white)
                        .frame(width: 30, height: 30)
                }
            }
            .accessibilityLabel("Stop recording")
            .accessibilityIdentifier("dictation_stop")
        }
        .padding(.vertical, 24)
    }

    // MARK: Transcript review

    private var transcriptReviewView: some View {
        VStack(spacing: 20) {
            Text("Check the transcript — fix any misheard words before extracting.")
                .font(.subheadline)
                .foregroundStyle(Theme.textSecondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 24)

            TextEditor(text: $editableTranscript)
                .font(.body)
                .scrollContentBackground(.hidden)
                .background(Theme.surfaceElevated)
                .clipShape(RoundedRectangle(cornerRadius: 12))
                .overlay(
                    RoundedRectangle(cornerRadius: 12)
                        .stroke(Theme.textTertiary.opacity(0.35), lineWidth: 1)
                )
                .padding(.horizontal, 24)
                .accessibilityIdentifier("dictation_transcript_editor")

            if extractor?.usesCloudModel == true {
                Label(
                    "Cloud AI will structure this transcript. Check every suggestion before saving.",
                    systemImage: "cloud.fill"
                )
                .font(.caption)
                .foregroundStyle(Theme.textSecondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 28)
                .accessibilityIdentifier("dictation_cloud_extraction_notice")
            }

            Button {
                errorMessage = nil
                phase = .transcribing
                operationTask?.cancel()
                operationTask = Task {
                    await runExtraction(transcript: editableTranscript)
                }
            } label: {
                Label("Extract", systemImage: "sparkles")
                    .font(.headline)
                    .foregroundStyle(.white)
                    .padding(.horizontal, 28)
                    .padding(.vertical, 14)
                    .background(Theme.forestGreen)
                    .clipShape(Capsule())
            }
            .disabled(editableTranscript.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
            .accessibilityIdentifier("dictation_extract")
        }
        .padding(.vertical, 24)
    }

    // MARK: Transcribing

    private var transcribingView: some View {
        VStack(spacing: 20) {
            ProgressView()
                .scaleEffect(1.3)
            Text("Reading your dictation…")
                .font(.subheadline)
                .foregroundStyle(Theme.textSecondary)
            Text(liveTranscript)
                .font(.caption)
                .foregroundStyle(Theme.textTertiary)
                .lineLimit(4)
                .padding(.horizontal, 32)
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    // MARK: Actions

    private func prepareEngines(for language: DictationLanguage) async {
        isPreparingEngine = true
        let candidateExtractor = DictationExtractorFactory.make(
            language: language,
            cloudExtraction: cloudExtraction
        )
        let resolved = await SpeechTranscriberService.resolve(preferredLocale: language.locale)
        guard !Task.isCancelled, selectedLanguage == language else { return }
        // Keep the current engine until the new language is ready. This avoids
        // replacing the sheet's state while the picker is being changed and
        // prevents a stale resolution from winning a rapid language switch.
        extractor = candidateExtractor
        applyResolvedEngine(resolved)
        isPreparingEngine = false
    }

    private var cloudExtraction: CloudDictationExtraction? {
        guard reviewViewModel.canUseCloudExtraction() else { return nil }
        return { transcript, language in
            try await reviewViewModel.extract(transcript: transcript, language: language)
        }
    }

    private func startRecording() {
        guard !isPreparingEngine else {
            errorMessage = "Dictation is still preparing. Try again in a moment."
            return
        }
        errorMessage = nil
        operationTask?.cancel()
        operationTask = Task {
            if let permissionError = await SpeechAuthService.requestAuthorization() {
                errorMessage = permissionError.localizedDescription
                return
            }
            // Availability can change after permission is granted or after
            // speech assets finish installing, so always resolve once more at
            // the point of use instead of trusting a stale preflight result.
            isPreparingEngine = true
            let language = selectedLanguage
            let resolved = await SpeechTranscriberService.resolve(preferredLocale: language.locale)
            guard !Task.isCancelled, selectedLanguage == language else { return }
            applyResolvedEngine(resolved)
            isPreparingEngine = false
            guard let transcriber else {
                errorMessage = engineUnavailableMessage ?? SpeechTranscriberService.unavailableMessage
                return
            }
            transcriber.partialHandler = { partial in
                liveTranscript = partial
            }
            transcriber.failureHandler = { [weak transcriber] failure in
                errorMessage = failure
                if phase == .recording {
                    phase = .idle
                    Task { [weak transcriber] in
                        await transcriber?.cancel()
                    }
                }
            }
            do {
                try await transcriber.start()
                phase = .recording
            } catch {
                errorMessage = error.localizedDescription
                phase = .idle
            }
        }
    }

    private func applyResolvedEngine(_ resolved: ResolvedDictationEngine?) {
        transcriber = resolved?.transcriber
        fallbackLocaleHint =
            resolved?.usesFallbackLocale == true
            ? "Dictating in \(resolved?.localeDisplayName ?? "")"
            : nil
        engineUnavailableMessage = resolved == nil ? SpeechTranscriberService.unavailableMessage : nil
    }

    private func stopRecording() {
        phase = .reviewingTranscript
        operationTask?.cancel()
        operationTask = Task {
            defer { try? AVAudioSession.sharedInstance().setActive(false) }
            guard let transcriber else {
                phase = .idle
                return
            }
            do {
                let transcript = try await transcriber.finish()
                // Persist before extraction or review. A failed extractor or
                // later cancellation must never discard the original note.
                reviewViewModel.saveCapture(
                    transcript: transcript,
                    audioPath: transcriber.recordingFilePath,
                    durationMillis: transcriber.recordingDurationMillis
                )
                guard !transcript.isEmpty else {
                    phase = .idle
                    errorMessage = "Nothing was captured. Try again."
                    return
                }
                editableTranscript = transcript
            } catch is CancellationError {
                return
            } catch {
                // `finish()` tears down the pipeline even when recognition
                // fails. Keep any partial transcript and completed audio.
                reviewViewModel.saveCapture(
                    transcript: liveTranscript,
                    audioPath: transcriber.recordingFilePath,
                    durationMillis: transcriber.recordingDurationMillis
                )
                errorMessage = error.localizedDescription
                phase = .recording
            }
        }
    }

    private func runExtraction(transcript: String) async {
        guard let extractor else {
            errorMessage = "Extractor unavailable."
            phase = .idle
            return
        }
        reviewViewModel.setTranscript(transcript)
        do {
            let sessionJson = try await extractor.extract(transcript: transcript, onUpdate: nil)
            try Task.checkCancellation()
            reviewViewModel.validate(sessionJson: sessionJson)
            phase = .reviewing
        } catch is CancellationError {
            return
        } catch let error as DictationExtractorError {
            errorMessage = error.localizedDescription
            // Keep the editable transcript visible when structured extraction
            // is unavailable or fails, so the user can copy it or retry.
            phase = .reviewingTranscript
        } catch {
            errorMessage = "Could not read the dictation: \(error.localizedDescription)"
            phase = .reviewingTranscript
        }
    }

    private func cancelAndDismiss() {
        operationTask?.cancel()
        operationTask = nil
        Task { await transcriber?.cancel() }
        onFinished()
    }

    // MARK: Error banner

    private func errorBanner(message: String) -> some View {
        HStack(spacing: 12) {
            Image(systemName: "exclamationmark.triangle.fill")
                .foregroundStyle(Theme.amber)
            Text(message)
                .font(.subheadline)
                .foregroundStyle(Theme.textPrimary)
                .lineLimit(2)
            Spacer()
            Button {
                self.errorMessage = nil
            } label: {
                Image(systemName: "xmark")
                    .font(.caption.weight(.bold))
                    .foregroundStyle(Theme.textSecondary)
            }
        }
        .padding(12)
        .background(Theme.surfaceElevated)
        .clipShape(RoundedRectangle(cornerRadius: 12))
        .shadow(color: .black.opacity(0.08), radius: 8, y: 2)
        .padding(.horizontal)
        .transition(.move(edge: .bottom).combined(with: .opacity))
    }
}

// MARK: - Waveform

/// Lightweight animated level bars standing in for a live waveform.
private struct WaveformIndicator: View {
    @State private var animating = false

    private let barHeights: [CGFloat] = [0.35, 0.7, 1.0, 0.55, 0.85, 0.45]

    var body: some View {
        HStack(spacing: 6) {
            ForEach(Array(barHeights.enumerated()), id: \.offset) { index, height in
                Capsule()
                    .fill(Theme.forestGreen)
                    .frame(width: 6, height: 40 * height)
                    .scaleEffect(y: animating ? 0.45 : 1.0, anchor: .center)
                    .animation(
                        .easeInOut(duration: 0.5)
                            .repeatForever(autoreverses: true)
                            .delay(Double(index) * 0.09),
                        value: animating
                    )
            }
        }
        .onAppear { animating = true }
    }
}
