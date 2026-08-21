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

    init(patientId: Int64) {
        let reproStore = IosReproAndDiagnosticsStores.shared.reproductionListStore(patientId: patientId)
        let ultrasoundStore = IosReproAndDiagnosticsStores.shared.ultrasoundListStore(patientId: patientId)
        let gestationStore = IosReproAndDiagnosticsStores.shared.gestationListStore(patientId: patientId)
        let reproMedStore = IosReproAndDiagnosticsStores.shared.reproMedicationListStore(patientId: patientId)

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

    deinit {
        cancellables.forEach { $0.cancel() }
    }
}
