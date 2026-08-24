import Foundation
import Shared
import SwiftUI

/// Navigation payload for the read-only record detail screen.
struct RecordDetailNav: Identifiable, Hashable {
    struct FieldRow: Identifiable, Hashable {
        let label: String
        let value: String
        var id: String { label }

        init(label: String, value: String) {
            self.label = label
            self.value = value
        }
    }

    let id = UUID()
    let title: String
    let displayType: String
    let patientId: Int64
    let recordId: Int64
    let fields: [FieldRow]
}

/// Hashable navigation payload for opening the detail by record identity:
/// the view loads the record itself instead of receiving eager field rows.
struct RecordDetailKey: Hashable {
    let displayType: String
    let patientId: Int64
    let recordId: Int64
}

/// Read-only view of everything inside one record. "Edit" pushes the
/// prefilled form on top of this detail; saving or cancelling returns here,
/// and the store subscription re-renders freshly saved data. Back returns to
/// the caller (tab, timeline, or search).
///
/// Always id-loaded: all per-type knowledge (store binding, field-row
/// building, title normalization, edit-route availability) lives behind the
/// Kotlin `RecordDetailOpener` facade, so this view only renders whatever
/// typed rows the handle's flow emits.
struct RecordDetailView: View {
    let key: RecordDetailKey
    let fallbackTitle: String

    init(nav: RecordDetailNav) {
        key = RecordDetailKey(
            displayType: nav.displayType,
            patientId: nav.patientId,
            recordId: nav.recordId
        )
        fallbackTitle = nav.title
    }

    init(
        displayType: String,
        patientId: Int64,
        recordId: Int64
    ) {
        key = RecordDetailKey(
            displayType: displayType,
            patientId: patientId,
            recordId: recordId
        )
        fallbackTitle = displayType
    }

    var body: some View {
        IdLoadedRecordDetailView(key: key, fallbackTitle: fallbackTitle)
    }
}

private struct FieldCell: View {
    let field: RecordDetailNav.FieldRow

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(field.label)
                .font(.caption)
                .foregroundStyle(Theme.textSecondary)
            Text(field.value)
                .font(.subheadline)
                .foregroundStyle(Theme.textPrimary)
        }
        .padding(.vertical, 3)
    }
}

/// Renders the field-row flow emitted by the Kotlin-provided detail handle.
/// Owns the Edit push so saving or cancelling pops back here with
/// re-emitted (fresh) data.
private struct IdLoadedRecordDetailView: View {
    let key: RecordDetailKey
    let fallbackTitle: String

    @StateObject private var observer: RecordDetailObserver
    @State private var editRoute: RecordEditRoute?

    /// Nil when the display type has no editor route; disables Edit.
    private var editDestination: RecordEditRoute? {
        guard let descriptor = observer.editRouteDescriptor else { return nil }
        return RecordEditRoute(descriptor: descriptor)
    }

    init(
        key: RecordDetailKey,
        fallbackTitle: String
    ) {
        self.key = key
        self.fallbackTitle = fallbackTitle
        _observer = StateObject(wrappedValue: RecordDetailObserver(key: key))
    }

    var body: some View {
        Group {
            if observer.isLoading {
                VStack(spacing: 16) {
                    ProgressView()
                        .scaleEffect(1.2)
                    Text("Loading \(fallbackTitle)…")
                        .font(.subheadline)
                        .foregroundStyle(Theme.textSecondary)
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else if let fields = observer.fields {
                List {
                    Section {
                        ForEach(fields) { field in
                            FieldCell(field: field)
                        }
                    }
                }
                .listStyle(.insetGrouped)
            } else {
                VStack(spacing: 20) {
                    Image(systemName: "exclamationmark.triangle")
                        .font(.system(size: 64))
                        .foregroundStyle(Theme.amber)
                    Text("Record not found")
                        .font(.title2.weight(.semibold))
                        .foregroundStyle(Theme.textPrimary)
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            }
        }
        .navigationTitle(observer.title ?? fallbackTitle)
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button {
                    editRoute = editDestination
                } label: {
                    Text("Edit")
                        .font(.subheadline.weight(.semibold))
                }
                .disabled(observer.fields == nil || editDestination == nil)
            }
        }
        .navigationDestination(item: $editRoute) { route in
            recordEditDestination(route)
        }
    }
}

/// Subscribes to the Kotlin `RecordDetailOpener`'s unified state flow and
/// converts each emitted state into ready-to-render field rows (or
/// loading / not-found markers). No per-type logic remains here: the handle
/// carries the title, the typed rows, and the edit-route descriptor.
@MainActor
final class RecordDetailObserver: ObservableObject {
    @Published private(set) var fields: [RecordDetailNav.FieldRow]?
    @Published private(set) var isLoading = true
    @Published private(set) var title: String?
    @Published private(set) var editRouteDescriptor: RecordEditRouteDescriptor?

    private var cancellable: NativeCancellable?
    private let handle: RecordDetailHandle

    init(key: RecordDetailKey) {
        let handle = RecordDetailOpener.shared.openDetail(
            recordTypeWireName: key.displayType,
            recordId: key.recordId,
            patientId: KotlinLong(longLong: key.patientId)
        )
        self.handle = handle
        title = handle.title
        editRouteDescriptor = handle.editRoute
        apply(handle.state.current)
        cancellable = handle.state.subscribe(onEach: { [weak self] state in
            Task { @MainActor in
                self?.apply(state)
            }
        })
    }

    private func apply(_ state: RecordDetailState) {
        var rows = state.rows?.map { RecordDetailNav.FieldRow(label: $0.label, value: $0.value) }
        if rows?.isEmpty == true, !state.isLoading {
            rows = nil
        }
        fields = rows
        isLoading = state.isLoading
    }

    deinit {
        cancellable?.cancel()
        handle.dispose()
    }
}
