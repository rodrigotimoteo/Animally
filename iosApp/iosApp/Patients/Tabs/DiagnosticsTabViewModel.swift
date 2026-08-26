import Foundation
import Shared

/// Manages the Diagnostics tab: lab results and imaging.
@MainActor
final class DiagnosticsTabViewModel: ObservableObject {
    @Published var labResults: [LabResult_] = []
    @Published var imagingRecords: [Imaging_] = []

    @Published var isLoading: Bool = true

    /// Waits for every child store's initial state so an empty first store does
    /// not hide the loading indicator while another store is still querying.
    private var receivedStoreKeys = Set<String>()

    private var cancellables: [NativeCancellable] = []

    private let labStore: LabResultListStore
    private let imagingStore: ImagingListStore

    init(patientId: Int64) {
        labStore = IosReproAndDiagnosticsStores.shared.labResultListStore(patientId: patientId)
        imagingStore = IosReproAndDiagnosticsStores.shared.imagingListStore(patientId: patientId)

        cancellables.append(labStore.state.subscribe(onEach: { [weak self] state in
            Task { @MainActor in self?.labResults = state.records }
            self?.markFirstEmission("labResults")
        }))
        cancellables.append(imagingStore.state.subscribe(onEach: { [weak self] state in
            Task { @MainActor in self?.imagingRecords = state.records }
            self?.markFirstEmission("imaging")
        }))

        labStore.load()
        imagingStore.load()

    }

    /// Soft-deletes the record and reloads the list via the store.
    func deleteLabResult(_ recordId: Int64) {
        labStore.delete(recordId: recordId)
    }

    /// Soft-deletes the record and reloads the list via the store.
    func deleteImaging(_ recordId: Int64) {
        imagingStore.delete(recordId: recordId)
    }

    private func markFirstEmission(_ key: String) {
        receivedStoreKeys.insert(key)
        isLoading = receivedStoreKeys.count < 2
    }

    /// Reloads every store this tab owns; stores re-query Kotlin and republish.
    func reload() {
        labStore.load()
        imagingStore.load()
    }

    deinit {
        cancellables.forEach { $0.cancel() }
    }
}
