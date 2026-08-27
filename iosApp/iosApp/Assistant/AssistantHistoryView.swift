import SwiftUI
import Shared

/// Browseable view of the assistant's bounded, device-local turn history.
///
/// The persistence boundary and retention policy live in shared Kotlin. This
/// view only searches and presents the iOS-friendly projection supplied by the
/// assistant store.
struct AssistantHistoryView: View {
    let turns: [AssistantHistoryItem]
    let isLoading: Bool
    let onUseQuestion: (String) -> Void

    @Environment(\.dismiss) private var dismiss
    @State private var searchText = ""

    private var filteredTurns: [AssistantHistoryItem] {
        let newestFirst = Array(turns.reversed())
        let query = searchText.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !query.isEmpty else { return newestFirst }
        return newestFirst.filter { turn in
            turn.question.localizedCaseInsensitiveContains(query) ||
                turn.answer.localizedCaseInsensitiveContains(query)
        }
    }

    var body: some View {
        NavigationStack {
            Group {
                if isLoading && turns.isEmpty {
                    ProgressView("Loading chat history…")
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                        .accessibilityIdentifier("assistant_history_loading")
                } else if turns.isEmpty {
                    ContentUnavailableView(
                        "No saved chats",
                        systemImage: "bubble.left.and.bubble.right",
                        description: Text(
                            "Completed assistant questions and answers will appear here. The latest 15 turns stay on this device."
                        )
                    )
                    .accessibilityIdentifier("assistant_history_empty")
                } else if filteredTurns.isEmpty {
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
                ForEach(filteredTurns, id: \.id) { turn in
                    NavigationLink {
                        AssistantHistoryDetailView(
                            turn: turn,
                            onUseQuestion: { onUseQuestion(turn.question) }
                        )
                    } label: {
                        historyRow(turn)
                    }
                    .accessibilityIdentifier("assistant_history_\(turn.id)")
                    .accessibilityHint("Opens the saved question and answer")
                }
            } header: {
                Text("\(filteredTurns.count) recent \(filteredTurns.count == 1 ? "chat" : "chats")")
            } footer: {
                Text("Saved locally on this device. Tap a chat to read the full answer or reuse its question.")
            }
        }
        .listStyle(.insetGrouped)
    }

    private func historyRow(_ turn: AssistantHistoryItem) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack(spacing: 8) {
                Image(systemName: "clock")
                Text(dateText(for: turn))
                Spacer(minLength: 4)
                sourceBadge(for: turn)
            }
            .font(.caption)
            .foregroundStyle(Theme.textSecondary)

            Text(turn.question)
                .font(.headline)
                .foregroundStyle(Theme.textPrimary)
                .lineLimit(2)

            Text(turn.answer.isEmpty ? "No response saved" : turn.answer)
                .font(.subheadline)
                .foregroundStyle(Theme.textSecondary)
                .lineLimit(3)

            if turn.interrupted {
                Label("Partial response saved", systemImage: "exclamationmark.triangle")
                    .font(.caption2.weight(.medium))
                    .foregroundStyle(Theme.amber)
            }
        }
        .padding(.vertical, 6)
    }

    private func sourceBadge(for turn: AssistantHistoryItem) -> some View {
        Label(
            turn.source.uppercased() == "CLOUD" ? "Cloud" : "On-device",
            systemImage: turn.source.uppercased() == "CLOUD" ? "cloud.fill" : "iphone"
        )
        .font(.caption2.weight(.medium))
    }

    private func dateText(for turn: AssistantHistoryItem) -> String {
        Date(timeIntervalSince1970: TimeInterval(turn.createdAtMillis) / 1000)
            .formatted(date: .abbreviated, time: .shortened)
    }
}

/// Full-screen detail for one saved assistant exchange.
private struct AssistantHistoryDetailView: View {
    let turn: AssistantHistoryItem
    let onUseQuestion: () -> Void

    @Environment(\.dismiss) private var dismiss

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                HStack(spacing: 8) {
                    Image(systemName: turn.source.uppercased() == "CLOUD" ? "cloud.fill" : "iphone")
                    Text(turn.source.uppercased() == "CLOUD" ? "Answered by cloud model" : "Answered on device")
                    Spacer()
                    Text(dateText)
                }
                .font(.caption)
                .foregroundStyle(Theme.textSecondary)

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

                if turn.interrupted {
                    Label(
                        "This response was saved after generation stopped early.",
                        systemImage: "exclamationmark.triangle"
                    )
                    .font(.footnote)
                    .foregroundStyle(Theme.amber)
                }

                Button {
                    onUseQuestion()
                    dismiss()
                } label: {
                    Label("Use question", systemImage: "arrow.uturn.right")
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.borderedProminent)
                .accessibilityIdentifier("assistant_history_use_question")
            }
            .padding()
        }
        .background(Theme.surfaceElevated.opacity(0.35))
        .navigationTitle("Saved chat")
        .navigationBarTitleDisplayMode(.inline)
        .accessibilityIdentifier("assistant_history_detail")
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
                .background(isUser ? Theme.forestGreen : Theme.surfaceElevated)
                .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
        }
    }

    private var dateText: String {
        Date(timeIntervalSince1970: TimeInterval(turn.createdAtMillis) / 1000)
            .formatted(date: .abbreviated, time: .shortened)
    }
}
