import Foundation
import Shared

/// Manages the Preventive tab: vaccination, deworming, dentistry, farrierVisit.
@MainActor
final class PreventiveTabViewModel: ObservableObject {
    @Published var vaccinations = RecordListState<Vaccination_>()
    @Published var dewormings = RecordListState<Deworming_>()
    @Published var dentistryRecords = RecordListState<Dentistry_>()
    @Published var farrierVisits = RecordListState<FarrierVisit_>()

    @Published var isLoading: Bool = true

    /// Waits for every child store's initial state so an empty first store does
    /// not hide the loading indicator while another store is still querying.
    private var receivedStoreKeys = Set<String>()
    private var pendingExpansionStates = [SectionID: Bool]()

    private var cancellables: [NativeCancellable] = []

    private let vaccinationStore: VaccinationListStore
    private let dewormingStore: DewormingListStore
    private let dentistryStore: DentistryListStore
    private let farrierStore: FarrierVisitListStore

    init(patientId: Int64) {
        vaccinationStore = IosRecordStores.shared.vaccinationListStore(patientId: patientId)
        dewormingStore = IosRecordStores.shared.dewormingListStore(patientId: patientId)
        dentistryStore = IosRecordStores.shared.dentistryListStore(patientId: patientId)
        farrierStore = IosRecordStores.shared.farrierVisitListStore(patientId: patientId)

        cancellables.append(vaccinationStore.state.subscribe(onEach: { [weak self] state in
            Task { @MainActor in
                guard let self, self.acceptExpansionState(state.displayState.isExpanded, for: .vaccinations) else {
                    return
                }
                self.vaccinations = RecordListState(
                    allItems: state.vaccinations,
                    visibleItems: state.visibleVaccinations,
                    matchingCount: state.filteredVaccinations.count,
                    searchQuery: state.displayState.searchQuery,
                    isExpanded: state.displayState.isExpanded
                )
            }
            self?.markFirstEmission("vaccinations")
        }))
        cancellables.append(dewormingStore.state.subscribe(onEach: { [weak self] state in
            Task { @MainActor in
                guard let self, self.acceptExpansionState(state.displayState.isExpanded, for: .dewormings) else {
                    return
                }
                self.dewormings = Self.recordListState(
                    allItems: state.records,
                    visibleItems: state.visibleRecords,
                    matchingCount: state.filteredRecords.count,
                    searchQuery: state.displayState.searchQuery,
                    isExpanded: state.displayState.isExpanded
                )
            }
            self?.markFirstEmission("dewormings")
        }))
        cancellables.append(dentistryStore.state.subscribe(onEach: { [weak self] state in
            Task { @MainActor in
                guard let self, self.acceptExpansionState(state.displayState.isExpanded, for: .dentistry) else {
                    return
                }
                self.dentistryRecords = Self.recordListState(
                    allItems: state.records,
                    visibleItems: state.visibleRecords,
                    matchingCount: state.filteredRecords.count,
                    searchQuery: state.displayState.searchQuery,
                    isExpanded: state.displayState.isExpanded
                )
            }
            self?.markFirstEmission("dentistry")
        }))
        cancellables.append(farrierStore.state.subscribe(onEach: { [weak self] state in
            Task { @MainActor in
                guard let self, self.acceptExpansionState(state.displayState.isExpanded, for: .farrier) else {
                    return
                }
                self.farrierVisits = Self.recordListState(
                    allItems: state.records,
                    visibleItems: state.visibleRecords,
                    matchingCount: state.filteredRecords.count,
                    searchQuery: state.displayState.searchQuery,
                    isExpanded: state.displayState.isExpanded
                )
            }
            self?.markFirstEmission("farrier")
        }))

        vaccinationStore.load()
        dewormingStore.load()
        dentistryStore.load()
        farrierStore.load()

    }

    /// Soft-deletes the record and reloads the list via the store.
    func deleteVaccination(_ recordId: Int64) {
        vaccinationStore.delete(recordId: recordId)
    }

    /// Soft-deletes the record and reloads the list via the store.
    func deleteDeworming(_ recordId: Int64) {
        dewormingStore.delete(recordId: recordId)
    }

    /// Soft-deletes the record and reloads the list via the store.
    func deleteDentistry(_ recordId: Int64) {
        dentistryStore.delete(recordId: recordId)
    }

    /// Soft-deletes the record and reloads the list via the store.
    func deleteFarrierVisit(_ recordId: Int64) {
        farrierStore.delete(recordId: recordId)
    }

    enum SectionID: Hashable {
        case vaccinations, dewormings, dentistry, farrier
    }

    func openSearch(for section: SectionID) {
        switch section {
        case .vaccinations: vaccinationStore.openSearch()
        case .dewormings: dewormingStore.openSearch()
        case .dentistry: dentistryStore.openSearch()
        case .farrier: farrierStore.openSearch()
        }
    }

    func updateSearch(_ query: String, for section: SectionID) {
        switch section {
        case .vaccinations: vaccinationStore.updateSearch(query: query)
        case .dewormings: dewormingStore.updateSearch(query: query)
        case .dentistry: dentistryStore.updateSearch(query: query)
        case .farrier: farrierStore.updateSearch(query: query)
        }
    }

    func closeSearch(for section: SectionID) {
        switch section {
        case .vaccinations: vaccinationStore.closeSearch()
        case .dewormings: dewormingStore.closeSearch()
        case .dentistry: dentistryStore.closeSearch()
        case .farrier: farrierStore.closeSearch()
        }
    }

    func toggleExpanded(for section: SectionID) {
        switch section {
        case .vaccinations:
            pendingExpansionStates[section] = !vaccinations.isExpanded
            vaccinationStore.toggleExpanded()
            vaccinations = Self.recordListState(from: vaccinationStore.state.current)
        case .dewormings:
            pendingExpansionStates[section] = !dewormings.isExpanded
            dewormingStore.toggleExpanded()
            dewormings = Self.recordListState(from: dewormingStore.state.current)
        case .dentistry:
            pendingExpansionStates[section] = !dentistryRecords.isExpanded
            dentistryStore.toggleExpanded()
            dentistryRecords = Self.recordListState(from: dentistryStore.state.current)
        case .farrier:
            pendingExpansionStates[section] = !farrierVisits.isExpanded
            farrierStore.toggleExpanded()
            farrierVisits = Self.recordListState(from: farrierStore.state.current)
        }
    }

    func display(for section: SectionID) -> RecordSectionDisplayState {
        switch section {
        case .vaccinations:
            return vaccinations.sectionDisplay(
                onSearchClick: { [weak self] in self?.openSearch(for: .vaccinations) },
                onSearchQueryChange: { [weak self] query in self?.updateSearch(query, for: .vaccinations) },
                onCloseSearch: { [weak self] in self?.closeSearch(for: .vaccinations) },
                onToggleExpanded: { [weak self] in self?.toggleExpanded(for: .vaccinations) }
            )
        case .dewormings:
            return dewormings.sectionDisplay(
                onSearchClick: { [weak self] in self?.openSearch(for: .dewormings) },
                onSearchQueryChange: { [weak self] query in self?.updateSearch(query, for: .dewormings) },
                onCloseSearch: { [weak self] in self?.closeSearch(for: .dewormings) },
                onToggleExpanded: { [weak self] in self?.toggleExpanded(for: .dewormings) }
            )
        case .dentistry:
            return dentistryRecords.sectionDisplay(
                onSearchClick: { [weak self] in self?.openSearch(for: .dentistry) },
                onSearchQueryChange: { [weak self] query in self?.updateSearch(query, for: .dentistry) },
                onCloseSearch: { [weak self] in self?.closeSearch(for: .dentistry) },
                onToggleExpanded: { [weak self] in self?.toggleExpanded(for: .dentistry) }
            )
        case .farrier:
            return farrierVisits.sectionDisplay(
                onSearchClick: { [weak self] in self?.openSearch(for: .farrier) },
                onSearchQueryChange: { [weak self] query in self?.updateSearch(query, for: .farrier) },
                onCloseSearch: { [weak self] in self?.closeSearch(for: .farrier) },
                onToggleExpanded: { [weak self] in self?.toggleExpanded(for: .farrier) }
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

    /// Applies the latest Kotlin state synchronously after an action.
    ///
    /// StateFlow callbacks are delivered asynchronously across the Kotlin/
    /// Swift boundary. Keeping this immediate snapshot avoids a stale SwiftUI
    /// projection during an animated list update; Kotlin remains the source of
    /// truth for filtering, expansion, and the visible item projection.
    private static func recordListState(from state: VaccinationListUiState) -> RecordListState<Vaccination_> {
        recordListState(
            allItems: state.vaccinations,
            visibleItems: state.visibleVaccinations,
            matchingCount: state.filteredVaccinations.count,
            searchQuery: state.displayState.searchQuery,
            isExpanded: state.displayState.isExpanded
        )
    }

    private static func recordListState(from state: DewormingListUiState) -> RecordListState<Deworming_> {
        recordListState(
            allItems: state.records,
            visibleItems: state.visibleRecords,
            matchingCount: state.filteredRecords.count,
            searchQuery: state.displayState.searchQuery,
            isExpanded: state.displayState.isExpanded
        )
    }

    private static func recordListState(from state: DentistryListUiState) -> RecordListState<Dentistry_> {
        recordListState(
            allItems: state.records,
            visibleItems: state.visibleRecords,
            matchingCount: state.filteredRecords.count,
            searchQuery: state.displayState.searchQuery,
            isExpanded: state.displayState.isExpanded
        )
    }

    private static func recordListState(from state: FarrierVisitListUiState) -> RecordListState<FarrierVisit_> {
        recordListState(
            allItems: state.records,
            visibleItems: state.visibleRecords,
            matchingCount: state.filteredRecords.count,
            searchQuery: state.displayState.searchQuery,
            isExpanded: state.displayState.isExpanded
        )
    }

    /// Rejects an older callback that would undo a just-issued expansion
    /// action while the Kotlin state crosses the native boundary.
    private func acceptExpansionState(_ isExpanded: Bool, for section: SectionID) -> Bool {
        guard let expected = pendingExpansionStates[section] else { return true }
        guard expected == isExpanded else { return false }
        pendingExpansionStates.removeValue(forKey: section)
        return true
    }

    private func markFirstEmission(_ key: String) {
        receivedStoreKeys.insert(key)
        isLoading = receivedStoreKeys.count < 4
    }

    /// Reloads every store this tab owns; stores re-query Kotlin and republish.
    func reload() {
        vaccinationStore.load()
        dewormingStore.load()
        dentistryStore.load()
        farrierStore.load()
    }

    deinit {
        cancellables.forEach { $0.cancel() }
    }
}
