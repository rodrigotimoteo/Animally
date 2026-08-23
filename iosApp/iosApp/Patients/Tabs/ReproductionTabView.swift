import SwiftUI
import Shared

struct ReproductionTabView: View {
    let patientId: Int64
    let refreshToken: Int
    @StateObject private var viewModel: ReproductionTabViewModel
    /// Fires when a record row is tapped; carries the display type, record id,
    /// and the field rows shown on the read-only detail screen.
    var onOpenRecord: ((String, Int64, [RecordDetailNav.FieldRow]) -> Void)? = nil

    init(
        patientId: Int64,
        refreshToken: Int = 0,
        onOpenRecord: ((String, Int64, [RecordDetailNav.FieldRow]) -> Void)? = nil,
    ) {
        self.patientId = patientId
        _viewModel = StateObject(wrappedValue: ReproductionTabViewModel(patientId: patientId))
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
                    icon: "heart.fill",
                    message: "No reproduction records yet"
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
        viewModel.reproductionEvents.count +
        viewModel.ultrasounds.count +
        viewModel.gestations.count +
        viewModel.reproMedications.count +
        viewModel.embryoTransfers.count +
        viewModel.icsiRecords.count
    }

    private var recordList: some View {
        List {
            // Reproduction Events
            recordSection(
                RecordSectionSpec(
                    title: "Events",
                    icon: "heart.fill",
                    items: viewModel.reproductionEvents,
                    recordId: { $0.id },
                    rowTitle: { $0.eventType },
                    rowSubtitle: { $0.details },
                    rowDate: { $0.date.displayString },
                    displayType: "Reproduction",
                    fields: { record in [
                        .init(label: "Date", value: record.date.displayString),
                        .init(label: "Event Type", value: record.eventType),
                        .init(label: "Details", value: record.details ?? ""),
                        .init(label: "Initial Exam Findings", value: record.initialExamFindings ?? ""),
                        .init(label: "Stallion", value: record.stallionName ?? ""),
                        .init(label: "Breeding Type", value: record.breedingType ?? ""),
                        .init(label: "Veterinarian", value: record.vetName ?? ""),
                        .init(label: "Notes", value: record.notes ?? ""),
                    ] },
                    onDelete: { viewModel.deleteReproductionEvent($0.id) },
                    deleteTitle: "Reproduction Event"
                ),
                onOpenRecord: onOpenRecord
            )

            // Ultrasounds
            RecordSection(title: "Ultrasounds", icon: "waveform.path.ecg", count: viewModel.ultrasounds.count) {
                ForEach(viewModel.ultrasounds, id: \.id) { record in
                    VStack(alignment: .leading, spacing: 6) {
                        RecordRowView(
                            icon: "waveform.path.ecg",
                            iconTint: Theme.forestGreen,
                            title: "Reproductive Ultrasound",
                            subtitle: record.ovaryStatus,
                            date: record.date.displayString
                        )
                        if let follicleSize = record.follicleSizeMm {
                            HStack(spacing: 4) {
                                Image(systemName: "ruler")
                                    .font(.caption2)
                                Text(String(format: "Follicle: %.1f mm", follicleSize.doubleValue))
                                    .font(.caption2)
                            }
                            .foregroundStyle(Theme.textSecondary)
                            .padding(.leading, 48)
                        }
                    }
                    .contentShape(Rectangle())

                    .onTapGesture {
                        let fs = record.follicleSizeMm?.doubleValue
                        let lfs = record.leftFollicleSizeMm?.doubleValue
                        let rfs = record.rightFollicleSizeMm?.doubleValue
                        var fields: [RecordDetailNav.FieldRow] = [
                            .init(label: "Date", value: record.date.displayString),
                            .init(label: "Ovary Status", value: record.ovaryStatus ?? ""),
                            .init(label: "Uterine Status", value: record.uterineStatus ?? ""),
                        ]
                        if let fs {
                            fields.append(.init(label: "Follicle Size (mm)", value: String(format: "%.1f", fs)))
                        }
                        fields.append(.init(label: "Left Ovary Status", value: record.leftOvaryStatus ?? ""))
                        fields.append(.init(label: "Right Ovary Status", value: record.rightOvaryStatus ?? ""))
                        if let lfs {
                            fields.append(.init(label: "Left Follicle Size (mm)", value: String(format: "%.1f", lfs)))
                        }
                        if let rfs {
                            fields.append(.init(label: "Right Follicle Size (mm)", value: String(format: "%.1f", rfs)))
                        }
                        fields.append(.init(label: "Uterine Edema", value: record.uterineEdema ?? ""))
                        fields.append(.init(label: "Fluid Description", value: record.uterineLiquidDescription ?? ""))
                        fields.append(.init(label: "Uterus Description", value: record.uterusDescription ?? ""))
                        fields.append(.init(label: "Findings", value: record.findings ?? ""))
                        fields.append(.init(label: "Veterinarian", value: record.vetName ?? ""))
                        fields.append(.init(label: "Notes", value: record.notes ?? ""))
                        onOpenRecord?("Ultrasound", record.id, fields.filter { !$0.value.isEmpty })
                    }

                    .recordSwipeDelete(title: "Ultrasound") {
                        viewModel.deleteUltrasound(record.id)
                    }
                }
            }

            // Gestations
            RecordSection(title: "Gestations", icon: "baby.fill", count: viewModel.gestations.count) {
                ForEach(viewModel.gestations, id: \.id) { record in
                    VStack(alignment: .leading, spacing: 6) {
                        RecordRowView(
                            icon: "baby.fill",
                            iconTint: Theme.forestGreen,
                            title: "Day \(record.gestationDays)",
                            subtitle: record.status,
                            date: record.breedingDate.displayString
                        )
                        HStack(spacing: 12) {
                            Label {
                                Text("Due: \(record.expectedDueDate.displayString)")
                            } icon: {
                                Image(systemName: "calendar.badge.clock")
                            }
                            .font(.caption2)
                            .foregroundStyle(Theme.textSecondary)

                            if let fetalCount = record.fetalCount {
                                Label {
                                    Text("\(fetalCount) fetus\(fetalCount.intValue > 1 ? "es" : "")")
                                } icon: {
                                    Image(systemName: "number")
                                }
                                .font(.caption2)
                                .foregroundStyle(Theme.textSecondary)
                            }
                        }
                        .padding(.leading, 48)
                    }
                    .contentShape(Rectangle())

                    .onTapGesture {
                        var fields: [RecordDetailNav.FieldRow] = [
                            .init(label: "Breeding Date", value: record.breedingDate.displayString),
                            .init(label: "Expected Due Date", value: record.expectedDueDate.displayString),
                            .init(label: "Gestation Day", value: "\(record.gestationDays)"),
                            .init(label: "Status", value: record.status ?? ""),
                        ]
                        if let fetalCount = record.fetalCount {
                            fields.append(.init(label: "Fetal Count", value: "\(fetalCount.intValue)"))
                        }
                        fields.append(.init(label: "Last Check Date", value: record.lastCheckDate?.displayString ?? ""))
                        fields.append(.init(label: "Notes", value: record.notes ?? ""))
                        onOpenRecord?("Gestation", record.id, fields.filter { !$0.value.isEmpty })
                    }

                    .recordSwipeDelete(title: "Gestation") {
                        viewModel.deleteGestation(record.id)
                    }
                }
            }

            // Repro Medications
            recordSection(
                RecordSectionSpec(
                    title: "Medications",
                    icon: "pills",
                    items: viewModel.reproMedications,
                    recordId: { $0.id },
                    rowTitle: { $0.medication },
                    rowSubtitle: { $0.purpose ?? $0.dosage },
                    rowDate: { $0.dateAdministered.displayString },
                    displayType: "Repro Medication",
                    fields: { record in [
                        .init(label: "Medication", value: record.medication),
                        .init(label: "Date Administered", value: record.dateAdministered.displayString),
                        .init(label: "Dosage", value: record.dosage ?? ""),
                        .init(label: "Purpose", value: record.purpose ?? ""),
                        .init(label: "Veterinarian", value: record.vetName ?? ""),
                        .init(label: "Notes", value: record.notes ?? ""),
                    ] },
                    onDelete: { viewModel.deleteReproMedication($0.id) }
                ),
                onOpenRecord: onOpenRecord
            )

            // Embryo Transfers
            recordSection(
                RecordSectionSpec(
                    title: "Embryo Transfers",
                    icon: "arrow.triangle.branch",
                    items: viewModel.embryoTransfers,
                    recordId: { $0.id },
                    rowTitle: { "\($0.embryoCount) embryo\($0.embryoCount == 1 ? "" : "s")" },
                    rowSubtitle: { $0.recipientMares },
                    rowDate: { $0.date.displayString },
                    displayType: "Embryo Transfer",
                    fields: { record in [
                        .init(label: "Date", value: record.date.displayString),
                        .init(label: "Embryo Count", value: "\(record.embryoCount)"),
                        .init(label: "Recipient Mares", value: record.recipientMares ?? ""),
                        .init(label: "Veterinarian", value: record.vetName ?? ""),
                        .init(label: "Notes", value: record.notes ?? ""),
                    ] },
                    onDelete: { viewModel.deleteEmbryoTransfer($0.id) }
                ),
                onOpenRecord: onOpenRecord
            )

            // ICSI
            recordSection(
                RecordSectionSpec(
                    title: "ICSI",
                    icon: "scope",
                    items: viewModel.icsiRecords,
                    recordId: { $0.id },
                    rowTitle: { "\($0.folliclesRecovered) follicle\($0.folliclesRecovered == 1 ? "" : "s") recovered" },
                    rowSubtitle: { $0.vetName },
                    rowDate: { $0.date.displayString },
                    displayType: "ICSI",
                    fields: { record in [
                        .init(label: "Date", value: record.date.displayString),
                        .init(label: "Follicles Recovered", value: "\(record.folliclesRecovered)"),
                        .init(label: "Veterinarian", value: record.vetName ?? ""),
                        .init(label: "Notes", value: record.notes ?? ""),
                    ] },
                    onDelete: { viewModel.deleteIcsi($0.id) }
                ),
                onOpenRecord: onOpenRecord
            )
        }
        .listStyle(.insetGrouped)
    }
}
