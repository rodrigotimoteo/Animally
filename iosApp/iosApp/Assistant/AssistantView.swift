import SwiftUI
import Shared

struct AssistantView: View {
    @EnvironmentObject private var theme: ThemeViewModel
    @StateObject private var viewModel = AssistantViewModel()
    @StateObject private var dictationViewModel =
        DictationReviewViewModel(store: IosSettingsStores.shared.dictationStore())
    @State private var draft: String = ""
    @State private var showDictation = false
    @State private var showDictationArchive = false
    @State private var showChatHistory = false
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
            .toolbar {
                ToolbarItemGroup(placement: .topBarTrailing) {
                    Button {
                        draft = ""
                        inputFocused = false
                        viewModel.startNewChat()
                    } label: {
                        Image(systemName: "square.and.pencil")
                    }
                    .disabled(viewModel.state.isGenerating || viewModel.state.isHistoryLoading)
                    .accessibilityLabel("New chat")
                    .accessibilityIdentifier("assistant_new_chat")

                    Button {
                        showChatHistory = true
                    } label: {
                        Image(systemName: "clock.arrow.circlepath")
                    }
                    .accessibilityLabel("Chat history")
                    .accessibilityIdentifier("assistant_chat_history_button")

                    Button {
                        showDictationArchive = true
                    } label: {
                        Image(systemName: "waveform.badge.mic")
                    }
                    .accessibilityLabel("Dictation history")
                    .accessibilityIdentifier("assistant_dictation_history")
                }
            }
            .overlay(alignment: .top) {
                if let errorMessage = viewModel.state.error ?? viewModel.state.historyError {
                    errorBanner(message: errorMessage)
                }
            }
            .onAppear {
                viewModel.refreshAvailability()
            }
            .sheet(isPresented: $showDictation) {
                DictationCaptureView(
                    viewModel: dictationViewModel,
                    onFinished: { showDictation = false }
                )
                .tint(theme.accentColor)
            }
            .sheet(isPresented: $showDictationArchive) {
                DictationArchiveView(viewModel: dictationViewModel)
                    .tint(theme.accentColor)
            }
            .sheet(isPresented: $showChatHistory) {
                AssistantHistoryView(
                    conversations: viewModel.state.conversations,
                    isLoading: viewModel.state.isHistoryLoading,
                    accentColor: theme.accentColor,
                    onUseQuestion: { question in
                        draft = question
                        showChatHistory = false
                        inputFocused = true
                    }
                )
                .tint(theme.accentColor)
                .onAppear {
                    if !viewModel.state.isHistoryLoading {
                        viewModel.refreshHistory()
                    }
                }
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
            if viewModel.state.isHistoryLoading {
                ProgressView("Loading conversation…")
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else if viewModel.state.messages.isEmpty {
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
                // The transcript is capped by the shared store, so keeping the
                // complete, small tree materialized makes accessibility updates
                // predictable while a cloud response is streaming.
                VStack(spacing: 12) {
                    ForEach(Array(viewModel.state.messages.enumerated()), id: \.offset) { index, message in
                        ChatBubble(
                            message: message,
                            accentColor: theme.accentColor,
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
                        .accessibilityIdentifier("assistant_message_\(index)")
                    }

                    if viewModel.state.isGenerating {
                        typingIndicator
                            .id("typing")
                    }
                }
                .padding(.horizontal)
                .padding(.vertical, 16)
            }
            .onChange(of: viewModel.state.messages.count) { _, _ in
                scrollToLatest(using: proxy)
            }
            .onChange(of: viewModel.state.isGenerating) { _, generating in
                scrollToLatest(using: proxy, isGenerating: generating)
            }
        }
        .scrollDismissesKeyboard(.immediately)
        .onTapGesture {
            inputFocused = false
        }
        .background(Theme.surfaceElevated.opacity(0.35))
        .accessibilityIdentifier("assistant_transcript")
    }

    private func scrollToLatest(using proxy: ScrollViewProxy, isGenerating: Bool? = nil) {
        let generating = isGenerating ?? viewModel.state.isGenerating
        let target: AnyHashable = generating
            ? AnyHashable("typing")
            : AnyHashable(viewModel.state.messages.count - 1)
        var transaction = Transaction()
        transaction.animation = nil
        withTransaction(transaction) {
            proxy.scrollTo(target, anchor: .bottom)
        }
    }

    private var typingIndicator: some View {
        HStack(alignment: .center, spacing: 8) {
            HStack(spacing: 5) {
                ForEach(0..<3, id: \.self) { dot in
                    Circle()
                        .fill(Theme.textSecondary)
                        .frame(width: 7, height: 7)
                        .opacity(viewModel.state.isGenerating ? 1 : 0.4)
                }
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 12)
            .background(Theme.surfaceElevated)
            .clipShape(ChatBubbleShape(isUser: false))
            Text("Thinking…")
                .font(.caption)
                .foregroundStyle(Theme.textSecondary)
            Spacer()
        }
        .accessibilityLabel("Assistant is thinking")
    }

    private var emptyChatView: some View {
        VStack(spacing: 16) {
            Image(systemName: "sparkles")
                .font(.system(size: 64))
                .foregroundStyle(theme.accentColor.opacity(0.6))
            Text("What would you like to know?")
                .font(.title2.weight(.semibold))
                .foregroundStyle(Theme.textPrimary)
            Text("I can look through your records and help you find the important details.")
                .font(.subheadline)
                .foregroundStyle(Theme.textSecondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 24)

            VStack(alignment: .leading, spacing: 8) {
                Text("Try asking")
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(Theme.textTertiary)
                ForEach([
                    "Which patients do I have?",
                    "Any recent treatments?",
                    "What happened this month?",
                ], id: \.self) { suggestion in
                    Button {
                        draft = suggestion
                        inputFocused = true
                    } label: {
                        HStack(spacing: 8) {
                            Image(systemName: "arrow.up.right")
                                .font(.caption.weight(.semibold))
                            Text(suggestion)
                                .font(.subheadline)
                            Spacer()
                        }
                        .foregroundStyle(theme.accentColor)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 10)
                        .background(theme.accentColor.opacity(0.08))
                        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                    }
                    .buttonStyle(.plain)
                    .accessibilityIdentifier("assistant_prompt_\(suggestion)")
                }
            }
            .frame(maxWidth: 360)
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
                    .foregroundStyle(theme.accentColor)
                    .frame(width: 40, height: 40)
                    .background(theme.accentColor.opacity(0.12))
                    .clipShape(Circle())
            }
            .disabled(viewModel.state.isGenerating || viewModel.state.isHistoryLoading)
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
                .disabled(viewModel.state.isGenerating || viewModel.state.isHistoryLoading)
                .accessibilityIdentifier("assistant_input")

            Button {
                sendDraft()
            } label: {
                Image(systemName: "arrow.up.circle.fill")
                    .font(.system(size: 32))
                    .foregroundStyle(canSend ? theme.accentColor : Theme.textTertiary)
                    .scaleEffect(viewModel.state.isGenerating ? 0.92 : 1.0)
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
        !draft.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty &&
            !viewModel.state.isGenerating &&
            !viewModel.state.isHistoryLoading
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
                    .fill(theme.accentColor.opacity(0.12))
                    .frame(width: 120, height: 120)
                Image(systemName: "sparkles")
                    .font(.system(size: 52))
                    .foregroundStyle(theme.accentColor)
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
                    .background(theme.accentColor)
                    .clipShape(Capsule())
            }

            Button {
                showDictation = true
            } label: {
                Label("Dictate records", systemImage: "mic.fill")
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(theme.accentColor)
                    .padding(.horizontal, 20)
                    .padding(.vertical, 10)
                    .background(theme.accentColor.opacity(0.10))
                    .clipShape(Capsule())
            }
            .accessibilityIdentifier("assistant_dictate")
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
    let accentColor: Color
    let onFollowUp: (String) -> Void
    let onOpenSource: (SearchResult) -> Void
    let onRetry: (() -> Void)?

    private var isUser: Bool {
        message.role == AssistantChatMessageRole.user
    }

    private var sourceGroups: [AssistantSourceGroup] {
        (message.sourceGroups as? [AssistantSourceGroup]) ?? []
    }

    private var followUps: [String] {
        (message.followUps as? [String]) ?? []
    }

    private var webSources: [VeterinaryWebSource] {
        (message.webSources as? [VeterinaryWebSource]) ?? []
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
                    .background(isUser ? accentColor : Theme.surfaceElevated)
                    .clipShape(ChatBubbleShape(isUser: isUser))
                if message.interrupted {
                    interruptedFooter
                }
                if !isUser && message.source == EngineSource.cloud {
                    cloudSourceFooter
                }
                if !isUser && !sourceGroups.isEmpty {
                    sourceChips
                }
                if !isUser && !webSources.isEmpty {
                    webReferenceChips
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
                        .foregroundStyle(accentColor)
                }
                .accessibilityIdentifier("assistant_retry")
            }
        }
    }

    /// Transparency badge: this answer came from the cloud model, not the
    /// on-device engine. Required so users always know where data was sent.
    private var cloudSourceFooter: some View {
        HStack(spacing: 4) {
            Image(systemName: "cloud.fill")
                .font(.caption2)
            Text("Answered by cloud model")
                .font(.caption2.weight(.medium))
                .accessibilityIdentifier("assistant_cloud_badge")
        }
        .foregroundStyle(Theme.textSecondary)
        .accessibilityElement(children: .combine)
        .accessibilityLabel("Answered by cloud model")
    }

    /// Tappable chips consolidated to one item per horse cited in this answer.
    private var sourceChips: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 6) {
                ForEach(Array(sourceGroups.enumerated()), id: \.offset) { _, group in
                    let source = group.primarySource
                    Button {
                        onOpenSource(source)
                    } label: {
                        HStack(spacing: 4) {
                            Image(systemName: source.recordType == "PATIENT" ? "horse" : "doc.text")
                                .font(.caption2)
                            Text(group.patientName.isEmpty ? source.recordType : group.patientName)
                                .font(.caption.weight(.medium))
                            if group.recordCount > 1 {
                                Text("\(group.recordCount)")
                                    .font(.caption2.weight(.bold))
                                    .padding(.horizontal, 5)
                                    .padding(.vertical, 2)
                                    .background(Theme.surfaceElevated.opacity(0.7))
                                    .clipShape(Capsule())
                            }
                        }
                        .padding(.horizontal, 10)
                        .padding(.vertical, 5)
                        .background(accentColor.opacity(0.12))
                        .foregroundStyle(accentColor)
                        .clipShape(Capsule())
                    }
                    .accessibilityIdentifier("assistant_source_chip")
                    .accessibilityLabel(
                        group.recordCount > 1
                            ? "Open \(group.recordCount) records for \(group.patientName)"
                            : "Open record for \(group.patientName)"
                    )
                }
            }
        }
    }

    /// Deterministic follow-up suggestions; tapping fills the input without
    /// sending so the vet can edit first.
    private var followUpChips: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 6) {
                ForEach(followUps, id: \.self) { suggestion in
                    Button {
                        onFollowUp(suggestion)
                    } label: {
                        Text(suggestion)
                            .font(.caption)
                            .fixedSize(horizontal: true, vertical: false)
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

    /// External literature links are visually separate from local record
    /// chips so a public reference can never look like a patient record.
    private var webReferenceChips: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 6) {
                ForEach(Array(webSources.enumerated()), id: \.offset) { _, source in
                    if let url = URL(string: source.url) {
                        Link(destination: url) {
                            HStack(spacing: 5) {
                                Image(systemName: "book.closed")
                                    .font(.caption2)
                                VStack(alignment: .leading, spacing: 1) {
                                    Text(source.title)
                                        .font(.caption.weight(.medium))
                                        .lineLimit(2)
                                    Text(source.publisher)
                                        .font(.caption2)
                                        .lineLimit(1)
                                }
                            }
                            .padding(.horizontal, 10)
                            .padding(.vertical, 6)
                            .background(Color.blue.opacity(0.10))
                            .foregroundStyle(.blue)
                            .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
                        }
                        .accessibilityIdentifier("assistant_web_source")
                        .accessibilityLabel("Open veterinary reference: \(source.title), \(source.publisher)")
                    }
                }
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
