import Foundation
import Shared

/// Manages the Diagnostics tab: lab results and imaging.
@MainActor
final class DiagnosticsTabViewModel: ObservableObject {
    @Published var labResults = RecordListState<LabResult_>()
    @Published var imagingRecords = RecordListState<Imaging_>()

    @Published var isLoading: Bool = true

    /// Waits for every child store's initial state so an empty first store does
    /// not hide the loading indicator while another store is still querying.
    private var receivedStoreKeys = Set<String>()

    private var cancellables: [NativeCancellable] = []

    private let labStore: LabResultListStore
    private let imagingStore: ImagingListStore

    init(patientId: Int64) {
        labStore = IosReproAndDiagnosticsStores.shared.labResultListStore(patientId: patientId)
        imagingStore = IosReproAndDiagnosticsStores.shared.imagingListStore(patientId: patientId)

        cancellables.append(labStore.state.subscribe(onEach: { [weak self] state in
            Task { @MainActor in
                self?.labResults = Self.recordListState(
                    allItems: state.records,
                    visibleItems: state.visibleRecords,
                    matchingCount: state.filteredRecords.count,
                    searchQuery: state.displayState.searchQuery,
                    isExpanded: state.displayState.isExpanded
                )
            }
            self?.markFirstEmission("labResults")
        }))
        cancellables.append(imagingStore.state.subscribe(onEach: { [weak self] state in
            Task { @MainActor in
                self?.imagingRecords = Self.recordListState(
                    allItems: state.records,
                    visibleItems: state.visibleRecords,
                    matchingCount: state.filteredRecords.count,
                    searchQuery: state.displayState.searchQuery,
                    isExpanded: state.displayState.isExpanded
                )
            }
            self?.markFirstEmission("imaging")
        }))

        labStore.load()
        imagingStore.load()

    }

    /// Soft-deletes the record and reloads the list via the store.
    func deleteLabResult(_ recordId: Int64) {
        labStore.delete(recordId: recordId)
    }

    /// Soft-deletes the record and reloads the list via the store.
    func deleteImaging(_ recordId: Int64) {
        imagingStore.delete(recordId: recordId)
    }

    enum SectionID {
        case labResults, imaging
    }

    func openSearch(for section: SectionID) {
        switch section {
        case .labResults: labStore.openSearch()
        case .imaging: imagingStore.openSearch()
        }
    }

    func updateSearch(_ query: String, for section: SectionID) {
        switch section {
        case .labResults: labStore.updateSearch(query: query)
        case .imaging: imagingStore.updateSearch(query: query)
        }
    }

    func closeSearch(for section: SectionID) {
        switch section {
        case .labResults: labStore.closeSearch()
        case .imaging: imagingStore.closeSearch()
        }
    }

    func toggleExpanded(for section: SectionID) {
        switch section {
        case .labResults: labStore.toggleExpanded()
        case .imaging: imagingStore.toggleExpanded()
        }
    }

    func display(for section: SectionID) -> RecordSectionDisplayState {
        switch section {
        case .labResults:
            return labResults.sectionDisplay(
                onSearchClick: { [weak self] in self?.openSearch(for: .labResults) },
                onSearchQueryChange: { [weak self] query in self?.updateSearch(query, for: .labResults) },
                onCloseSearch: { [weak self] in self?.closeSearch(for: .labResults) },
                onToggleExpanded: { [weak self] in self?.toggleExpanded(for: .labResults) }
            )
        case .imaging:
            return imagingRecords.sectionDisplay(
                onSearchClick: { [weak self] in self?.openSearch(for: .imaging) },
                onSearchQueryChange: { [weak self] query in self?.updateSearch(query, for: .imaging) },
                onCloseSearch: { [weak self] in self?.closeSearch(for: .imaging) },
                onToggleExpanded: { [weak self] in self?.toggleExpanded(for: .imaging) }
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
        isLoading = receivedStoreKeys.count < 2
    }

    /// Reloads every store this tab owns; stores re-query Kotlin and republish.
    func reload() {
        labStore.load()
        imagingStore.load()
    }

    deinit {
        cancellables.forEach { $0.cancel() }
    }
}
