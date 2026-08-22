import SwiftUI
import Shared

struct DiagnosticsTabView: View {
    @StateObject private var viewModel: DiagnosticsTabViewModel
    /// Fires when a record row is tapped; args are the display type name and record id.
    var onEditRecord: ((String, Int64) -> Void)? = nil

    init(
        patientId: Int64,
        onEditRecord: ((String, Int64) -> Void)? = nil,
    ) {
        _viewModel = StateObject(wrappedValue: DiagnosticsTabViewModel(patientId: patientId))
        self.onEditRecord = onEditRecord
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
    }

    private var totalRecords: Int {
        viewModel.labResults.count + viewModel.imagingRecords.count
    }

    private var recordList: some View {
        List {
            // Lab Results
            RecordSection(title: "Lab Results", icon: "testtube.2", count: viewModel.labResults.count) {
                ForEach(viewModel.labResults, id: \.id) { record in
                    VStack(alignment: .leading, spacing: 6) {
                        RecordRowView(
                            icon: "testtube.2",
                            iconTint: Theme.forestGreen,
                            title: record.testType,
                            subtitle: record.vetName,
                            date: record.date.displayString
                        )
                        if let results = record.results, !results.isEmpty {
                            HStack(alignment: .top, spacing: 4) {
                                Image(systemName: "text.alignleft")
                                    .font(.caption2)
                                Text(results)
                                    .font(.caption2)
                                    .lineLimit(2)
                            }
                            .foregroundStyle(Theme.textSecondary)
                            .padding(.leading, 48)
                        }
                        if let normalRange = record.normalRange, !normalRange.isEmpty {
                            HStack(spacing: 4) {
                                Image(systemName: "range.right")
                                    .font(.caption2)
                                Text("Normal: \(normalRange)")
                                    .font(.caption2)
                            }
                            .foregroundStyle(Theme.textSecondary)
                            .padding(.leading, 48)
                        }
                    }
                    .contentShape(Rectangle())

                    .onTapGesture {

                        onEditRecord?("Lab Result", record.id)

                    }

                    .recordSwipeDelete(title: "Lab Result") {
                        viewModel.deleteLabResult(record.id)
                    }
                }
            }

            // Imaging
            RecordSection(title: "Imaging", icon: "photo.on.rectangle.angled", count: viewModel.imagingRecords.count) {
                ForEach(viewModel.imagingRecords, id: \.id) { record in
                    VStack(alignment: .leading, spacing: 6) {
                        RecordRowView(
                            icon: "photo.on.rectangle.angled",
                            iconTint: Theme.forestGreen,
                            title: record.type,
                            subtitle: record.vetName,
                            date: record.date.displayString
                        )
                        if let findings = record.findings, !findings.isEmpty {
                            HStack(alignment: .top, spacing: 4) {
                                Image(systemName: "text.alignleft")
                                    .font(.caption2)
                                Text(findings)
                                    .font(.caption2)
                                    .lineLimit(2)
                            }
                            .foregroundStyle(Theme.textSecondary)
                            .padding(.leading, 48)
                        }
                    }
                    .contentShape(Rectangle())

                    .onTapGesture {

                        onEditRecord?("Imaging", record.id)

                    }

                    .recordSwipeDelete(title: "Imaging Study") {
                        viewModel.deleteImaging(record.id)
                    }
                }
            }
        }
        .listStyle(.insetGrouped)
    }
}
