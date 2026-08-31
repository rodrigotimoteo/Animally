import Foundation
import Shared

/// Preventive tab: vaccination, deworming, dentistry, farrierVisit.
@MainActor
final class PreventiveTabViewModel: GenericRecordTabVM<PreventiveTabViewModel.SectionID> {
    @Published var vaccinations = RecordListState<Vaccination_>()
    @Published var dewormings = RecordListState<Deworming_>()
    @Published var dentistryRecords = RecordListState<Dentistry_>()
    @Published var farrierVisits = RecordListState<FarrierVisit_>()

    private let vaccinationStore: VaccinationListStore
    private let dewormingStore: DewormingListStore
    private let dentistryStore: DentistryListStore
    private let farrierStore: FarrierVisitListStore

    enum SectionID: Hashable {
        case vaccinations, dewormings, dentistry, farrier
    }

    init(patientId: Int64) {
        vaccinationStore = RecordListStores.vaccinationListStore(patientId: patientId)
        dewormingStore = RecordListStores.dewormingListStore(patientId: patientId)
        dentistryStore = RecordListStores.dentistryListStore(patientId: patientId)
        farrierStore = RecordListStores.farrierVisitListStore(patientId: patientId)
        super.init()

        sections = [
            .vaccinations: SectionBox(box: box(for: vaccinationStore), isExpanded: { [weak self] in self?.vaccinations.isExpanded ?? false }, display: { [weak self] in guard let self else { return Self.emptyDisplay }; return self.buildDisplay(for: .vaccinations, state: self.vaccinations) }),
            .dewormings: SectionBox(box: box(for: dewormingStore), isExpanded: { [weak self] in self?.dewormings.isExpanded ?? false }, display: { [weak self] in guard let self else { return Self.emptyDisplay }; return self.buildDisplay(for: .dewormings, state: self.dewormings) }),
            .dentistry: SectionBox(box: box(for: dentistryStore), isExpanded: { [weak self] in self?.dentistryRecords.isExpanded ?? false }, display: { [weak self] in guard let self else { return Self.emptyDisplay }; return self.buildDisplay(for: .dentistry, state: self.dentistryRecords) }),
            .farrier: SectionBox(box: box(for: farrierStore), isExpanded: { [weak self] in self?.farrierVisits.isExpanded ?? false }, display: { [weak self] in guard let self else { return Self.emptyDisplay }; return self.buildDisplay(for: .farrier, state: self.farrierVisits) }),
        ]

        bindWithExpansion(vaccinationStore.state, section: .vaccinations, isExpanded: { $0.displayState.isExpanded }) { [weak self] state in
            self?.vaccinations = Self.recordListState(allItems: state.vaccinations, visibleItems: state.visibleVaccinations, matchingCount: state.filteredVaccinations.count, searchQuery: state.displayState.searchQuery, isExpanded: state.displayState.isExpanded)
        }
        bindWithExpansion(dewormingStore.state, section: .dewormings, isExpanded: { $0.displayState.isExpanded }) { [weak self] state in
            self?.dewormings = Self.recordListState(allItems: state.records, visibleItems: state.visibleRecords, matchingCount: state.filteredRecords.count, searchQuery: state.displayState.searchQuery, isExpanded: state.displayState.isExpanded)
        }
        bindWithExpansion(dentistryStore.state, section: .dentistry, isExpanded: { $0.displayState.isExpanded }) { [weak self] state in
            self?.dentistryRecords = Self.recordListState(allItems: state.records, visibleItems: state.visibleRecords, matchingCount: state.filteredRecords.count, searchQuery: state.displayState.searchQuery, isExpanded: state.displayState.isExpanded)
        }
        bindWithExpansion(farrierStore.state, section: .farrier, isExpanded: { $0.displayState.isExpanded }) { [weak self] state in
            self?.farrierVisits = Self.recordListState(allItems: state.records, visibleItems: state.visibleRecords, matchingCount: state.filteredRecords.count, searchQuery: state.displayState.searchQuery, isExpanded: state.displayState.isExpanded)
        }

        reloadAll()
    }

    func deleteVaccination(_ recordId: Int64) { delete(recordId, for: .vaccinations) }
    func deleteDeworming(_ recordId: Int64) { delete(recordId, for: .dewormings) }
    func deleteDentistry(_ recordId: Int64) { delete(recordId, for: .dentistry) }
    func deleteFarrierVisit(_ recordId: Int64) { delete(recordId, for: .farrier) }

    func reload() { reloadAll() }
}
