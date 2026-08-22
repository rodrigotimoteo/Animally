import SwiftUI
import Shared

struct MedicalTabView: View {
    @StateObject private var viewModel: MedicalTabViewModel

    init(patientId: Int64) {
        _viewModel = StateObject(wrappedValue: MedicalTabViewModel(patientId: patientId))
    }

    var body: some View {
        Group {
            if viewModel.isLoading {
                ProgressView("Loading records…")
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else if totalRecords == 0 {
                TabEmptyStateView(
                    icon: "cross.case.fill",
                    message: "No medical records yet"
                )
            } else {
                recordList
            }
        }
    }

    private var totalRecords: Int {
        viewModel.consultations.count +
        viewModel.lamenessRecords.count +
        viewModel.surgeries.count +
        viewModel.medications.count +
        viewModel.substances.count +
        viewModel.weights.count
    }

    private var recordList: some View {
        List {
            // Consultations
            RecordSection(title: "Consultations", icon: "stethoscope", count: viewModel.consultations.count) {
                ForEach(viewModel.consultations, id: \.id) { record in
                    RecordRowView(
                        icon: "stethoscope",
                        iconTint: Theme.forestGreen,
                        title: record.assessment.isEmpty ? "Consultation" : record.assessment,
                        subtitle: record.vetName,
                        date: record.date.displayString
                    )
                    .recordSwipeDelete(title: "Consultation") {
                        viewModel.deleteConsultation(record.id)
                    }
                }
            }

            // Preventive-care records (vaccinations, dewormings, dentistry,
            // farrier visits) live in the Preventive tab only.

            // Lameness
            RecordSection(title: "Lameness Evaluations", icon: "figure.run", count: viewModel.lamenessRecords.count) {
                ForEach(viewModel.lamenessRecords, id: \.id) { record in
                    RecordRowView(
                        icon: "figure.run",
                        iconTint: Theme.forestGreen,
                        title: "AAEP Grade \(record.gradeAAEP)",
                        subtitle: record.limbLocation,
                        date: record.date.displayString
                    )
                    .recordSwipeDelete(title: "Lameness Evaluation") {
                        viewModel.deleteLameness(record.id)
                    }
                }
            }

            // Surgeries
            RecordSection(title: "Surgeries", icon: "scissors", count: viewModel.surgeries.count) {
                ForEach(viewModel.surgeries, id: \.id) { record in
                    RecordRowView(
                        icon: "scissors",
                        iconTint: Theme.forestGreen,
                        title: record.type ?? "Surgery",
                        subtitle: record.surgeon,
                        date: record.date.displayString
                    )
                    .recordSwipeDelete(title: "Surgery") {
                        viewModel.deleteSurgery(record.id)
                    }
                }
            }

            // Medications
            RecordSection(title: "Medications", icon: "pills", count: viewModel.medications.count) {
                ForEach(viewModel.medications, id: \.id) { record in
                    RecordRowView(
                        icon: "pills",
                        iconTint: Theme.forestGreen,
                        title: record.name,
                        subtitle: record.dosage,
                        date: record.startDate?.displayString
                    )
                    .recordSwipeDelete(title: "Medication") {
                        viewModel.deleteMedication(record.id)
                    }
                }
            }

            // Controlled Substances
            RecordSection(title: "Controlled Substances", icon: "lock.shield.fill", count: viewModel.substances.count) {
                ForEach(viewModel.substances, id: \.id) { record in
                    RecordRowView(
                        icon: "lock.shield.fill",
                        iconTint: Theme.forestGreen,
                        title: record.drugName,
                        subtitle: "\(record.dose)\(record.unit.map { " \($0)" } ?? "")",
                        date: record.date.displayString
                    )
                    .recordSwipeDelete(title: "Controlled Substance") {
                        viewModel.deleteSubstance(record.id)
                    }
                }
            }

            // Weights
            RecordSection(title: "Weight Records", icon: "scalemass.fill", count: viewModel.weights.count) {
                ForEach(viewModel.weights, id: \.id) { record in
                    RecordRowView(
                        icon: "scalemass.fill",
                        iconTint: Theme.forestGreen,
                        title: String(format: "%.1f kg", record.weightKg),
                        subtitle: nil,
                        date: record.date.displayString
                    )
                    .recordSwipeDelete(title: "Weight Entry") {
                        viewModel.deleteWeight(record.id)
                    }
                }
            }
        }
        .listStyle(.insetGrouped)
    }
}
