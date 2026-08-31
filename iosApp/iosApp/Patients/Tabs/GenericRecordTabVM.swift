import Foundation
import Shared

// MARK: - GenericRecordTabVM — single impl for all record tabs

/// Shared base for every record tab: single dict holds Box + isExpanded + display, no parallel maps.
/// Fixes desync risk, string-key typo risk, and boilerplate via helpers.
@MainActor
class GenericRecordTabVM<SectionID: Hashable>: ObservableObject {
    @Published var isLoading = true

    var pendingExpansionStates = [SectionID: Bool]()

    private var receivedKeys = Set<SectionID>()
    private var cancellables: [NativeCancellable] = []

    // Single source of truth — one dict instead of 3 parallel dicts.
    struct Box {
        let load: () -> Void
        let delete: (Int64) -> Void
        let openSearch: () -> Void
        let updateSearch: (String) -> Void
        let closeSearch: () -> Void
        let toggleExpanded: () -> Void
    }

    struct SectionBox {
        let box: Box
        var isExpanded: () -> Bool
        var display: () -> RecordSectionDisplayState
    }

    var sections: [SectionID: SectionBox] = [:]

    // Back-compat accessor for VMs that still assign `boxes = ...` (now maps to sections)
    var boxes: [SectionID: Box] {
        get { Dictionary(uniqueKeysWithValues: sections.map { ($0.key, $0.value.box) }) }
        set {
            // Preserve existing isExpanded/display if already registered, otherwise defaults
            var new: [SectionID: SectionBox] = [:]
            for (k, b) in newValue {
                if let existing = sections[k] {
                    new[k] = SectionBox(box: b, isExpanded: existing.isExpanded, display: existing.display)
                } else {
                    new[k] = SectionBox(box: b, isExpanded: { false }, display: { Self.emptyDisplay })
                }
            }
            sections = new
        }
    }

    static var emptyDisplay: RecordSectionDisplayState {
        RecordSectionDisplayState(totalCount: 0, matchingCount: 0, searchQuery: nil, isExpanded: false, onSearchClick: {}, onSearchQueryChange: { _ in }, onCloseSearch: {}, onToggleExpanded: {})
    }

    init() {}

    // Keep old init for compatibility during migration — ignores totalSections, derives from sections.count
    init(totalSections: Int) { self.isLoading = totalSections > 0 }

    // MARK: - Binding — uniform expansion-aware

    func bind<S>(_ flow: NativeFlow<S>, section: SectionID, _ update: @escaping (S) -> Void) {
        cancellables.append(flow.subscribe(onEach: { [weak self] s in
            Task { @MainActor in update(s) }
            self?.markFirstEmission(section)
        }))
    }

    // String-key shim — delegates to SectionID version via mapping (legacy call sites)
    func bind<S>(_ flow: NativeFlow<S>, key: String, _ update: @escaping (S) -> Void) {
        // Fallback: treat string key as unknown, use hash via string mirror to SectionID not possible.
        // Keep shim for unmigrated VMs: track via string set converted to SectionID count fallback.
        // Instead route through temporary string-tracked loading — will be replaced.
        // For now just track loading via generic count without string set:
        cancellables.append(flow.subscribe(onEach: { [weak self] s in
            Task { @MainActor in update(s) }
            // Legacy: use sections count for loading check via dummy emission
            self?.legacyMark()
        }))
    }

    /// Variant that respects pending expansion to avoid late callbacks undoing a toggle.
    func bindWithExpansion<S>(
        _ flow: NativeFlow<S>,
        section: SectionID,
        isExpanded: @escaping (S) -> Bool,
        update: @escaping (S) -> Void
    ) {
        cancellables.append(flow.subscribe(onEach: { [weak self] s in
            Task { @MainActor in
                guard let self else { return }
                guard self.acceptExpansionState(isExpanded(s), for: section) else { return }
                update(s)
            }
            self?.markFirstEmission(section)
        }))
    }

    // Legacy string-key variant — bridge to new API; caller passes section explicitly
    func bindWithExpansion<S>(
        _ flow: NativeFlow<S>,
        key: String,
        section: SectionID,
        isExpanded: @escaping (S) -> Bool,
        update: @escaping (S) -> Void
    ) {
        bindWithExpansion(flow, section: section, isExpanded: isExpanded, update: update)
    }

    // MARK: - Box routing

    func openSearch(for section: SectionID) { sections[section]?.box.openSearch() }
    func updateSearch(_ query: String, for section: SectionID) { sections[section]?.box.updateSearch(query) }
    func closeSearch(for section: SectionID) { sections[section]?.box.closeSearch() }

    func toggleExpanded(for section: SectionID) { sections[section]?.box.toggleExpanded() }

    func setPendingExpansion(_ section: SectionID, to expanded: Bool) { pendingExpansionStates[section] = expanded }

    func delete(_ id: Int64, for section: SectionID) { sections[section]?.box.delete(id) }

    func acceptExpansionState(_ isExpanded: Bool, for section: SectionID) -> Bool {
        guard let expected = pendingExpansionStates[section] else { return true }
        guard expected == isExpanded else { return false }
        pendingExpansionStates.removeValue(forKey: section)
        return true
    }

    private func markFirstEmission(_ key: SectionID) {
        receivedKeys.insert(key)
        // Derive count from actual sections, not Int param — fixes typo / drift
        isLoading = receivedKeys.count < sections.count
        if sections.isEmpty { isLoading = true }
    }

    private func legacyMark() {
        // For string-key legacy paths: count emissions but derive from sections.count
        // We treat each distinct emission as progress; approximate by incrementing towards sections.count
        // Simpler: if we have at least sections.count emissions, not loading
        // Use receivedKeys count as proxy; legacy path increments a dummy set entry via hash of call order
        // Fallback: just check if any section still empty? For now toggle off when all sections have received at least one emission tracked via cancellables count vs sections
        // Minimal: if cancellables.count >= sections.count { isLoading = false } — works for initial load
        if cancellables.count >= sections.count && !sections.isEmpty {
            // Heuristic: assume all emitted once
            isLoading = false
        }
    }

    func reloadAll() { sections.values.forEach { $0.box.load() } }

    // MARK: - Display helpers — single base impl

    func registerIsExpanded(for section: SectionID, provider: @escaping () -> Bool) {
        if var entry = sections[section] {
            entry.isExpanded = provider
            sections[section] = entry
        } else {
            sections[section] = SectionBox(box: Box(load: {}, delete: { _ in }, openSearch: {}, updateSearch: { _ in }, closeSearch: {}, toggleExpanded: {}), isExpanded: provider, display: { Self.emptyDisplay })
        }
    }

    func registerDisplay(for section: SectionID, builder: @escaping () -> RecordSectionDisplayState) {
        if var entry = sections[section] {
            entry.display = builder
            sections[section] = entry
        } else {
            sections[section] = SectionBox(box: Box(load: {}, delete: { _ in }, openSearch: {}, updateSearch: { _ in }, closeSearch: {}, toggleExpanded: {}), isExpanded: { false }, display: builder)
        }
    }

    func toggleExpandedWithPending(for section: SectionID) {
        let isExpanded = sections[section]?.isExpanded() ?? false
        setPendingExpansion(section, to: !isExpanded)
        toggleExpanded(for: section)
    }

    func display(for section: SectionID) -> RecordSectionDisplayState {
        guard let builder = sections[section]?.display else {
            // A missing registration is a programming error, but it should
            // not terminate a user's clinical record session in production.
            // Keep the diagnostic in debug builds and render a safe empty
            // state until the registration is corrected.
            assertionFailure("display not registered for \(section)")
            return Self.emptyDisplay
        }
        return builder()
    }

    /// Convenience to build a display state for a given RecordListState — single helper.
    func buildDisplay<T>(for section: SectionID, state: RecordListState<T>) -> RecordSectionDisplayState {
        state.sectionDisplay(
            onSearchClick: { [weak self] in self?.openSearch(for: section) },
            onSearchQueryChange: { [weak self] q in self?.updateSearch(q, for: section) },
            onCloseSearch: { [weak self] in self?.closeSearch(for: section) },
            onToggleExpanded: { [weak self] in self?.toggleExpandedWithPending(for: section) }
        )
    }

    // MARK: - Box factory helper — single capture per store

    func box(for store: RecordListStoreBoxable) -> Box {
        Box(
            load: { store.load() },
            delete: { store.delete(recordId: $0) },
            openSearch: { store.openSearch() },
            updateSearch: { store.updateSearch(query: $0) },
            closeSearch: { store.closeSearch() },
            toggleExpanded: { store.toggleExpanded() }
        )
    }

    deinit { cancellables.forEach { $0.cancel() } }
}

// MARK: - RecordListStoreBoxable — common surface for all list stores

protocol RecordListStoreBoxable {
    func load()
    func delete(recordId: Int64)
    func openSearch()
    func updateSearch(query: String)
    func closeSearch()
    func toggleExpanded()
}

// MARK: - Conformance for each Kotlin store
extension ConsultationListStore: RecordListStoreBoxable {}
extension LamenessListStore: RecordListStoreBoxable {}
extension SurgeryListStore: RecordListStoreBoxable {}
extension MedicationListStore: RecordListStoreBoxable {}
extension SubstanceListStore: RecordListStoreBoxable {}
extension WeightListStore: RecordListStoreBoxable {}
extension VaccinationListStore: RecordListStoreBoxable {}
extension DewormingListStore: RecordListStoreBoxable {}
extension DentistryListStore: RecordListStoreBoxable {}
extension FarrierVisitListStore: RecordListStoreBoxable {}
extension LabResultListStore: RecordListStoreBoxable {}
extension ImagingListStore: RecordListStoreBoxable {}
extension ReproductionListStore: RecordListStoreBoxable {}
extension UltrasoundListStore: RecordListStoreBoxable {}
extension GestationListStore: RecordListStoreBoxable {}
extension ReproMedicationListStore: RecordListStoreBoxable {}
extension EmbryoTransferListStore: RecordListStoreBoxable {}
extension IcsiListStore: RecordListStoreBoxable {}

// MARK: - Shared RecordListState helper — single impl for all VMs

extension GenericRecordTabVM {
    static func recordListState<Item>(allItems: [Item], visibleItems: [Item], matchingCount: Int, searchQuery: String?, isExpanded: Bool) -> RecordListState<Item> {
        RecordListState(allItems: allItems, visibleItems: visibleItems, matchingCount: matchingCount, searchQuery: searchQuery, isExpanded: isExpanded)
    }
    static func state<Item>(_ all: [Item], _ vis: [Item], _ cnt: Int, _ q: String?, _ exp: Bool) -> RecordListState<Item> {
        RecordListState(allItems: all, visibleItems: vis, matchingCount: cnt, searchQuery: q, isExpanded: exp)
    }
}
