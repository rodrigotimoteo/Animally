import Foundation
import Shared

/// Diagnostics tab: lab results and imaging — now via GenericRecordTabVM.
@MainActor
final class DiagnosticsTabViewModel: GenericRecordTabVM<DiagnosticsTabViewModel.SectionID> {
    @Published var labResults = RecordListState<LabResult_>()
    @Published var imagingRecords = RecordListState<Imaging_>()

    private let labStore: LabResultListStore
    private let imagingStore: ImagingListStore

    enum SectionID: Hashable {
        case labResults, imaging
    }

    init(patientId: Int64) {
        labStore = RecordListStores.labResultListStore(patientId: patientId)
        imagingStore = RecordListStores.imagingListStore(patientId: patientId)
        super.init()

        sections = [
            .labResults: SectionBox(box: box(for: labStore), isExpanded: { [weak self] in self?.labResults.isExpanded ?? false }, display: { [weak self] in guard let self else { return Self.emptyDisplay }; return self.buildDisplay(for: .labResults, state: self.labResults) }),
            .imaging: SectionBox(box: box(for: imagingStore), isExpanded: { [weak self] in self?.imagingRecords.isExpanded ?? false }, display: { [weak self] in guard let self else { return Self.emptyDisplay }; return self.buildDisplay(for: .imaging, state: self.imagingRecords) }),
        ]

        bindWithExpansion(labStore.state, section: .labResults, isExpanded: { $0.displayState.isExpanded }) { [weak self] state in
            self?.labResults = Self.recordListState(allItems: state.records, visibleItems: state.visibleRecords, matchingCount: state.filteredRecords.count, searchQuery: state.displayState.searchQuery, isExpanded: state.displayState.isExpanded)
        }
        bindWithExpansion(imagingStore.state, section: .imaging, isExpanded: { $0.displayState.isExpanded }) { [weak self] state in
            self?.imagingRecords = Self.recordListState(allItems: state.records, visibleItems: state.visibleRecords, matchingCount: state.filteredRecords.count, searchQuery: state.displayState.searchQuery, isExpanded: state.displayState.isExpanded)
        }

        reloadAll()
    }

    func deleteLabResult(_ recordId: Int64) { delete(recordId, for: .labResults) }
    func deleteImaging(_ recordId: Int64) { delete(recordId, for: .imaging) }

    func reload() { reloadAll() }
}
