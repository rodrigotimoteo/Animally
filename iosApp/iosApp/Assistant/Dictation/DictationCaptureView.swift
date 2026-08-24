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

    @StateObject private var reviewViewModel: DictationReviewViewModel
    @State private var phase: Phase = .idle
    @State private var liveTranscript = ""
    @State private var editableTranscript = ""
    @State private var errorMessage: String?
    @State private var assetHint: String?
    @State private var disambiguatedPatients: [Int: Patient] = [:]

    @State private var transcriber: (any SpeechTranscribing)?
    @State private var extractor: (any DictationExtracting)?

    init(onFinished: @escaping () -> Void) {
        self.onFinished = onFinished
        _reviewViewModel = StateObject(wrappedValue: DictationReviewViewModel(store: IosSettingsStores.shared.dictationStore()))
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
    }

    // MARK: Idle

    private var idleView: some View {
        VStack(spacing: 24) {
            if let assetHint {
                Label(assetHint, systemImage: "arrow.down.circle")
                    .font(.footnote)
                    .foregroundStyle(Theme.amber)
                    .multilineTextAlignment(.leading)
                    .padding(.horizontal, 24)
            }

            Text("Describe the records you want to save — weights, ultrasounds, deworming.")
                .font(.subheadline)
                .foregroundStyle(Theme.textSecondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 32)

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
            .accessibilityIdentifier("dictation_start")
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .task {
            await prepareEngines()
        }
    }

    // MARK: Recording

    private var recordingView: some View {
        VStack(spacing: 28) {
            WaveformIndicator()
                .frame(height: 48)

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

            Button {
                phase = .transcribing
                Task { await runExtraction(transcript: editableTranscript) }
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

    private func prepareEngines() async {
        extractor = DictationExtractorFactory.make()
        if #available(iOS 26.0, *) {
            let preparation = await SpeechTranscriberService.prepareAssets()
            assetHint = preparation.userHint
        }
        transcriber = await SpeechTranscriberService.make()
    }

    private func startRecording() {
        errorMessage = nil
        Task {
            if let permissionError = await SpeechAuthService.requestAuthorization() {
                errorMessage = permissionError.localizedDescription
                return
            }
            guard let transcriber else {
                errorMessage = "Speech engine is not ready yet."
                return
            }
            transcriber.partialHandler = { partial in
                liveTranscript = partial
            }
            transcriber.failureHandler = { failure in
                errorMessage = failure
            }
            do {
                try await transcriber.start()
                phase = .recording
            } catch {
                errorMessage = error.localizedDescription
            }
        }
    }

    private func stopRecording() {
        phase = .reviewingTranscript
        Task {
            defer { try? AVAudioSession.sharedInstance().setActive(false) }
            guard let transcriber else {
                phase = .idle
                return
            }
            do {
                let transcript = try await transcriber.finish()
                guard !transcript.isEmpty else {
                    phase = .idle
                    errorMessage = "Nothing was captured. Try again."
                    return
                }
                editableTranscript = transcript
            } catch {
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
            reviewViewModel.validate(sessionJson: sessionJson)
            phase = .reviewing
        } catch {
            errorMessage = "Could not read the dictation: \(error.localizedDescription)"
            phase = .idle
        }
    }

    private func cancelAndDismiss() {
        if phase == .recording {
            Task { await transcriber?.cancel() }
        }
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
