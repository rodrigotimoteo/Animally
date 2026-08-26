import Foundation
import Shared

/// Manages the Reproduction tab: reproduction events, ultrasounds, gestations, repro medications.
@MainActor
final class ReproductionTabViewModel: ObservableObject {
    @Published var reproductionEvents = RecordListState<ReproductionEvent>()
    @Published var ultrasounds = RecordListState<Ultrasound_>()
    @Published var gestations = RecordListState<Gestation_>()
    @Published var reproMedications = RecordListState<ReproMedication_>()
    @Published var embryoTransfers = RecordListState<EmbryoTransfer_>()
    @Published var icsiRecords = RecordListState<Icsi_>()

    @Published var isLoading: Bool = true

    /// Waits for every child store's initial state so an empty first store does
    /// not hide the loading indicator while another store is still querying.
    private var receivedStoreKeys = Set<String>()

    private var cancellables: [NativeCancellable] = []

    private let reproStore: ReproductionListStore
    private let ultrasoundStore: UltrasoundListStore
    private let gestationStore: GestationListStore
    private let reproMedStore: ReproMedicationListStore
    private let embryoTransferStore: EmbryoTransferListStore
    private let icsiStore: IcsiListStore

    init(patientId: Int64) {
        reproStore = IosReproAndDiagnosticsStores.shared.reproductionListStore(patientId: patientId)
        ultrasoundStore = IosReproAndDiagnosticsStores.shared.ultrasoundListStore(patientId: patientId)
        gestationStore = IosReproAndDiagnosticsStores.shared.gestationListStore(patientId: patientId)
        reproMedStore = IosReproAndDiagnosticsStores.shared.reproMedicationListStore(patientId: patientId)
        embryoTransferStore = IosRecordStores.shared.embryoTransferListStore(patientId: patientId)
        icsiStore = IosRecordStores.shared.icsiListStore(patientId: patientId)

        cancellables.append(reproStore.state.subscribe(onEach: { [weak self] state in
            Task { @MainActor in
                self?.reproductionEvents = Self.recordListState(
                    allItems: state.records,
                    visibleItems: state.visibleRecords,
                    matchingCount: state.filteredRecords.count,
                    searchQuery: state.displayState.searchQuery,
                    isExpanded: state.displayState.isExpanded
                )
            }
            self?.markFirstEmission("reproduction")
        }))
        cancellables.append(ultrasoundStore.state.subscribe(onEach: { [weak self] state in
            Task { @MainActor in
                self?.ultrasounds = Self.recordListState(
                    allItems: state.records,
                    visibleItems: state.visibleRecords,
                    matchingCount: state.filteredRecords.count,
                    searchQuery: state.displayState.searchQuery,
                    isExpanded: state.displayState.isExpanded
                )
            }
            self?.markFirstEmission("ultrasound")
        }))
        cancellables.append(gestationStore.state.subscribe(onEach: { [weak self] state in
            Task { @MainActor in
                self?.gestations = Self.recordListState(
                    allItems: state.records,
                    visibleItems: state.visibleRecords,
                    matchingCount: state.filteredRecords.count,
                    searchQuery: state.displayState.searchQuery,
                    isExpanded: state.displayState.isExpanded
                )
            }
            self?.markFirstEmission("gestation")
        }))
        cancellables.append(reproMedStore.state.subscribe(onEach: { [weak self] state in
            Task { @MainActor in
                self?.reproMedications = Self.recordListState(
                    allItems: state.records,
                    visibleItems: state.visibleRecords,
                    matchingCount: state.filteredRecords.count,
                    searchQuery: state.displayState.searchQuery,
                    isExpanded: state.displayState.isExpanded
                )
            }
            self?.markFirstEmission("reproMedications")
        }))
        cancellables.append(embryoTransferStore.state.subscribe(onEach: { [weak self] state in
            Task { @MainActor in
                self?.embryoTransfers = Self.recordListState(
                    allItems: state.records,
                    visibleItems: state.visibleRecords,
                    matchingCount: state.filteredRecords.count,
                    searchQuery: state.displayState.searchQuery,
                    isExpanded: state.displayState.isExpanded
                )
            }
            self?.markFirstEmission("embryoTransfers")
        }))
        cancellables.append(icsiStore.state.subscribe(onEach: { [weak self] state in
            Task { @MainActor in
                self?.icsiRecords = Self.recordListState(
                    allItems: state.records,
                    visibleItems: state.visibleRecords,
                    matchingCount: state.filteredRecords.count,
                    searchQuery: state.displayState.searchQuery,
                    isExpanded: state.displayState.isExpanded
                )
            }
            self?.markFirstEmission("icsi")
        }))

        reproStore.load()
        ultrasoundStore.load()
        gestationStore.load()
        reproMedStore.load()
        embryoTransferStore.load()
        icsiStore.load()

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

    /// Soft-deletes the record and reloads the list via the store.
    func deleteEmbryoTransfer(_ recordId: Int64) {
        embryoTransferStore.delete(recordId: recordId)
    }

    /// Soft-deletes the record and reloads the list via the store.
    func deleteIcsi(_ recordId: Int64) {
        icsiStore.delete(recordId: recordId)
    }

    enum SectionID {
        case reproductionEvents, ultrasounds, gestations, reproMedications, embryoTransfers, icsi
    }

    func openSearch(for section: SectionID) {
        switch section {
        case .reproductionEvents: reproStore.openSearch()
        case .ultrasounds: ultrasoundStore.openSearch()
        case .gestations: gestationStore.openSearch()
        case .reproMedications: reproMedStore.openSearch()
        case .embryoTransfers: embryoTransferStore.openSearch()
        case .icsi: icsiStore.openSearch()
        }
    }

    func updateSearch(_ query: String, for section: SectionID) {
        switch section {
        case .reproductionEvents: reproStore.updateSearch(query: query)
        case .ultrasounds: ultrasoundStore.updateSearch(query: query)
        case .gestations: gestationStore.updateSearch(query: query)
        case .reproMedications: reproMedStore.updateSearch(query: query)
        case .embryoTransfers: embryoTransferStore.updateSearch(query: query)
        case .icsi: icsiStore.updateSearch(query: query)
        }
    }

    func closeSearch(for section: SectionID) {
        switch section {
        case .reproductionEvents: reproStore.closeSearch()
        case .ultrasounds: ultrasoundStore.closeSearch()
        case .gestations: gestationStore.closeSearch()
        case .reproMedications: reproMedStore.closeSearch()
        case .embryoTransfers: embryoTransferStore.closeSearch()
        case .icsi: icsiStore.closeSearch()
        }
    }

    func toggleExpanded(for section: SectionID) {
        switch section {
        case .reproductionEvents: reproStore.toggleExpanded()
        case .ultrasounds: ultrasoundStore.toggleExpanded()
        case .gestations: gestationStore.toggleExpanded()
        case .reproMedications: reproMedStore.toggleExpanded()
        case .embryoTransfers: embryoTransferStore.toggleExpanded()
        case .icsi: icsiStore.toggleExpanded()
        }
    }

    func display(for section: SectionID) -> RecordSectionDisplayState {
        switch section {
        case .reproductionEvents:
            return reproductionEvents.sectionDisplay(
                onSearchClick: { [weak self] in self?.openSearch(for: .reproductionEvents) },
                onSearchQueryChange: { [weak self] query in self?.updateSearch(query, for: .reproductionEvents) },
                onCloseSearch: { [weak self] in self?.closeSearch(for: .reproductionEvents) },
                onToggleExpanded: { [weak self] in self?.toggleExpanded(for: .reproductionEvents) }
            )
        case .ultrasounds:
            return ultrasounds.sectionDisplay(
                onSearchClick: { [weak self] in self?.openSearch(for: .ultrasounds) },
                onSearchQueryChange: { [weak self] query in self?.updateSearch(query, for: .ultrasounds) },
                onCloseSearch: { [weak self] in self?.closeSearch(for: .ultrasounds) },
                onToggleExpanded: { [weak self] in self?.toggleExpanded(for: .ultrasounds) }
            )
        case .gestations:
            return gestations.sectionDisplay(
                onSearchClick: { [weak self] in self?.openSearch(for: .gestations) },
                onSearchQueryChange: { [weak self] query in self?.updateSearch(query, for: .gestations) },
                onCloseSearch: { [weak self] in self?.closeSearch(for: .gestations) },
                onToggleExpanded: { [weak self] in self?.toggleExpanded(for: .gestations) }
            )
        case .reproMedications:
            return reproMedications.sectionDisplay(
                onSearchClick: { [weak self] in self?.openSearch(for: .reproMedications) },
                onSearchQueryChange: { [weak self] query in self?.updateSearch(query, for: .reproMedications) },
                onCloseSearch: { [weak self] in self?.closeSearch(for: .reproMedications) },
                onToggleExpanded: { [weak self] in self?.toggleExpanded(for: .reproMedications) }
            )
        case .embryoTransfers:
            return embryoTransfers.sectionDisplay(
                onSearchClick: { [weak self] in self?.openSearch(for: .embryoTransfers) },
                onSearchQueryChange: { [weak self] query in self?.updateSearch(query, for: .embryoTransfers) },
                onCloseSearch: { [weak self] in self?.closeSearch(for: .embryoTransfers) },
                onToggleExpanded: { [weak self] in self?.toggleExpanded(for: .embryoTransfers) }
            )
        case .icsi:
            return icsiRecords.sectionDisplay(
                onSearchClick: { [weak self] in self?.openSearch(for: .icsi) },
                onSearchQueryChange: { [weak self] query in self?.updateSearch(query, for: .icsi) },
                onCloseSearch: { [weak self] in self?.closeSearch(for: .icsi) },
                onToggleExpanded: { [weak self] in self?.toggleExpanded(for: .icsi) }
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
        reproStore.load()
        ultrasoundStore.load()
        gestationStore.load()
        reproMedStore.load()
        embryoTransferStore.load()
        icsiStore.load()
    }

    deinit {
        cancellables.forEach { $0.cancel() }
    }
}
