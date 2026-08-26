import Foundation
import Shared

/// Manages the Medical tab: clinical record stores for a patient.
/// Preventive-care records (vaccinations, dewormings, dentistry, farrier
/// visits) are owned by PreventiveTabViewModel.
@MainActor
final class MedicalTabViewModel: ObservableObject {
    @Published var consultations = RecordListState<Consultation_>()
    @Published var lamenessRecords = RecordListState<Lameness_>()
    @Published var surgeries = RecordListState<Surgery_>()
    @Published var medications = RecordListState<Medication_>()
    @Published var substances = RecordListState<ControlledSubstance>()
    @Published var weights = RecordListState<Weight_>()

    @Published var isLoading: Bool = true

    /// Waits for every child store's initial state so an empty first store does
    /// not hide the loading indicator while another store is still querying.
    private var receivedStoreKeys = Set<String>()

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
            Task { @MainActor in
                self?.consultations = RecordListState(
                    allItems: state.consultations,
                    visibleItems: state.visibleConsultations,
                    matchingCount: state.filteredConsultations.count,
                    searchQuery: state.displayState.searchQuery,
                    isExpanded: state.displayState.isExpanded
                )
            }
            self?.markFirstEmission("consultations")
        }))
        cancellables.append(lamenessStore.state.subscribe(onEach: { [weak self] state in
            Task { @MainActor in
                self?.lamenessRecords = Self.recordListState(
                    allItems: state.records,
                    visibleItems: state.visibleRecords,
                    matchingCount: state.filteredRecords.count,
                    searchQuery: state.displayState.searchQuery,
                    isExpanded: state.displayState.isExpanded
                )
            }
            self?.markFirstEmission("lameness")
        }))
        cancellables.append(surgeryStore.state.subscribe(onEach: { [weak self] state in
            Task { @MainActor in
                self?.surgeries = Self.recordListState(
                    allItems: state.records,
                    visibleItems: state.visibleRecords,
                    matchingCount: state.filteredRecords.count,
                    searchQuery: state.displayState.searchQuery,
                    isExpanded: state.displayState.isExpanded
                )
            }
            self?.markFirstEmission("surgeries")
        }))
        cancellables.append(medicationStore.state.subscribe(onEach: { [weak self] state in
            Task { @MainActor in
                self?.medications = Self.recordListState(
                    allItems: state.records,
                    visibleItems: state.visibleRecords,
                    matchingCount: state.filteredRecords.count,
                    searchQuery: state.displayState.searchQuery,
                    isExpanded: state.displayState.isExpanded
                )
            }
            self?.markFirstEmission("medications")
        }))
        cancellables.append(substanceStore.state.subscribe(onEach: { [weak self] state in
            Task { @MainActor in
                self?.substances = Self.recordListState(
                    allItems: state.records,
                    visibleItems: state.visibleRecords,
                    matchingCount: state.filteredRecords.count,
                    searchQuery: state.displayState.searchQuery,
                    isExpanded: state.displayState.isExpanded
                )
            }
            self?.markFirstEmission("substances")
        }))
        cancellables.append(weightStore.state.subscribe(onEach: { [weak self] state in
            Task { @MainActor in
                self?.weights = Self.recordListState(
                    allItems: state.records,
                    visibleItems: state.visibleRecords,
                    matchingCount: state.filteredRecords.count,
                    searchQuery: state.displayState.searchQuery,
                    isExpanded: state.displayState.isExpanded
                )
            }
            self?.markFirstEmission("weights")
        }))

        // Load all stores
        consultationStore.load()
        lamenessStore.load()
        surgeryStore.load()
        medicationStore.load()
        substanceStore.load()
        weightStore.load()

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

    enum SectionID {
        case consultations, lameness, surgeries, medications, substances, weights
    }

    func openSearch(for section: SectionID) {
        switch section {
        case .consultations: consultationStore.openSearch()
        case .lameness: lamenessStore.openSearch()
        case .surgeries: surgeryStore.openSearch()
        case .medications: medicationStore.openSearch()
        case .substances: substanceStore.openSearch()
        case .weights: weightStore.openSearch()
        }
    }

    func updateSearch(_ query: String, for section: SectionID) {
        switch section {
        case .consultations: consultationStore.updateSearch(query: query)
        case .lameness: lamenessStore.updateSearch(query: query)
        case .surgeries: surgeryStore.updateSearch(query: query)
        case .medications: medicationStore.updateSearch(query: query)
        case .substances: substanceStore.updateSearch(query: query)
        case .weights: weightStore.updateSearch(query: query)
        }
    }

    func closeSearch(for section: SectionID) {
        switch section {
        case .consultations: consultationStore.closeSearch()
        case .lameness: lamenessStore.closeSearch()
        case .surgeries: surgeryStore.closeSearch()
        case .medications: medicationStore.closeSearch()
        case .substances: substanceStore.closeSearch()
        case .weights: weightStore.closeSearch()
        }
    }

    func toggleExpanded(for section: SectionID) {
        switch section {
        case .consultations: consultationStore.toggleExpanded()
        case .lameness: lamenessStore.toggleExpanded()
        case .surgeries: surgeryStore.toggleExpanded()
        case .medications: medicationStore.toggleExpanded()
        case .substances: substanceStore.toggleExpanded()
        case .weights: weightStore.toggleExpanded()
        }
    }

    func display(for section: SectionID) -> RecordSectionDisplayState {
        switch section {
        case .consultations:
            return consultations.sectionDisplay(
                onSearchClick: { [weak self] in self?.openSearch(for: .consultations) },
                onSearchQueryChange: { [weak self] query in self?.updateSearch(query, for: .consultations) },
                onCloseSearch: { [weak self] in self?.closeSearch(for: .consultations) },
                onToggleExpanded: { [weak self] in self?.toggleExpanded(for: .consultations) }
            )
        case .lameness:
            return lamenessRecords.sectionDisplay(
                onSearchClick: { [weak self] in self?.openSearch(for: .lameness) },
                onSearchQueryChange: { [weak self] query in self?.updateSearch(query, for: .lameness) },
                onCloseSearch: { [weak self] in self?.closeSearch(for: .lameness) },
                onToggleExpanded: { [weak self] in self?.toggleExpanded(for: .lameness) }
            )
        case .surgeries:
            return surgeries.sectionDisplay(
                onSearchClick: { [weak self] in self?.openSearch(for: .surgeries) },
                onSearchQueryChange: { [weak self] query in self?.updateSearch(query, for: .surgeries) },
                onCloseSearch: { [weak self] in self?.closeSearch(for: .surgeries) },
                onToggleExpanded: { [weak self] in self?.toggleExpanded(for: .surgeries) }
            )
        case .medications:
            return medications.sectionDisplay(
                onSearchClick: { [weak self] in self?.openSearch(for: .medications) },
                onSearchQueryChange: { [weak self] query in self?.updateSearch(query, for: .medications) },
                onCloseSearch: { [weak self] in self?.closeSearch(for: .medications) },
                onToggleExpanded: { [weak self] in self?.toggleExpanded(for: .medications) }
            )
        case .substances:
            return substances.sectionDisplay(
                onSearchClick: { [weak self] in self?.openSearch(for: .substances) },
                onSearchQueryChange: { [weak self] query in self?.updateSearch(query, for: .substances) },
                onCloseSearch: { [weak self] in self?.closeSearch(for: .substances) },
                onToggleExpanded: { [weak self] in self?.toggleExpanded(for: .substances) }
            )
        case .weights:
            return weights.sectionDisplay(
                onSearchClick: { [weak self] in self?.openSearch(for: .weights) },
                onSearchQueryChange: { [weak self] query in self?.updateSearch(query, for: .weights) },
                onCloseSearch: { [weak self] in self?.closeSearch(for: .weights) },
                onToggleExpanded: { [weak self] in self?.toggleExpanded(for: .weights) }
            )
        }
    }

    private static func recordListState<Item>(
        allItems: [Item],
        visibleItems: [Item],
        matchingCount: Int,
        searchQuery: String?,
        isExpanded: Bool
    ) -> RecordListState<Item> {
        RecordListState(
            allItems: allItems,
            visibleItems: visibleItems,
            matchingCount: matchingCount,
            searchQuery: searchQuery,
            isExpanded: isExpanded
        )
    }

    private func markFirstEmission(_ key: String) {
        receivedStoreKeys.insert(key)
        isLoading = receivedStoreKeys.count < 6
    }

    /// Reloads every store this tab owns; stores re-query Kotlin and republish.
    func reload() {
        consultationStore.load()
        lamenessStore.load()
        surgeryStore.load()
        medicationStore.load()
        substanceStore.load()
        weightStore.load()
    }

    deinit {
        cancellables.forEach { $0.cancel() }
    }
}
