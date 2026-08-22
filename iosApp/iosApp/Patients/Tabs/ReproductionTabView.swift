import SwiftUI
import Shared

struct ReproductionTabView: View {
    @StateObject private var viewModel: ReproductionTabViewModel

    init(patientId: Int64) {
        _viewModel = StateObject(wrappedValue: ReproductionTabViewModel(patientId: patientId))
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
        viewModel.reproMedications.count
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
                    .recordSwipeDelete(title: "Repro Medication") {
                        viewModel.deleteReproMedication(record.id)
                    }
                }
            }
        }
        .listStyle(.insetGrouped)
    }
}
