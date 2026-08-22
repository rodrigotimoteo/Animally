import Foundation
import Shared

/// Manages the Diagnostics tab: lab results and imaging.
@MainActor
final class DiagnosticsTabViewModel: ObservableObject {
    @Published var labResults: [LabResult_] = []
    @Published var imagingRecords: [Imaging_] = []

    @Published var isLoading: Bool = true

    private var cancellables: [NativeCancellable] = []

    private let labStore: LabResultListStore
    private let imagingStore: ImagingListStore

    init(patientId: Int64) {
        labStore = IosReproAndDiagnosticsStores.shared.labResultListStore(patientId: patientId)
        imagingStore = IosReproAndDiagnosticsStores.shared.imagingListStore(patientId: patientId)

        cancellables.append(labStore.state.subscribe(onEach: { [weak self] state in
            Task { @MainActor in self?.labResults = state.records }
        }))
        cancellables.append(imagingStore.state.subscribe(onEach: { [weak self] state in
            Task { @MainActor in self?.imagingRecords = state.records }
        }))

        labStore.load()
        imagingStore.load()

        Task { @MainActor in
            try? await Task.sleep(for: .milliseconds(100))
            self.isLoading = false
        }
    }

    /// Soft-deletes the record and reloads the list via the store.
    func deleteLabResult(_ recordId: Int64) {
        labStore.delete(recordId: recordId)
    }

    /// Soft-deletes the record and reloads the list via the store.
    func deleteImaging(_ recordId: Int64) {
        imagingStore.delete(recordId: recordId)
    }

    deinit {
        cancellables.forEach { $0.cancel() }
    }
}
