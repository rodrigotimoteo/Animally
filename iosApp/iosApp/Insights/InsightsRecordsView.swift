import SwiftUI
import Shared

@MainActor
final class InsightsRecordsListViewModel: ObservableObject {
    @Published var state: InsightsRecordsUiState
    private let store: InsightsRecordsStore
    private var cancellable: NativeCancellable?

    init(drillDown: InsightsDrillDown) {
        store = IosInsightsStores.shared.insightsRecordsStore(drillDown: drillDown)
        state = store.state.current
        cancellable = store.state.subscribe(onEach: { [weak self] newState in
            Task { @MainActor in
                self?.state = newState
            }
        })
    }

    @Published var awaitError: String?

    func reload() { store.reload(); awaitError = nil }

    func reloadAsync() async -> Bool {
        awaitError = nil
        reload()
        let ok = await StoreAwait.awaitIdle(store.state, isLoading: { $0.isLoading })
        if !ok { awaitError = StoreAwaitError.timeout.localizedDescription }
        return ok
    }

    func dismissError() { store.dismissError() }
    func dismissAwaitError() { awaitError = nil }

    deinit {
        cancellable?.cancel()
        store.clear()
    }
}

struct InsightsRecordsView: View {
    @StateObject private var viewModel: InsightsRecordsListViewModel
    @State private var selectedKey: RecordDetailKey?
    private let drillDown: InsightsDrillDown
    private let patientName: String?

    init(drillDown: InsightsDrillDown, patientName: String? = nil) {
        self.drillDown = drillDown
        self.patientName = patientName
        _viewModel = StateObject(wrappedValue: InsightsRecordsListViewModel(drillDown: drillDown))
    }

    var body: some View {
        Group {
            // Error precedence: full-screen error only when refs empty (initial load failure).
            // When refs non-empty, inline banner via .overlay preserves list content — not redundant.
            if viewModel.state.isLoading && viewModel.state.refs.isEmpty {
                loadingView
            } else if let error = viewModel.state.errorMessage, viewModel.state.refs.isEmpty {
                // Deduplicated branch: error with empty refs → full error view
                errorView(message: error)
            } else if viewModel.state.refs.isEmpty {
                emptyView
            } else {
                recordsList
            }
        }
        .navigationTitle(navigationTitle)
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button {
                    viewModel.reload()
                } label: {
                    Image(systemName: "arrow.clockwise")
                        .accessibilityLabel("Reload records")
                }
                .disabled(viewModel.state.isLoading)
            }
        }
        .overlay(alignment: .top) {
            VStack(spacing: 8) {
                if let error = viewModel.state.errorMessage, !viewModel.state.refs.isEmpty {
                    errorBanner(message: error)
                }
                if let a = viewModel.awaitError {
                    InlineErrorBanner(message: a, onRetry: { Task { await viewModel.reloadAsync() } }, onDismiss: { viewModel.dismissAwaitError() })
                }
            }
        }
        .refreshable {
            await viewModel.reloadAsync()
        }
        .navigationDestination(item: $selectedKey) { key in
            RecordDetailView(displayType: key.displayType, patientId: key.patientId, recordId: key.recordId)
        }
    }

    private var navigationTitle: String {
        if let eventType = drillDown.reproductionEventType {
            return eventType.displayLabel
        }
        if let type = drillDown.recordType {
            return type.displayName
        }
        return "Records"
    }

    private var recordsList: some View {
        List {
            Section {
                ForEach(viewModel.state.refs, id: \.stableId) { ref in
                    Button {
                        selectedKey = RecordDetailKey(
                            displayType: ref.recordType.wireName,
                            patientId: ref.patientId,
                            recordId: ref.recordId
                        )
                    } label: {
                        recordRow(ref: ref)
                    }
                    .buttonStyle(.plain)
                    .accessibilityLabel("\(ref.recordType.displayName), \(ref.patientName), \(ref.date.displayString)")
                    .accessibilityHint("Opens record detail")
                    .accessibilityIdentifier("insights_record_\(ref.recordType.wireName)_\(ref.recordId)")
                }
            } header: {
                headerView
            }
        }
        .listStyle(.insetGrouped)
        .accessibilityIdentifier("insights_records_list")
    }

    private var headerView: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(headerSubtitle)
                .font(.caption)
                .foregroundStyle(Theme.textSecondary)
                .textCase(nil)
            Text("\(viewModel.state.refs.count) record\(viewModel.state.refs.count == 1 ? "" : "s")")
                .font(.caption.weight(.semibold))
                .foregroundStyle(Theme.forestGreen)
                .textCase(nil)
        }
        .padding(.vertical, 4)
    }

    private var headerSubtitle: String {
        let fromStr = drillDown.from.displayString
        let toStr = drillDown.to.displayString
        let scope = drillDown.patientId == nil
            ? "All active patients"
            : patientName?.nonEmpty ?? "Selected patient"
        let type = drillDown.reproductionEventType?.displayLabel
            ?? drillDown.recordType?.displayName
            ?? "All types"
        return "\(type) · \(scope) · \(fromStr) – \(toStr)"
    }

    private func recordRow(ref: InsightsRecordRef) -> some View {
        RecordRowView(
            icon: RecordTypeIcon.systemName(for: ref.recordType),
            iconTint: Theme.forestGreen,
            title: ref.recordType.displayName,
            subtitle: ref.patientName,
            date: ref.date.displayString
        )
    }

    private var loadingView: some View {
        VStack(spacing: 16) {
            ProgressView().scaleEffect(1.2)
            Text("Loading records…")
                .font(.subheadline)
                .foregroundStyle(Theme.textSecondary)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .accessibilityLabel("Loading records")
    }

    private var emptyView: some View {
        VStack(spacing: 16) {
            Image(systemName: "doc.text.magnifyingglass")
                .font(.system(size: 48))
                .foregroundStyle(Theme.forestGreen.opacity(0.4))
            Text("No records for this filter")
                .font(.headline)
                .foregroundStyle(Theme.textPrimary)
            Text("Try a different date range or record type.")
                .font(.subheadline)
                .foregroundStyle(Theme.textSecondary)
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .padding()
        .accessibilityLabel("No records for this filter")
        .accessibilityIdentifier("insights_records_empty")
    }

    private func errorView(message: String) -> some View {
        VStack(spacing: 16) {
            Image(systemName: "exclamationmark.triangle.fill")
                .font(.system(size: 48))
                .foregroundStyle(Theme.amber)
            Text(message)
                .font(.subheadline)
                .foregroundStyle(Theme.textPrimary)
                .multilineTextAlignment(.center)
            Button("Retry") { viewModel.reload() }
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(Theme.forestGreen)
                .accessibilityLabel("Retry loading records")
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .padding()
    }

    private func errorBanner(message: String) -> some View {
        InlineErrorBanner(systemImage: "exclamationmark.triangle.fill", message: message, tint: Theme.amber, showsRetry: true, onRetry: { viewModel.reload() }, onDismiss: { viewModel.dismissError() })
    }


}

private extension String {
    var nonEmpty: String? {
        isEmpty ? nil : self
    }
}

// Stable identity for InsightsRecordRef — ignores mutable display fields (patientName/date).
// Uses deterministic composite "\(recordType.wireName)-\(patientId)-\(recordId)" per S3 oracle.
private extension InsightsRecordRef {
    var stableId: String { "\(recordType.wireName)-\(patientId)-\(recordId)" }
}
