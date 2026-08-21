import SwiftUI
import Shared

struct AssistantView: View {
    @StateObject private var viewModel = AssistantViewModel()
    @State private var draft: String = ""

    var body: some View {
        NavigationStack {
            Group {
                if isAvailable {
                    chatContent
                } else {
                    unavailableView
                }
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
                        ChatBubble(message: message)
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
        .background(Theme.surfaceElevated.opacity(0.35))
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
    }

    private var inputBar: some View {
        HStack(spacing: 12) {
            TextField("Ask a question…", text: $draft, axis: .vertical)
                .textFieldStyle(.plain)
                .lineLimit(1...4)
                .padding(.horizontal, 14)
                .padding(.vertical, 10)
                .background(Theme.surfaceElevated)
                .clipShape(RoundedRectangle(cornerRadius: 20))
                .disabled(viewModel.state.isGenerating)

            Button {
                sendDraft()
            } label: {
                Image(systemName: "arrow.up.circle.fill")
                    .font(.system(size: 32))
                    .foregroundStyle(canSend ? Theme.forestGreen : Theme.textTertiary)
            }
            .disabled(!canSend)
            .accessibilityLabel("Send message")
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
        viewModel.ask(question: question)
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

    private var isUser: Bool {
        message.role == AssistantChatMessageRole.user
    }

    var body: some View {
        HStack(alignment: .bottom) {
            if isUser { Spacer(minLength: 48) }
            Text(message.text)
                .font(.subheadline)
                .foregroundStyle(isUser ? .white : Theme.textPrimary)
                .padding(.horizontal, 14)
                .padding(.vertical, 10)
                .background(isUser ? Theme.forestGreen : Theme.surfaceElevated)
                .clipShape(ChatBubbleShape(isUser: isUser))
            if !isUser { Spacer(minLength: 48) }
        }
        .accessibilityLabel("\(isUser ? "You" : "Assistant"): \(message.text)")
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
