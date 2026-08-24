import SwiftUI
import Shared

struct AssistantView: View {
    @StateObject private var viewModel = AssistantViewModel()
    @State private var draft: String = ""
    @State private var showDictation = false
    @State private var path = NavigationPath()
    @FocusState private var inputFocused: Bool

    var body: some View {
        NavigationStack(path: $path) {
            Group {
                if isAvailable {
                    chatContent
                } else {
                    unavailableView
                }
            }
            .navigationDestination(for: Route.self) { route in
                switch route {
                case .patientDetail(let id):
                    PatientDetailView(patientId: id)
                case .patientEdit(let id):
                    PatientEditView(patientId: id)
                case .ownerDetail(let id):
                    OwnerDetailView(ownerId: id)
                case .ownerEdit(let id):
                    OwnerEditView(ownerId: id)
                }
            }
            .navigationDestination(for: RecordDetailKey.self) { key in
                RecordDetailView(
                    displayType: key.displayType,
                    patientId: key.patientId,
                    recordId: key.recordId
                )
            }
            .navigationTitle("Assistant")
            .navigationBarTitleDisplayMode(.inline)
            .overlay(alignment: .top) {
                if let errorMessage = viewModel.state.error {
                    errorBanner(message: errorMessage)
                }
            }
            .onAppear {
                viewModel.refreshAvailability()
            }
            .sheet(isPresented: $showDictation) {
                DictationCaptureView(onFinished: { showDictation = false })
            }
        }
    }

    // MARK: - Availability

    private var isAvailable: Bool {
        if viewModel.state.availability is LlmAvailabilityAvailable {
            return true
        }
        return false
    }

    // MARK: - Chat

    private var chatContent: some View {
        VStack(spacing: 0) {
            if viewModel.state.messages.isEmpty {
                emptyChatView
            } else {
                transcript
            }

            inputBar
        }
    }

    private var transcript: some View {
        ScrollViewReader { proxy in
            ScrollView {
                LazyVStack(spacing: 12) {
                    ForEach(Array(viewModel.state.messages.enumerated()), id: \.offset) { index, message in
                        ChatBubble(
                            message: message,
                            onFollowUp: { suggestion in
                                draft = suggestion
                                inputFocused = true
                            },
                            onOpenSource: { source in
                                openSource(source)
                            },
                            onRetry: retryQuestion(forIndex: index).map { question in
                                { viewModel.ask(question: question) }
                            }
                        )
                        .id(index)
                    }

                    if viewModel.state.isGenerating {
                        typingIndicator
                            .id("typing")
                    }
                }
                .padding(.horizontal)
                .padding(.vertical, 16)
            }
            .onChange(of: viewModel.state.messages.count) { _ in
                withAnimation(.easeOut(duration: 0.25)) {
                    proxy.scrollTo(viewModel.state.isGenerating ? AnyHashable("typing") : AnyHashable(viewModel.state.messages.count - 1), anchor: .bottom)
                }
            }
            .onChange(of: viewModel.state.isGenerating) { generating in
                withAnimation(.easeOut(duration: 0.25)) {
                    proxy.scrollTo(generating ? AnyHashable("typing") : AnyHashable(viewModel.state.messages.count - 1), anchor: .bottom)
                }
            }
        }
        .scrollDismissesKeyboard(.immediately)
        .onTapGesture {
            inputFocused = false
        }
        .background(Theme.surfaceElevated.opacity(0.35))
        .accessibilityIdentifier("assistant_transcript")
    }

    private var typingIndicator: some View {
        HStack {
            HStack(spacing: 5) {
                ForEach(0..<3, id: \.self) { dot in
                    Circle()
                        .fill(Theme.textSecondary)
                        .frame(width: 7, height: 7)
                        .opacity(viewModel.state.isGenerating ? 1 : 0.4)
                        .animation(
                            .easeInOut(duration: 0.6)
                                .repeatForever()
                                .delay(Double(dot) * 0.2),
                            value: viewModel.state.isGenerating
                        )
                }
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 12)
            .background(Theme.surfaceElevated)
            .clipShape(ChatBubbleShape(isUser: false))
            Spacer()
        }
        .accessibilityLabel("Assistant is thinking")
    }

    private var emptyChatView: some View {
        VStack(spacing: 20) {
            Image(systemName: "sparkles")
                .font(.system(size: 64))
                .foregroundStyle(Theme.forestGreen.opacity(0.6))
            Text("Ask about your patients")
                .font(.title2.weight(.semibold))
                .foregroundStyle(Theme.textPrimary)
            Text("Ask anything about your records — history, treatments, reminders, and more")
                .font(.subheadline)
                .foregroundStyle(Theme.textSecondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 24)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .padding()
        .contentShape(Rectangle())
        .onTapGesture {
            inputFocused = false
        }
    }

    private var inputBar: some View {
        HStack(spacing: 12) {
            Button {
                showDictation = true
            } label: {
                Image(systemName: "mic.fill")
                    .font(.system(size: 20))
                    .foregroundStyle(Theme.forestGreen)
                    .frame(width: 40, height: 40)
                    .background(Theme.forestGreen.opacity(0.12))
                    .clipShape(Circle())
            }
            .disabled(viewModel.state.isGenerating)
            .accessibilityLabel("Dictate records")
            .accessibilityIdentifier("assistant_dictate")

            TextField("Ask a question…", text: $draft, axis: .vertical)
                .focused($inputFocused)
                .textFieldStyle(.plain)
                .lineLimit(1...4)
                .padding(.horizontal, 14)
                .padding(.vertical, 10)
                .background(Theme.surfaceElevated)
                .clipShape(RoundedRectangle(cornerRadius: 20))
                .disabled(viewModel.state.isGenerating)
                .accessibilityIdentifier("assistant_input")

            Button {
                sendDraft()
            } label: {
                Image(systemName: "arrow.up.circle.fill")
                    .font(.system(size: 32))
                    .foregroundStyle(canSend ? Theme.forestGreen : Theme.textTertiary)
                    .scaleEffect(viewModel.state.isGenerating ? 0.92 : 1.0)
                    .animation(
                        viewModel.state.isGenerating
                            ? .easeInOut(duration: 0.9).repeatForever(autoreverses: true)
                            : .easeOut(duration: 0.2),
                        value: viewModel.state.isGenerating
                    )
            }
            .disabled(!canSend)
            .accessibilityLabel("Send message")
            .accessibilityIdentifier("assistant_send")
        }
        .padding(.horizontal)
        .padding(.vertical, 10)
        .background(.bar)
    }

    private var canSend: Bool {
        !draft.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty && !viewModel.state.isGenerating
    }

    private func sendDraft() {
        let question = draft.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !question.isEmpty else { return }
        draft = ""
        inputFocused = false
        viewModel.ask(question: question)
    }

    // MARK: - Source deep links

    /// Routes a cited record to its detail: patient rows push the patient
    /// page, every other record type pushes the patient page underneath and
    /// the read-only record detail on top (same pattern as Search).
    private func openSource(_ source: SearchResult) {
        if source.recordType == "OWNER" {
            path.append(Route.ownerDetail(source.patientId))
            return
        }
        path.append(Route.patientDetail(source.patientId))
        guard source.recordType != "PATIENT" else { return }
        path.append(RecordDetailKey(
            displayType: source.recordType,
            patientId: source.patientId,
            recordId: source.recordId
        ))
    }

    /// The user question that produced the assistant turn at [index], when
    /// retrying that turn makes sense (interrupted reply with a question
    /// directly before it).
    private func retryQuestion(forIndex index: Int) -> String? {
        let messages = viewModel.state.messages
        guard index < messages.count, messages[index].interrupted else { return nil }
        guard index > 0, messages[index - 1].role == AssistantChatMessageRole.user else { return nil }
        return messages[index - 1].text
    }

    // MARK: - Unavailable

    private var unavailableView: some View {
        VStack(spacing: 24) {
            ZStack {
                Circle()
                    .fill(Theme.forestGreen.opacity(0.12))
                    .frame(width: 120, height: 120)
                Image(systemName: "sparkles")
                    .font(.system(size: 52))
                    .foregroundStyle(Theme.forestGreen)
            }

            Text("On-device AI not available here")
                .font(.title2.weight(.semibold))
                .foregroundStyle(Theme.textPrimary)

            Text("The assistant runs entirely on your device using Apple Intelligence. It needs an iPhone 15 Pro or newer running iOS 26 or later.")
                .font(.subheadline)
                .foregroundStyle(Theme.textSecondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 24)

            if let reasonText = unavailableReasonText {
                Label(reasonText, systemImage: "info.circle")
                    .font(.footnote.weight(.medium))
                    .foregroundStyle(Theme.textSecondary)
                    .padding(.horizontal, 14)
                    .padding(.vertical, 8)
                    .background(Theme.surfaceElevated)
                    .clipShape(Capsule())
            }

            Button {
                viewModel.refreshAvailability()
            } label: {
                Text("Check Again")
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(.white)
                    .padding(.horizontal, 28)
                    .padding(.vertical, 12)
                    .background(Theme.forestGreen)
                    .clipShape(Capsule())
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .padding()
    }

    private var unavailableReasonText: String? {
        switch viewModel.state.availability {
        case let unavailable as LlmAvailabilityUnavailable:
            switch unavailable.reason {
            case UnavailableReason.deviceNotEligible:
                return "This device doesn't support Apple Intelligence"
            case UnavailableReason.appleIntelligenceNotEnabled:
                return "Apple Intelligence isn't enabled in Settings"
            case UnavailableReason.modelNotReady:
                return "The Apple Intelligence model is still downloading"
            case UnavailableReason.noLocalModel:
                return "No local AI model found on this device"
            default:
                return nil
            }
        default:
            return nil
        }
    }

    // MARK: - Error banner

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
                viewModel.dismissError()
            } label: {
                Image(systemName: "xmark")
                    .font(.caption.weight(.bold))
                    .foregroundStyle(Theme.textSecondary)
                    .accessibilityLabel("Dismiss error")
            }
        }
        .padding(12)
        .background(Theme.surfaceElevated)
        .clipShape(RoundedRectangle(cornerRadius: 12))
        .shadow(color: .black.opacity(0.08), radius: 8, y: 2)
        .padding(.horizontal)
        .padding(.top, 8)
        .transition(.move(edge: .top).combined(with: .opacity))
    }
}

// MARK: - Chat bubble

private struct ChatBubble: View {
    let message: AssistantChatMessage
    let onFollowUp: (String) -> Void
    let onOpenSource: (SearchResult) -> Void
    let onRetry: (() -> Void)?

    private var isUser: Bool {
        message.role == AssistantChatMessageRole.user
    }

    private var sources: [SearchResult] {
        (message.sources as? [SearchResult]) ?? []
    }

    private var followUps: [String] {
        (message.followUps as? [String]) ?? []
    }

    var body: some View {
        HStack(alignment: .bottom) {
            if isUser { Spacer(minLength: 48) }
            VStack(alignment: isUser ? .trailing : .leading, spacing: 6) {
                Text(message.text)
                    .font(.subheadline)
                    .foregroundStyle(isUser ? .white : Theme.textPrimary)
                    .padding(.horizontal, 14)
                    .padding(.vertical, 10)
                    .background(isUser ? Theme.forestGreen : Theme.surfaceElevated)
                    .clipShape(ChatBubbleShape(isUser: isUser))
                if message.interrupted {
                    interruptedFooter
                }
                if !isUser && !sources.isEmpty {
                    sourceChips
                }
                if !isUser && !followUps.isEmpty {
                    followUpChips
                }
            }
            if !isUser { Spacer(minLength: 48) }
        }
        .accessibilityLabel("\(isUser ? "You" : "Assistant"): \(message.text)")
    }

    /// Interruption marker with a retry affordance; the partial text above
    /// stays visible so nothing the model produced is lost.
    private var interruptedFooter: some View {
        HStack(spacing: 6) {
            Image(systemName: "exclamationmark.triangle.fill")
                .font(.caption2)
                .foregroundStyle(Theme.amber)
            Text("Response cut short.")
                .font(.caption)
                .foregroundStyle(Theme.textSecondary)
            if let onRetry {
                Button(action: onRetry) {
                    Text("Retry")
                        .font(.caption.weight(.semibold))
                        .foregroundStyle(Theme.forestGreen)
                }
                .accessibilityIdentifier("assistant_retry")
            }
        }
    }

    /// Tappable chips for the records cited in this answer.
    private var sourceChips: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 6) {
                ForEach(Array(sources.enumerated()), id: \.offset) { _, source in
                    Button {
                        onOpenSource(source)
                    } label: {
                        HStack(spacing: 4) {
                            Image(systemName: source.recordType == "PATIENT" ? "horse" : "doc.text")
                                .font(.caption2)
                            Text(source.patientName.isEmpty ? source.recordType : source.patientName)
                                .font(.caption.weight(.medium))
                        }
                        .padding(.horizontal, 10)
                        .padding(.vertical, 5)
                        .background(Theme.forestGreen.opacity(0.12))
                        .foregroundStyle(Theme.forestGreen)
                        .clipShape(Capsule())
                    }
                    .accessibilityIdentifier("assistant_source_chip")
                    .accessibilityLabel("Open \(source.recordType) for \(source.patientName)")
                }
            }
        }
    }

    /// Deterministic follow-up suggestions; tapping fills the input without
    /// sending so the vet can edit first.
    private var followUpChips: some View {
        HStack(spacing: 6) {
            ForEach(followUps, id: \.self) { suggestion in
                Button {
                    onFollowUp(suggestion)
                } label: {
                    Text(suggestion)
                        .font(.caption)
                        .padding(.horizontal, 10)
                        .padding(.vertical, 5)
                        .background(Theme.surfaceElevated)
                        .foregroundStyle(Theme.textSecondary)
                        .clipShape(Capsule())
                        .overlay(Capsule().strokeBorder(Theme.textTertiary.opacity(0.35)))
                }
                .accessibilityIdentifier("assistant_followup_chip")
                .accessibilityLabel("Suggest: \(suggestion)")
            }
        }
    }
}

private struct ChatBubbleShape: Shape {
    let isUser: Bool

    func path(in rect: CGRect) -> Path {
        let radius: CGFloat = 16
        let smallRadius: CGFloat = 5
        var path = Path()
        path.addRoundedRect(
            in: rect,
            cornerSize: CGSize(width: radius, height: radius),
            style: .continuous
        )
        var tail = Path()
        tail.addRect(CGRect(
            x: isUser ? rect.maxX - smallRadius * 2 : rect.minX,
            y: rect.minY,
            width: smallRadius * 2,
            height: smallRadius * 2
        ))
        return path.subtracting(tail)
    }
}
