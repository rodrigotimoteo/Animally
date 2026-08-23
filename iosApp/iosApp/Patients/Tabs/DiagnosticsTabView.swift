import SwiftUI
import Shared

struct DiagnosticsTabView: View {
    let patientId: Int64
    let refreshToken: Int
    @StateObject private var viewModel: DiagnosticsTabViewModel
    /// Fires when a record row is tapped; carries the display type, record id,
    /// and the field rows shown on the read-only detail screen.
    var onOpenRecord: ((String, Int64, [RecordDetailNav.FieldRow]) -> Void)? = nil

    init(
        patientId: Int64,
        refreshToken: Int = 0,
        onOpenRecord: ((String, Int64, [RecordDetailNav.FieldRow]) -> Void)? = nil,
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

                        onOpenRecord?("Lab Result", record.id, [
                            .init(label: "Date", value: record.date.displayString),
                            .init(label: "Test Type", value: record.testType),
                            .init(label: "Results", value: record.results ?? ""),
                            .init(label: "Normal Range", value: record.normalRange ?? ""),
                            .init(label: "Veterinarian", value: record.vetName ?? ""),
                            .init(label: "Notes", value: record.notes ?? ""),
                        ].filter { !$0.value.isEmpty })

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

                        onOpenRecord?("Imaging", record.id, [
                            .init(label: "Date", value: record.date.displayString),
                            .init(label: "Type", value: record.type),
                            .init(label: "Findings", value: record.findings ?? ""),
                            .init(label: "Veterinarian", value: record.vetName ?? ""),
                            .init(label: "Notes", value: record.notes ?? ""),
                        ].filter { !$0.value.isEmpty })

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
