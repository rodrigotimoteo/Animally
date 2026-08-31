import Foundation
import Shared

/// Manages the Medical tab: clinical record stores for a patient.
/// Uses GenericRecordTabVM base — single Box registry + uniform bindWithExpansion + base display.
@MainActor
final class MedicalTabViewModel: GenericRecordTabVM<MedicalTabViewModel.SectionID> {
    @Published var consultations = RecordListState<Consultation_>()
    @Published var lamenessRecords = RecordListState<Lameness_>()
    @Published var surgeries = RecordListState<Surgery_>()
    @Published var medications = RecordListState<Medication_>()
    @Published var substances = RecordListState<ControlledSubstance>()
    @Published var weights = RecordListState<Weight_>()

    private let consultationStore: ConsultationListStore
    private let lamenessStore: LamenessListStore
    private let surgeryStore: SurgeryListStore
    private let medicationStore: MedicationListStore
    private let substanceStore: SubstanceListStore
    private let weightStore: WeightListStore

    enum SectionID: Hashable {
        case consultations, lameness, surgeries, medications, substances, weights
    }

    init(patientId: Int64) {
        consultationStore = RecordListStores.consultationListStore(patientId: patientId)
        lamenessStore = RecordListStores.lamenessListStore(patientId: patientId)
        surgeryStore = RecordListStores.surgeryListStore(patientId: patientId)
        medicationStore = RecordListStores.medicationListStore(patientId: patientId)
        substanceStore = RecordListStores.substanceListStore(patientId: patientId)
        weightStore = RecordListStores.weightListStore(patientId: patientId)
        super.init()

        // MARK: Box registry — single helper, no 6× closure duplication
        sections = [
            .consultations: SectionBox(box: box(for: consultationStore), isExpanded: { [weak self] in self?.consultations.isExpanded ?? false }, display: { [weak self] in guard let self else { return Self.emptyDisplay }; return self.buildDisplay(for: .consultations, state: self.consultations) }),
            .lameness: SectionBox(box: box(for: lamenessStore), isExpanded: { [weak self] in self?.lamenessRecords.isExpanded ?? false }, display: { [weak self] in guard let self else { return Self.emptyDisplay }; return self.buildDisplay(for: .lameness, state: self.lamenessRecords) }),
            .surgeries: SectionBox(box: box(for: surgeryStore), isExpanded: { [weak self] in self?.surgeries.isExpanded ?? false }, display: { [weak self] in guard let self else { return Self.emptyDisplay }; return self.buildDisplay(for: .surgeries, state: self.surgeries) }),
            .medications: SectionBox(box: box(for: medicationStore), isExpanded: { [weak self] in self?.medications.isExpanded ?? false }, display: { [weak self] in guard let self else { return Self.emptyDisplay }; return self.buildDisplay(for: .medications, state: self.medications) }),
            .substances: SectionBox(box: box(for: substanceStore), isExpanded: { [weak self] in self?.substances.isExpanded ?? false }, display: { [weak self] in guard let self else { return Self.emptyDisplay }; return self.buildDisplay(for: .substances, state: self.substances) }),
            .weights: SectionBox(box: box(for: weightStore), isExpanded: { [weak self] in self?.weights.isExpanded ?? false }, display: { [weak self] in guard let self else { return Self.emptyDisplay }; return self.buildDisplay(for: .weights, state: self.weights) }),
        ]

        // MARK: Uniform expansion-aware bindings — SectionID keyed, no strings
        bindWithExpansion(consultationStore.state, section: .consultations, isExpanded: { $0.displayState.isExpanded }) { [weak self] state in
            self?.consultations = Self.recordListState(allItems: state.consultations, visibleItems: state.visibleConsultations, matchingCount: state.filteredConsultations.count, searchQuery: state.displayState.searchQuery, isExpanded: state.displayState.isExpanded)
        }
        bindWithExpansion(lamenessStore.state, section: .lameness, isExpanded: { $0.displayState.isExpanded }) { [weak self] state in
            self?.lamenessRecords = Self.recordListState(allItems: state.records, visibleItems: state.visibleRecords, matchingCount: state.filteredRecords.count, searchQuery: state.displayState.searchQuery, isExpanded: state.displayState.isExpanded)
        }
        bindWithExpansion(surgeryStore.state, section: .surgeries, isExpanded: { $0.displayState.isExpanded }) { [weak self] state in
            self?.surgeries = Self.recordListState(allItems: state.records, visibleItems: state.visibleRecords, matchingCount: state.filteredRecords.count, searchQuery: state.displayState.searchQuery, isExpanded: state.displayState.isExpanded)
        }
        bindWithExpansion(medicationStore.state, section: .medications, isExpanded: { $0.displayState.isExpanded }) { [weak self] state in
            self?.medications = Self.recordListState(allItems: state.records, visibleItems: state.visibleRecords, matchingCount: state.filteredRecords.count, searchQuery: state.displayState.searchQuery, isExpanded: state.displayState.isExpanded)
        }
        bindWithExpansion(substanceStore.state, section: .substances, isExpanded: { $0.displayState.isExpanded }) { [weak self] state in
            self?.substances = Self.recordListState(allItems: state.records, visibleItems: state.visibleRecords, matchingCount: state.filteredRecords.count, searchQuery: state.displayState.searchQuery, isExpanded: state.displayState.isExpanded)
        }
        bindWithExpansion(weightStore.state, section: .weights, isExpanded: { $0.displayState.isExpanded }) { [weak self] state in
            self?.weights = Self.recordListState(allItems: state.records, visibleItems: state.visibleRecords, matchingCount: state.filteredRecords.count, searchQuery: state.displayState.searchQuery, isExpanded: state.displayState.isExpanded)
        }

        reloadAll()
    }

    func deleteConsultation(_ recordId: Int64) { delete(recordId, for: .consultations) }
    func deleteLameness(_ recordId: Int64) { delete(recordId, for: .lameness) }
    func deleteSurgery(_ recordId: Int64) { delete(recordId, for: .surgeries) }
    func deleteMedication(_ recordId: Int64) { delete(recordId, for: .medications) }
    func deleteSubstance(_ recordId: Int64) { delete(recordId, for: .substances) }
    func deleteWeight(_ recordId: Int64) { delete(recordId, for: .weights) }

    func reload() { reloadAll() }
}
