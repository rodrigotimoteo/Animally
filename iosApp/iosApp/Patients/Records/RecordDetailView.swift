import Foundation
import Shared
import SwiftUI

/// Navigation payload for the read-only record detail screen.
/// Deterministic identity derived from patientId+recordId+displayType so the same
/// record always hashes identically and Back/Push diffs remain stable.
struct RecordDetailNav: Identifiable, Hashable {
    struct FieldRow: Identifiable, Hashable {
        let label: String
        let value: String
        let index: Int
        var id: String { "\(index)-\(label)-\(value)" }

        init(label: String, value: String, index: Int = 0) {
            self.label = label
            self.value = value
            self.index = index
        }
    }

    let title: String
    let displayType: String
    let patientId: Int64
    let recordId: Int64
    let fields: [FieldRow]

    var id: String { "\(patientId)-\(recordId)-\(displayType)" }

    func hash(into hasher: inout Hasher) {
        hasher.combine(patientId)
        hasher.combine(recordId)
        hasher.combine(displayType)
    }

    static func == (lhs: RecordDetailNav, rhs: RecordDetailNav) -> Bool {
        lhs.patientId == rhs.patientId && lhs.recordId == rhs.recordId && lhs.displayType == rhs.displayType
    }
}

/// Hashable navigation payload for opening the detail by record identity:
/// the view loads the record itself instead of receiving eager field rows.
/// Deterministic Identifiable so navigationDestination(item:) diff stable.
struct RecordDetailKey: Hashable, Identifiable {
    let displayType: String
    let patientId: Int64
    let recordId: Int64

    var id: String { "\(patientId)-\(recordId)-\(displayType)" }
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
            } else if observer.fields != nil || !observer.attachments.isEmpty {
                List {
                    if let fields = observer.fields, !fields.isEmpty {
                        Section {
                            ForEach(fields) { field in
                                FieldCell(field: field)
                            }
                        }
                    }

                    if !observer.attachments.isEmpty {
                        Section {
                            RecordDetailAttachmentGallery(attachments: observer.attachments)
                        } header: {
                            Label("Images", systemImage: "photo.on.rectangle.angled")
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
        .onChange(of: editRoute) { oldValue, newValue in
            // The editor can be backed by a different Koin view-model
            // instance than this read-only observer. Reload when it is
            // dismissed so an edited record never shows stale values.
            if oldValue != nil, newValue == nil {
                observer.reload()
            }
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
    @Published private(set) var attachments: [RecordDetailAttachment] = []
    @Published private(set) var isLoading = true
    @Published private(set) var title: String?
    @Published private(set) var editRouteDescriptor: RecordEditRouteDescriptor?

    private var cancellable: NativeCancellable?
    private let key: RecordDetailKey
    private var handle: RecordDetailHandle?
    private var generation = 0

    init(key: RecordDetailKey) {
        self.key = key
        reload()
    }

    /// Reopens the backing Kotlin store after an edit destination is popped.
    /// Each reload gets a generation so a queued callback from the previous
    /// store cannot overwrite the freshly loaded state.
    func reload() {
        generation += 1
        let currentGeneration = generation
        cancellable?.cancel()
        handle?.dispose()

        let nextHandle = RecordDetailOpener.shared.openDetail(
            recordTypeWireName: key.displayType,
            recordId: key.recordId,
            patientId: KotlinLong(longLong: key.patientId)
        )
        handle = nextHandle
        title = nextHandle.title
        editRouteDescriptor = nextHandle.editRoute
        apply(nextHandle.state.current)
        cancellable = nextHandle.state.subscribe(onEach: { [weak self] state in
            Task { @MainActor in
                guard let self, self.generation == currentGeneration else { return }
                self.apply(state)
            }
        })
    }

    private func apply(_ state: RecordDetailState) {
        var rows = state.rows?.enumerated().map { idx, row in
            RecordDetailNav.FieldRow(label: row.label, value: row.value, index: idx)
        }
        if rows?.isEmpty == true, !state.isLoading {
            rows = nil
        }
        fields = rows
        attachments = state.attachments
        isLoading = state.isLoading
    }

    deinit {
        cancellable?.cancel()
        handle?.dispose()
    }
}
