import Foundation
import Shared

/// Manages the Medical tab: clinical record stores for a patient.
/// Preventive-care records (vaccinations, dewormings, dentistry, farrier
/// visits) are owned by PreventiveTabViewModel.
@MainActor
final class MedicalTabViewModel: ObservableObject {
    @Published var consultations: [Consultation_] = []
    @Published var lamenessRecords: [Lameness_] = []
    @Published var surgeries: [Surgery_] = []
    @Published var medications: [Medication_] = []
    @Published var substances: [ControlledSubstance] = []
    @Published var weights: [Weight_] = []

    @Published var isLoading: Bool = true

    private var cancellables: [NativeCancellable] = []

    private let consultationStore: ConsultationListStore
    private let lamenessStore: LamenessListStore
    private let surgeryStore: SurgeryListStore
    private let medicationStore: MedicationListStore
    private let substanceStore: SubstanceListStore
    private let weightStore: WeightListStore

    init(patientId: Int64) {
        consultationStore = IosRecordStores.shared.consultationListStore(patientId: patientId)
        lamenessStore = IosRecordStores.shared.lamenessListStore(patientId: patientId)
        surgeryStore = IosRecordStores.shared.surgeryListStore(patientId: patientId)
        medicationStore = IosRecordStores.shared.medicationListStore(patientId: patientId)
        substanceStore = IosRecordStores.shared.substanceListStore(patientId: patientId)
        weightStore = IosRecordStores.shared.weightListStore(patientId: patientId)

        cancellables.append(consultationStore.state.subscribe(onEach: { [weak self] state in
            Task { @MainActor in self?.consultations = state.consultations }
        }))
        cancellables.append(lamenessStore.state.subscribe(onEach: { [weak self] state in
            Task { @MainActor in self?.lamenessRecords = state.records }
        }))
        cancellables.append(surgeryStore.state.subscribe(onEach: { [weak self] state in
            Task { @MainActor in self?.surgeries = state.records }
        }))
        cancellables.append(medicationStore.state.subscribe(onEach: { [weak self] state in
            Task { @MainActor in self?.medications = state.records }
        }))
        cancellables.append(substanceStore.state.subscribe(onEach: { [weak self] state in
            Task { @MainActor in self?.substances = state.records }
        }))
        cancellables.append(weightStore.state.subscribe(onEach: { [weak self] state in
            Task { @MainActor in self?.weights = state.records }
        }))

        // Load all stores
        consultationStore.load()
        lamenessStore.load()
        surgeryStore.load()
        medicationStore.load()
        substanceStore.load()
        weightStore.load()

        // Mark loading as done after a short delay to allow initial state to propagate
        Task { @MainActor in
            try? await Task.sleep(for: .milliseconds(100))
            self.isLoading = false
        }
    }

    /// Soft-deletes the record and reloads the list via the store.
    func deleteConsultation(_ recordId: Int64) {
        consultationStore.delete(recordId: recordId)
    }

    /// Soft-deletes the record and reloads the list via the store.
    func deleteLameness(_ recordId: Int64) {
        lamenessStore.delete(recordId: recordId)
    }

    /// Soft-deletes the record and reloads the list via the store.
    func deleteSurgery(_ recordId: Int64) {
        surgeryStore.delete(recordId: recordId)
    }

    /// Soft-deletes the record and reloads the list via the store.
    func deleteMedication(_ recordId: Int64) {
        medicationStore.delete(recordId: recordId)
    }

    /// Soft-deletes the record and reloads the list via the store.
    func deleteSubstance(_ recordId: Int64) {
        substanceStore.delete(recordId: recordId)
    }

    /// Soft-deletes the record and reloads the list via the store.
    func deleteWeight(_ recordId: Int64) {
        weightStore.delete(recordId: recordId)
    }

    deinit {
        cancellables.forEach { $0.cancel() }
    }
}
