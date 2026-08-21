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
        viewModel.vaccinations.count +
        viewModel.dewormings.count +
        viewModel.dentistryRecords.count +
        viewModel.farrierVisits.count +
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
                }
            }

            // Vaccinations
            RecordSection(title: "Vaccinations", icon: "syringe", count: viewModel.vaccinations.count) {
                ForEach(viewModel.vaccinations, id: \.id) { record in
                    RecordRowView(
                        icon: "syringe",
                        iconTint: Theme.forestGreen,
                        title: record.vaccineName,
                        subtitle: record.vetName,
                        date: record.dateAdministered.displayString
                    )
                }
            }

            // Dewormings
            RecordSection(title: "Dewormings", icon: "pills.fill", count: viewModel.dewormings.count) {
                ForEach(viewModel.dewormings, id: \.id) { record in
                    RecordRowView(
                        icon: "pills.fill",
                        iconTint: Theme.forestGreen,
                        title: record.product,
                        subtitle: record.dose,
                        date: record.dateAdministered.displayString
                    )
                }
            }

            // Dentistry
            RecordSection(title: "Dentistry", icon: "mouth.fill", count: viewModel.dentistryRecords.count) {
                ForEach(viewModel.dentistryRecords, id: \.id) { record in
                    RecordRowView(
                        icon: "mouth.fill",
                        iconTint: Theme.forestGreen,
                        title: record.treatment ?? "Dental check",
                        subtitle: record.findings,
                        date: record.date.displayString
                    )
                }
            }

            // Farrier
            RecordSection(title: "Farrier Visits", icon: "figure.walk", count: viewModel.farrierVisits.count) {
                ForEach(viewModel.farrierVisits, id: \.id) { record in
                    RecordRowView(
                        icon: "figure.walk",
                        iconTint: Theme.forestGreen,
                        title: record.trimOrShoe ?? "Farrier visit",
                        subtitle: record.farrier,
                        date: record.date.displayString
                    )
                }
            }

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
                }
            }
        }
        .listStyle(.insetGrouped)
    }
}
