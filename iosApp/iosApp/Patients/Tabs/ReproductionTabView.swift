import SwiftUI
import Shared

struct ReproductionTabView: View {
    @StateObject private var viewModel: ReproductionTabViewModel
    /// Fires when a record row is tapped; carries the display type, record id,
    /// and the field rows shown on the read-only detail screen.
    var onOpenRecord: ((String, Int64, [RecordDetailNav.FieldRow]) -> Void)? = nil

    init(
        patientId: Int64,
        onOpenRecord: ((String, Int64, [RecordDetailNav.FieldRow]) -> Void)? = nil,
    ) {
        _viewModel = StateObject(wrappedValue: ReproductionTabViewModel(patientId: patientId))
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
            RecordSection(title: "Events", icon: "heart.fill", count: viewModel.reproductionEvents.count) {
                ForEach(viewModel.reproductionEvents, id: \.id) { record in
                    RecordRowView(
                        icon: "heart.fill",
                        iconTint: Theme.forestGreen,
                        title: record.eventType,
                        subtitle: record.details,
                        date: record.date.displayString
                    )
                    .contentShape(Rectangle())

                    .onTapGesture {

                        onOpenRecord?("Reproduction", record.id, [
                            .init(label: "Date", value: record.date.displayString),
                            .init(label: "Event Type", value: record.eventType),
                            .init(label: "Details", value: record.details ?? ""),
                            .init(label: "Initial Exam Findings", value: record.initialExamFindings ?? ""),
                            .init(label: "Stallion", value: record.stallionName ?? ""),
                            .init(label: "Breeding Type", value: record.breedingType ?? ""),
                            .init(label: "Veterinarian", value: record.vetName ?? ""),
                            .init(label: "Notes", value: record.notes ?? ""),
                        ].filter { !$0.value.isEmpty })

                    }

                    .recordSwipeDelete(title: "Reproduction Event") {
                        viewModel.deleteReproductionEvent(record.id)
                    }
                }
            }

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
            RecordSection(title: "Medications", icon: "pills", count: viewModel.reproMedications.count) {
                ForEach(viewModel.reproMedications, id: \.id) { record in
                    RecordRowView(
                        icon: "pills",
                        iconTint: Theme.forestGreen,
                        title: record.medication,
                        subtitle: record.purpose ?? record.dosage,
                        date: record.dateAdministered.displayString
                    )
                    .contentShape(Rectangle())

                    .onTapGesture {

                        onOpenRecord?("Repro Medication", record.id, [
                            .init(label: "Medication", value: record.medication),
                            .init(label: "Date Administered", value: record.dateAdministered.displayString),
                            .init(label: "Dosage", value: record.dosage ?? ""),
                            .init(label: "Purpose", value: record.purpose ?? ""),
                            .init(label: "Veterinarian", value: record.vetName ?? ""),
                            .init(label: "Notes", value: record.notes ?? ""),
                        ].filter { !$0.value.isEmpty })

                    }

                    .recordSwipeDelete(title: "Repro Medication") {
                        viewModel.deleteReproMedication(record.id)
                    }
                }
            }

            // Embryo Transfers
            RecordSection(title: "Embryo Transfers", icon: "arrow.triangle.branch", count: viewModel.embryoTransfers.count) {
                ForEach(viewModel.embryoTransfers, id: \.id) { record in
                    RecordRowView(
                        icon: "arrow.triangle.branch",
                        iconTint: Theme.forestGreen,
                        title: "\(record.embryoCount) embryo\(record.embryoCount == 1 ? "" : "s")",
                        subtitle: record.recipientMares,
                        date: record.date.displayString
                    )
                    .contentShape(Rectangle())
                    .onTapGesture {
                        onOpenRecord?("Embryo Transfer", record.id, [
                            .init(label: "Date", value: record.date.displayString),
                            .init(label: "Embryo Count", value: "\(record.embryoCount)"),
                            .init(label: "Recipient Mares", value: record.recipientMares ?? ""),
                            .init(label: "Veterinarian", value: record.vetName ?? ""),
                            .init(label: "Notes", value: record.notes ?? ""),
                        ].filter { !$0.value.isEmpty })
                    }
                    .recordSwipeDelete(title: "Embryo Transfer") {
                        viewModel.deleteEmbryoTransfer(record.id)
                    }
                }
            }

            // ICSI
            RecordSection(title: "ICSI", icon: "scope", count: viewModel.icsiRecords.count) {
                ForEach(viewModel.icsiRecords, id: \.id) { record in
                    RecordRowView(
                        icon: "scope",
                        iconTint: Theme.forestGreen,
                        title: "\(record.folliclesRecovered) follicle\(record.folliclesRecovered == 1 ? "" : "s") recovered",
                        subtitle: record.vetName,
                        date: record.date.displayString
                    )
                    .contentShape(Rectangle())
                    .onTapGesture {
                        onOpenRecord?("ICSI", record.id, [
                            .init(label: "Date", value: record.date.displayString),
                            .init(label: "Follicles Recovered", value: "\(record.folliclesRecovered)"),
                            .init(label: "Veterinarian", value: record.vetName ?? ""),
                            .init(label: "Notes", value: record.notes ?? ""),
                        ].filter { !$0.value.isEmpty })
                    }
                    .recordSwipeDelete(title: "ICSI") {
                        viewModel.deleteIcsi(record.id)
                    }
                }
            }
        }
        .listStyle(.insetGrouped)
    }
}
