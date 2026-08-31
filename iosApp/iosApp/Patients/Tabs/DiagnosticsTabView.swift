import SwiftUI
import Shared

struct DiagnosticsTabView: View {
    let patientId: Int64
    let refreshToken: Int
    @StateObject private var viewModel: DiagnosticsTabViewModel
    /// Lazy open via RecordDetailKey.
    var onOpenRecord: ((String, Int64) -> Void)? = nil

    init(
        patientId: Int64,
        refreshToken: Int = 0,
        onOpenRecord: ((String, Int64) -> Void)? = nil,
    ) {
        self.patientId = patientId
        _viewModel = StateObject(wrappedValue: DiagnosticsTabViewModel(patientId: patientId))
        self.refreshToken = refreshToken
        self.onOpenRecord = onOpenRecord
    }

    var body: some View {
        Group {
            if viewModel.isLoading {
                ProgressView("Loading records…")
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else if totalRecords == 0 {
                TabEmptyStateView(
                    icon: "doc.text.fill",
                    message: "No diagnostics or files yet"
                )
            } else {
                recordList
            }
        }
        .onChange(of: refreshToken) { _, _ in
            viewModel.reload()
        }
    }

    private var totalRecords: Int {
        viewModel.labResults.totalCount + viewModel.imagingRecords.totalCount
    }

    private var recordList: some View {
        List {
            // Lab Results
            recordSection(
                RecordSectionSpec(
                    title: "Lab Results",
                    icon: "testtube.2",
                    items: viewModel.labResults.visibleItems,
                    recordId: { $0.id },
                    rowTitle: { $0.testType },
                    rowSubtitle: { $0.vetName },
                    rowDate: { $0.date.displayString },
                    displayType: "Lab Result",
                    onDelete: { viewModel.deleteLabResult($0.id) },
                    extraLine: { record in
                        [
                            record.results,
                            record.normalRange.map { "Normal: \($0)" },
                        ]
                        .compactMap { $0?.trimmingCharacters(in: .whitespacesAndNewlines) }
                        .filter { !$0.isEmpty }
                        .joined(separator: " · ")
                    },
                    extraLineLabel: nil,
                    display: viewModel.display(for: .labResults)
                ),
                onOpenRecord: onOpenRecord
            )

            // Imaging
            recordSection(
                RecordSectionSpec(
                    title: "Imaging",
                    icon: "photo.on.rectangle.angled",
                    items: viewModel.imagingRecords.visibleItems,
                    recordId: { $0.id },
                    rowTitle: { $0.type },
                    rowSubtitle: { $0.vetName },
                    rowDate: { $0.date.displayString },
                    displayType: "Imaging",
                    onDelete: { viewModel.deleteImaging($0.id) },
                    deleteTitle: "Imaging Study",
                    extraLine: { $0.findings },
                    extraLineLabel: nil,
                    display: viewModel.display(for: .imaging)
                ),
                onOpenRecord: onOpenRecord
            )
        }
        .listStyle(.insetGrouped)
    }
}
