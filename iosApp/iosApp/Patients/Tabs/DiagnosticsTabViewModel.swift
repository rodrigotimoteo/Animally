import Foundation
import Shared

/// Manages the Diagnostics tab: lab results and imaging.
@MainActor
final class DiagnosticsTabViewModel: ObservableObject {
    @Published var labResults: [LabResult_] = []
    @Published var imagingRecords: [Imaging_] = []

    @Published var isLoading: Bool = true

    /// Flips once the first store emission arrives; replaces the old fixed-delay hack.
    private var receivedFirstEmission = false

    private var cancellables: [NativeCancellable] = []

    private let labStore: LabResultListStore
    private let imagingStore: ImagingListStore

    init(patientId: Int64) {
        labStore = IosReproAndDiagnosticsStores.shared.labResultListStore(patientId: patientId)
        imagingStore = IosReproAndDiagnosticsStores.shared.imagingListStore(patientId: patientId)

        cancellables.append(labStore.state.subscribe(onEach: { [weak self] state in
            Task { @MainActor in self?.labResults = state.records }
            self?.markFirstEmission()
        }))
        cancellables.append(imagingStore.state.subscribe(onEach: { [weak self] state in
            Task { @MainActor in self?.imagingRecords = state.records }
            self?.markFirstEmission()
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

    private func markFirstEmission() {
        guard !receivedFirstEmission else { return }
        receivedFirstEmission = true
        isLoading = false
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
