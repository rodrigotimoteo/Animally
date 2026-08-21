import Foundation
import Shared

/// Manages the Medical tab: all 10 record stores for a patient.
@MainActor
final class MedicalTabViewModel: ObservableObject {
    @Published var consultations: [Consultation_] = []
    @Published var vaccinations: [Vaccination_] = []
    @Published var dewormings: [Deworming_] = []
    @Published var dentistryRecords: [Dentistry_] = []
    @Published var farrierVisits: [FarrierVisit_] = []
    @Published var lamenessRecords: [Lameness_] = []
    @Published var surgeries: [Surgery_] = []
    @Published var medications: [Medication_] = []
    @Published var substances: [ControlledSubstance] = []
    @Published var weights: [Weight_] = []

    @Published var isLoading: Bool = true

    private var cancellables: [NativeCancellable] = []

    init(patientId: Int64) {
        let consultationStore = IosRecordStores.shared.consultationListStore(patientId: patientId)
        let vaccinationStore = IosRecordStores.shared.vaccinationListStore(patientId: patientId)
        let dewormingStore = IosRecordStores.shared.dewormingListStore(patientId: patientId)
        let dentistryStore = IosRecordStores.shared.dentistryListStore(patientId: patientId)
        let farrierStore = IosRecordStores.shared.farrierVisitListStore(patientId: patientId)
        let lamenessStore = IosRecordStores.shared.lamenessListStore(patientId: patientId)
        let surgeryStore = IosRecordStores.shared.surgeryListStore(patientId: patientId)
        let medicationStore = IosRecordStores.shared.medicationListStore(patientId: patientId)
        let substanceStore = IosRecordStores.shared.substanceListStore(patientId: patientId)
        let weightStore = IosRecordStores.shared.weightListStore(patientId: patientId)

        cancellables.append(consultationStore.state.subscribe(onEach: { [weak self] state in
            Task { @MainActor in self?.consultations = state.consultations }
        }))
        cancellables.append(vaccinationStore.state.subscribe(onEach: { [weak self] state in
            Task { @MainActor in self?.vaccinations = state.vaccinations }
        }))
        cancellables.append(dewormingStore.state.subscribe(onEach: { [weak self] state in
            Task { @MainActor in self?.dewormings = state.records }
        }))
        cancellables.append(dentistryStore.state.subscribe(onEach: { [weak self] state in
            Task { @MainActor in self?.dentistryRecords = state.records }
        }))
        cancellables.append(farrierStore.state.subscribe(onEach: { [weak self] state in
            Task { @MainActor in self?.farrierVisits = state.records }
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
        vaccinationStore.load()
        dewormingStore.load()
        dentistryStore.load()
        farrierStore.load()
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

    deinit {
        cancellables.forEach { $0.cancel() }
    }
}
