import SwiftUI
import Shared
import os

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

    func reload() {
        store.reload()
    }

    func reloadAsync() async {
        reload()
        for _ in 0..<50 {
            if !state.isLoading { break }
            try? await Task.sleep(nanoseconds: 100_000_000)
        }
        if state.isLoading {
            // 5s bound reached but isLoading still true → log timeout (spinner hang guard)
            Logger(subsystem: "com.animally.insights", category: "insights")
                .error("InsightsRecords reloadAsync timeout: isLoading still true after 5s")
        }
        try? await Task.sleep(nanoseconds: 50_000_000)
    }

    func dismissError() {
        store.dismissError()
    }

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
            if let error = viewModel.state.errorMessage, !viewModel.state.refs.isEmpty {
                errorBanner(message: error)
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
        HStack(spacing: 12) {
            Image(systemName: iconForRecordType(ref.recordType))
                .font(.body)
                .foregroundStyle(Theme.forestGreen)
                .frame(width: 36, height: 36)
                .background(Theme.forestGreen.opacity(0.12))
                .clipShape(RoundedRectangle(cornerRadius: 8))
                .accessibilityHidden(true)

            VStack(alignment: .leading, spacing: 3) {
                Text(ref.recordType.displayName)
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(Theme.textPrimary)
                    .lineLimit(1)
                Text(ref.patientName)
                    .font(.caption)
                    .foregroundStyle(Theme.textSecondary)
                    .lineLimit(1)
            }

            Spacer()

            Text(ref.date.displayString)
                .font(.caption)
                .foregroundStyle(Theme.textTertiary)
                .lineLimit(1)
                .fixedSize(horizontal: true, vertical: false)
        }
        .padding(.vertical, 4)
        .contentShape(Rectangle())
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
        HStack(spacing: 12) {
            Image(systemName: "exclamationmark.triangle.fill").foregroundStyle(Theme.amber)
            Text(message).font(.subheadline).foregroundStyle(Theme.textPrimary).lineLimit(2)
            Spacer()
            Button("Retry") { viewModel.reload() }
                .font(.caption.weight(.bold))
                .foregroundStyle(Theme.forestGreen)
            Button {
                viewModel.dismissError()
            } label: {
                Image(systemName: "xmark").font(.caption.weight(.bold)).foregroundStyle(Theme.textSecondary)
                    .accessibilityLabel("Dismiss error")
            }
        }
        .padding(12)
        .background(Theme.surfaceElevated)
        .clipShape(RoundedRectangle(cornerRadius: 12))
        .shadow(color: .black.opacity(0.08), radius: 8, y: 2)
        .padding(.horizontal)
        .padding(.top, 8)
    }

    private func iconForRecordType(_ type: RecordType) -> String {
        switch type.wireName {
        case "VACCINATION": return "syringe.fill"
        case "DEWORMING": return "pills.fill"
        case "CONSULTATION": return "stethoscope"
        case "WEIGHT": return "scalemass.fill"
        case "REPRODUCTION_EVENT": return "heart.fill"
        case "FARRIER_VISIT": return "figure.walk"
        case "DENTISTRY": return "mouth.fill"
        case "CUSTOM_REMINDER": return "bell.badge.fill"
        case "EMBRYO_TRANSFER": return "arrow.triangle.branch"
        case "ICSI": return "scope"
        case "ULTRASOUND": return "waveform.path.ecg"
        case "GESTATION": return "heart.circle.fill"
        case "REPRO_MEDICATION": return "pills"
        case "LAB_RESULT": return "testtube.2"
        case "IMAGING": return "photo.fill"
        case "LAMENESS": return "figure.walk.motion"
        case "SURGERY": return "cross.case.fill"
        case "MEDICATION": return "pill.fill"
        case "CONTROLLED_SUBSTANCE": return "cross.vial.fill"
        case "ANAMNESE": return "doc.text.fill"
        default: return "doc.text.fill"
        }
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
