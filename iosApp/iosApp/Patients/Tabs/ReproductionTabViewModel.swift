import Foundation
import Shared

@MainActor
final class ReproductionTabViewModel: GenericRecordTabVM<ReproductionTabViewModel.SectionID> {
    @Published var reproductionEvents = RecordListState<ReproductionEvent>()
    @Published var ultrasounds = RecordListState<Ultrasound_>()
    @Published var gestations = RecordListState<Gestation_>()
    @Published var reproMedications = RecordListState<ReproMedication_>()
    @Published var embryoTransfers = RecordListState<EmbryoTransfer_>()
    @Published var icsiRecords = RecordListState<Icsi_>()

    private var todayKotlin = DateFormatters.todayLocalDate()

    private let reproStore: ReproductionListStore
    private let ultrasoundStore: UltrasoundListStore
    private let gestationStore: GestationListStore
    private let reproMedStore: ReproMedicationListStore
    private let embryoTransferStore: EmbryoTransferListStore
    private let icsiStore: IcsiListStore

    enum SectionID: Hashable { case reproductionEvents, ultrasounds, gestations, reproMedications, embryoTransfers, icsi }

    init(patientId: Int64) {
        todayKotlin = DateFormatters.todayLocalDate()
        reproStore = RecordListStores.reproductionListStore(patientId: patientId)
        ultrasoundStore = RecordListStores.ultrasoundListStore(patientId: patientId)
        gestationStore = RecordListStores.gestationListStore(patientId: patientId)
        reproMedStore = RecordListStores.reproMedicationListStore(patientId: patientId)
        embryoTransferStore = RecordListStores.embryoTransferListStore(patientId: patientId)
        icsiStore = RecordListStores.icsiListStore(patientId: patientId)
        super.init()

        sections = [
            .reproductionEvents: SectionBox(box: box(for: reproStore), isExpanded: { [weak self] in self?.reproductionEvents.isExpanded ?? false }, display: { [weak self] in guard let self else { return Self.emptyDisplay }; return self.buildDisplay(for: .reproductionEvents, state: self.reproductionEvents) }),
            .ultrasounds: SectionBox(box: box(for: ultrasoundStore), isExpanded: { [weak self] in self?.ultrasounds.isExpanded ?? false }, display: { [weak self] in guard let self else { return Self.emptyDisplay }; return self.buildDisplay(for: .ultrasounds, state: self.ultrasounds) }),
            .gestations: SectionBox(box: box(for: gestationStore), isExpanded: { [weak self] in self?.gestations.isExpanded ?? false }, display: { [weak self] in guard let self else { return Self.emptyDisplay }; return self.buildDisplay(for: .gestations, state: self.gestations) }),
            .reproMedications: SectionBox(box: box(for: reproMedStore), isExpanded: { [weak self] in self?.reproMedications.isExpanded ?? false }, display: { [weak self] in guard let self else { return Self.emptyDisplay }; return self.buildDisplay(for: .reproMedications, state: self.reproMedications) }),
            .embryoTransfers: SectionBox(box: box(for: embryoTransferStore), isExpanded: { [weak self] in self?.embryoTransfers.isExpanded ?? false }, display: { [weak self] in guard let self else { return Self.emptyDisplay }; return self.buildDisplay(for: .embryoTransfers, state: self.embryoTransfers) }),
            .icsi: SectionBox(box: box(for: icsiStore), isExpanded: { [weak self] in self?.icsiRecords.isExpanded ?? false }, display: { [weak self] in guard let self else { return Self.emptyDisplay }; return self.buildDisplay(for: .icsi, state: self.icsiRecords) }),
        ]

        bindWithExpansion(reproStore.state, section: .reproductionEvents, isExpanded: { $0.displayState.isExpanded }) { [weak self] s in self?.reproductionEvents = Self.recordListState(allItems: s.records, visibleItems: s.visibleRecords, matchingCount: s.filteredRecords.count, searchQuery: s.displayState.searchQuery, isExpanded: s.displayState.isExpanded) }
        bindWithExpansion(ultrasoundStore.state, section: .ultrasounds, isExpanded: { $0.displayState.isExpanded }) { [weak self] s in self?.ultrasounds = Self.recordListState(allItems: s.records, visibleItems: s.visibleRecords, matchingCount: s.filteredRecords.count, searchQuery: s.displayState.searchQuery, isExpanded: s.displayState.isExpanded) }
        bindWithExpansion(gestationStore.state, section: .gestations, isExpanded: { $0.displayState.isExpanded }) { [weak self] s in self?.gestations = Self.recordListState(allItems: s.records, visibleItems: s.visibleRecords, matchingCount: s.filteredRecords.count, searchQuery: s.displayState.searchQuery, isExpanded: s.displayState.isExpanded) }
        bindWithExpansion(reproMedStore.state, section: .reproMedications, isExpanded: { $0.displayState.isExpanded }) { [weak self] s in self?.reproMedications = Self.recordListState(allItems: s.records, visibleItems: s.visibleRecords, matchingCount: s.filteredRecords.count, searchQuery: s.displayState.searchQuery, isExpanded: s.displayState.isExpanded) }
        bindWithExpansion(embryoTransferStore.state, section: .embryoTransfers, isExpanded: { $0.displayState.isExpanded }) { [weak self] s in self?.embryoTransfers = Self.recordListState(allItems: s.records, visibleItems: s.visibleRecords, matchingCount: s.filteredRecords.count, searchQuery: s.displayState.searchQuery, isExpanded: s.displayState.isExpanded) }
        bindWithExpansion(icsiStore.state, section: .icsi, isExpanded: { $0.displayState.isExpanded }) { [weak self] s in self?.icsiRecords = Self.recordListState(allItems: s.records, visibleItems: s.visibleRecords, matchingCount: s.filteredRecords.count, searchQuery: s.displayState.searchQuery, isExpanded: s.displayState.isExpanded) }

        reloadAll()
    }

    func deleteReproductionEvent(_ id: Int64) { delete(id, for: .reproductionEvents) }
    func deleteUltrasound(_ id: Int64) { delete(id, for: .ultrasounds) }
    func deleteGestation(_ id: Int64) { delete(id, for: .gestations) }
    func deleteReproMedication(_ id: Int64) { delete(id, for: .reproMedications) }
    func deleteEmbryoTransfer(_ id: Int64) { delete(id, for: .embryoTransfers) }
    func deleteIcsi(_ id: Int64) { delete(id, for: .icsi) }

    func gestationDay(for g: Gestation_) -> Int { GestationCalculator.gestationDay(for: g, today: todayKotlin) }
    func daysUntilDue(for g: Gestation_) -> Int { GestationCalculator.daysUntilDue(for: g, today: todayKotlin) }
    func reload() { todayKotlin = DateFormatters.todayLocalDate(); reloadAll() }
}
