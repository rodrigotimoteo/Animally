import SwiftUI
import Shared

/// Browseable view of the assistant's bounded, device-local conversation history.
///
/// The persistence boundary and retention policy live in shared Kotlin. This
/// view only searches and presents the iOS-friendly projection supplied by the
/// assistant store.
struct AssistantHistoryView: View {
    let conversations: [AssistantConversationItem]
    let isLoading: Bool
    let accentColor: Color
    let onUseQuestion: (String) -> Void
    let onOpenSource: (AssistantHistorySource) -> Void

    @Environment(\.dismiss) private var dismiss
    @State private var searchText = ""

    private var filteredConversations: [AssistantConversationItem] {
        let query = searchText.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !query.isEmpty else { return conversations }
        return conversations.filter { conversation in
            conversation.title.localizedCaseInsensitiveContains(query) ||
                conversation.preview.localizedCaseInsensitiveContains(query) ||
                conversation.turns.contains { turn in
                    turn.question.localizedCaseInsensitiveContains(query) ||
                        turn.answer.localizedCaseInsensitiveContains(query)
                }
        }
    }

    var body: some View {
        NavigationStack {
            Group {
                if isLoading && conversations.isEmpty {
                    ProgressView("Loading chat history…")
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                        .accessibilityIdentifier("assistant_history_loading")
                } else if conversations.isEmpty {
                    ContentUnavailableView(
                        "No saved chats",
                        systemImage: "bubble.left.and.bubble.right",
                        description: Text(
                            "Completed conversations will appear here. The latest 15 turns stay on this device, grouped into chat blocks."
                        )
                    )
                    .accessibilityIdentifier("assistant_history_empty")
                } else if filteredConversations.isEmpty {
                    ContentUnavailableView.search(text: searchText)
                } else {
                    historyList
                }
            }
            .navigationTitle("Chat history")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("Done") { dismiss() }
                        .accessibilityIdentifier("assistant_history_done")
                }
            }
            .searchable(text: $searchText, prompt: "Search chats")
        }
        .accessibilityIdentifier("assistant_chat_history")
    }

    private var historyList: some View {
        List {
            Section {
                ForEach(filteredConversations, id: \.id) { conversation in
                    NavigationLink {
                        AssistantConversationDetailView(
                            conversation: conversation,
                            accentColor: accentColor,
                            onUseQuestion: onUseQuestion,
                            onOpenSource: onOpenSource
                        )
                    } label: {
                        conversationRow(conversation)
                    }
                    .accessibilityIdentifier("assistant_conversation_\(conversation.id)")
                    .accessibilityHint("Opens the complete conversation")
                }
            } header: {
                Text("\(filteredConversations.count) recent \(filteredConversations.count == 1 ? "conversation" : "conversations")")
            } footer: {
                Text("Saved locally on this device. Tap a conversation to read every exchange or reuse a question.")
            }
        }
        .listStyle(.insetGrouped)
    }

    private func conversationRow(_ conversation: AssistantConversationItem) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack(spacing: 8) {
                Image(systemName: "clock")
                Text(dateText(for: conversation.updatedAtMillis))
                Spacer(minLength: 4)
                Text("\(conversation.turnCount) \(conversation.turnCount == 1 ? "exchange" : "exchanges")")
            }
            .font(.caption)
            .foregroundStyle(Theme.textSecondary)

            Text(conversation.title)
                .font(.headline)
                .foregroundStyle(Theme.textPrimary)
                .lineLimit(2)

            Text(conversation.preview.isEmpty ? "No response saved" : conversation.preview)
                .font(.subheadline)
                .foregroundStyle(Theme.textSecondary)
                .lineLimit(3)

            if conversation.turns.contains(where: \.interrupted) {
                Label("Partial response saved", systemImage: "exclamationmark.triangle")
                    .font(.caption2.weight(.medium))
                    .foregroundStyle(Theme.amber)
            }
        }
        .padding(.vertical, 6)
    }

    private func dateText(for millis: Int64) -> String {
        Date(timeIntervalSince1970: TimeInterval(millis) / 1000)
            .formatted(date: .abbreviated, time: .shortened)
    }
}

/// Full-screen detail for one persisted multi-turn conversation.
private struct AssistantConversationDetailView: View {
    let conversation: AssistantConversationItem
    let accentColor: Color
    let onUseQuestion: (String) -> Void
    let onOpenSource: (AssistantHistorySource) -> Void

    @Environment(\.dismiss) private var dismiss

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                conversationHeader

                ForEach(conversation.turns, id: \.id) { turn in
                    VStack(alignment: .leading, spacing: 10) {
                        exchangeCard(
                            title: "You asked",
                            text: turn.question,
                            isUser: true
                        )

                        exchangeCard(
                            title: "Assistant",
                            text: turn.answer.isEmpty ? "No response saved" : turn.answer,
                            isUser: false
                        )

                        sourceBadge(for: turn)

                        recordSourceLinks(for: turn)

                        webReferenceLinks(for: turn)

                        if turn.interrupted {
                            Label(
                                "This response was saved after generation stopped early.",
                                systemImage: "exclamationmark.triangle"
                            )
                            .font(.footnote)
                            .foregroundStyle(Theme.amber)
                        }

                        Button {
                            onUseQuestion(turn.question)
                            dismiss()
                        } label: {
                            Label("Use this question", systemImage: "arrow.uturn.right")
                        }
                        .font(.footnote.weight(.medium))
                        .buttonStyle(.bordered)
                        .accessibilityIdentifier("assistant_history_use_question_\(turn.id)")
                    }
                    .accessibilityIdentifier("assistant_history_turn_\(turn.id)")
                }
            }
            .padding()
        }
        .background(Theme.surfaceElevated.opacity(0.35))
        .navigationTitle("Conversation")
        .navigationBarTitleDisplayMode(.inline)
        .accessibilityIdentifier("assistant_history_detail")
    }

    private var conversationHeader: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(spacing: 8) {
                Image(systemName: "bubble.left.and.bubble.right")
                Text("\(conversation.turnCount) \(conversation.turnCount == 1 ? "exchange" : "exchanges")")
                Spacer()
                Text(dateRangeText)
            }
            .font(.caption)
            .foregroundStyle(Theme.textSecondary)

            Text(conversation.title)
                .font(.title3.weight(.semibold))
                .foregroundStyle(Theme.textPrimary)
        }
    }

    private func exchangeCard(title: String, text: String, isUser: Bool) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(title)
                .font(.caption.weight(.semibold))
                .foregroundStyle(Theme.textSecondary)

            Text(text)
                .font(.body)
                .foregroundStyle(isUser ? .white : Theme.textPrimary)
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(14)
                .background(isUser ? accentColor : Theme.surfaceElevated)
                .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
        }
    }

    private func sourceBadge(for turn: AssistantHistoryItem) -> some View {
        let isCloud = turn.source.uppercased() == "CLOUD"
        return Label(
            isCloud ? "Answered by cloud model" : "Answered on device",
            systemImage: isCloud ? "cloud.fill" : "iphone"
        )
        .font(.caption)
        .foregroundStyle(Theme.textSecondary)
    }

    private func webReferenceLinks(for turn: AssistantHistoryItem) -> some View {
        let sources = (turn.webSources as? [VeterinaryWebSource]) ?? []
        return Group {
            if !sources.isEmpty {
                VStack(alignment: .leading, spacing: 6) {
                    Label("Public veterinary references", systemImage: "book.closed")
                        .font(.caption.weight(.medium))
                        .foregroundStyle(Theme.textSecondary)
                    ForEach(Array(sources.enumerated()), id: \.offset) { _, source in
                        if let url = URL(string: source.url) {
                            Link(destination: url) {
                                HStack(alignment: .top, spacing: 8) {
                                    Image(systemName: "arrow.up.right.square")
                                        .font(.caption)
                                    VStack(alignment: .leading, spacing: 2) {
                                        Text(source.title)
                                            .font(.footnote.weight(.medium))
                                            .multilineTextAlignment(.leading)
                                        Text(source.publisher)
                                            .font(.caption2)
                                    }
                                    Spacer(minLength: 0)
                                }
                                .foregroundStyle(accentColor)
                            }
                            .accessibilityIdentifier("assistant_history_web_source")
                        }
                    }
                }
                .padding(12)
                .background(Theme.surfaceElevated)
                .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
            }
        }
    }

    private func recordSourceLinks(for turn: AssistantHistoryItem) -> some View {
        let sources = (turn.recordSources as? [AssistantHistorySource]) ?? []
        return Group {
            if !sources.isEmpty {
                VStack(alignment: .leading, spacing: 6) {
                    Label("Patient records", systemImage: "doc.text.magnifyingglass")
                        .font(.caption.weight(.medium))
                        .foregroundStyle(Theme.textSecondary)
                    ForEach(Array(sources.enumerated()), id: \.offset) { _, source in
                        Button {
                            onOpenSource(source)
                        } label: {
                            HStack(alignment: .top, spacing: 8) {
                                Image(systemName: "arrow.up.right.square")
                                    .font(.caption)
                                VStack(alignment: .leading, spacing: 2) {
                                    Text(source.patientName)
                                        .font(.footnote.weight(.medium))
                                        .multilineTextAlignment(.leading)
                                    Text(recordTypeLabel(source.recordType) + (source.date.map { " · \($0)" } ?? ""))
                                        .font(.caption2)
                                }
                                Spacer(minLength: 0)
                            }
                            .foregroundStyle(accentColor)
                            .frame(maxWidth: .infinity, alignment: .leading)
                        }
                        .buttonStyle(.plain)
                        .accessibilityIdentifier("assistant_history_record_source")
                        .accessibilityLabel("Open \(source.patientName), \(recordTypeLabel(source.recordType))")
                    }
                }
                .padding(12)
                .background(Theme.surfaceElevated)
                .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
            }
        }
    }

    private func recordTypeLabel(_ rawType: String) -> String {
        switch rawType.uppercased() {
        case "FARRIER_VISIT": return "Farrier visit"
        case "PATIENT": return "Patient"
        case "OWNER": return "Owner"
        case "REPRODUCTION": return "Breeding record"
        default:
            return rawType
                .replacingOccurrences(of: "_", with: " ")
                .lowercased()
                .capitalized
        }
    }

    private var dateRangeText: String {
        let formatter = Date.FormatStyle(date: .abbreviated, time: .shortened)
        let start = Date(timeIntervalSince1970: TimeInterval(conversation.createdAtMillis) / 1000)
        let end = Date(timeIntervalSince1970: TimeInterval(conversation.updatedAtMillis) / 1000)
        return start == end ? start.formatted(formatter) : "\(start.formatted(formatter)) – \(end.formatted(formatter))"
    }
}
