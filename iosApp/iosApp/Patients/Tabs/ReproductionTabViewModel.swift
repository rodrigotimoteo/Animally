import Foundation
import Shared

/// Manages the Reproduction tab: reproduction events, ultrasounds, gestations, repro medications.
@MainActor
final class ReproductionTabViewModel: ObservableObject {
    @Published var reproductionEvents: [ReproductionEvent] = []
    @Published var ultrasounds: [Ultrasound_] = []
    @Published var gestations: [Gestation_] = []
    @Published var reproMedications: [ReproMedication_] = []

    @Published var isLoading: Bool = true

    private var cancellables: [NativeCancellable] = []

    private let reproStore: ReproductionListStore
    private let ultrasoundStore: UltrasoundListStore
    private let gestationStore: GestationListStore
    private let reproMedStore: ReproMedicationListStore

    init(patientId: Int64) {
        reproStore = IosReproAndDiagnosticsStores.shared.reproductionListStore(patientId: patientId)
        ultrasoundStore = IosReproAndDiagnosticsStores.shared.ultrasoundListStore(patientId: patientId)
        gestationStore = IosReproAndDiagnosticsStores.shared.gestationListStore(patientId: patientId)
        reproMedStore = IosReproAndDiagnosticsStores.shared.reproMedicationListStore(patientId: patientId)

        cancellables.append(reproStore.state.subscribe(onEach: { [weak self] state in
            Task { @MainActor in self?.reproductionEvents = state.records }
        }))
        cancellables.append(ultrasoundStore.state.subscribe(onEach: { [weak self] state in
            Task { @MainActor in self?.ultrasounds = state.records }
        }))
        cancellables.append(gestationStore.state.subscribe(onEach: { [weak self] state in
            Task { @MainActor in self?.gestations = state.records }
        }))
        cancellables.append(reproMedStore.state.subscribe(onEach: { [weak self] state in
            Task { @MainActor in self?.reproMedications = state.records }
        }))

        reproStore.load()
        ultrasoundStore.load()
        gestationStore.load()
        reproMedStore.load()

        Task { @MainActor in
            try? await Task.sleep(for: .milliseconds(100))
            self.isLoading = false
        }
    }

    /// Soft-deletes the record and reloads the list via the store.
    func deleteReproductionEvent(_ recordId: Int64) {
        reproStore.delete(recordId: recordId)
    }

    /// Soft-deletes the record and reloads the list via the store.
    func deleteUltrasound(_ recordId: Int64) {
        ultrasoundStore.delete(recordId: recordId)
    }

    /// Soft-deletes the record and reloads the list via the store.
    func deleteGestation(_ recordId: Int64) {
        gestationStore.delete(recordId: recordId)
    }

    /// Soft-deletes the record and reloads the list via the store.
    func deleteReproMedication(_ recordId: Int64) {
        reproMedStore.delete(recordId: recordId)
    }

    deinit {
        cancellables.forEach { $0.cancel() }
    }
}
