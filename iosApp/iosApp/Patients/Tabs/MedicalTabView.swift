import SwiftUI
import Shared

struct MedicalTabView: View {
    @StateObject private var viewModel: MedicalTabViewModel
    /// Fires when a record row is tapped; carries the display type, record id,
    /// and the field rows shown on the read-only detail screen.
    var onOpenRecord: ((String, Int64, [RecordDetailNav.FieldRow]) -> Void)? = nil

    init(
        patientId: Int64,
        onOpenRecord: ((String, Int64, [RecordDetailNav.FieldRow]) -> Void)? = nil,
    ) {
        _viewModel = StateObject(wrappedValue: MedicalTabViewModel(patientId: patientId))
        self.onOpenRecord = onOpenRecord
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
                    .contentShape(Rectangle())

                    .onTapGesture {

                        onOpenRecord?("Consultation", record.id, [
                            .init(label: "Date", value: record.date.displayString),
                            .init(label: "Subjective", value: record.subjective),
                            .init(label: "Objective", value: record.objective),
                            .init(label: "Assessment", value: record.assessment),
                            .init(label: "Plan", value: record.plan),
                            .init(label: "Veterinarian", value: record.vetName ?? ""),
                            .init(label: "Next Visit", value: record.nextVisitDate?.displayString ?? ""),
                        ].filter { !$0.value.isEmpty })

                    }

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
                    .contentShape(Rectangle())

                    .onTapGesture {

                        onOpenRecord?("Lameness", record.id, [
                            .init(label: "Date", value: record.date.displayString),
                            .init(label: "AAEP Grade", value: "\(record.gradeAAEP)"),
                            .init(label: "Limb Location", value: record.limbLocation ?? ""),
                            .init(label: "Flexion Test", value: record.flexionTest ?? ""),
                            .init(label: "Diagnosis", value: record.diagnosis ?? ""),
                            .init(label: "Treatment", value: record.treatment ?? ""),
                            .init(label: "Veterinarian", value: record.vetName ?? ""),
                            .init(label: "Notes", value: record.notes ?? ""),
                        ].filter { !$0.value.isEmpty })

                    }

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
                    .contentShape(Rectangle())

                    .onTapGesture {

                        onOpenRecord?("Surgery", record.id, [
                            .init(label: "Date", value: record.date.displayString),
                            .init(label: "Type", value: record.type ?? ""),
                            .init(label: "Description", value: record.description ?? ""),
                            .init(label: "Outcome", value: record.outcome ?? ""),
                            .init(label: "Surgeon", value: record.surgeon ?? ""),
                            .init(label: "Anesthesia", value: record.anesthesia ?? ""),
                            .init(label: "Analgesia", value: record.analgesia ?? ""),
                            .init(label: "Complications", value: record.complications ?? ""),
                            .init(label: "Recovery Notes", value: record.recoveryNotes ?? ""),
                        ].filter { !$0.value.isEmpty })

                    }

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
                    .contentShape(Rectangle())

                    .onTapGesture {

                        onOpenRecord?("Medication", record.id, [
                            .init(label: "Name", value: record.name),
                            .init(label: "Dosage", value: record.dosage),
                            .init(label: "Route", value: record.route ?? ""),
                            .init(label: "Frequency", value: record.frequency ?? ""),
                            .init(label: "Start Date", value: record.startDate?.displayString ?? ""),
                            .init(label: "End Date", value: record.endDate?.displayString ?? ""),
                            .init(label: "Prescribed By", value: record.prescribedBy ?? ""),
                            .init(label: "Notes", value: record.notes ?? ""),
                        ].filter { !$0.value.isEmpty })

                    }

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
                    .contentShape(Rectangle())

                    .onTapGesture {

                        onOpenRecord?("Controlled Substance", record.id, [
                            .init(label: "Drug Name", value: record.drugName),
                            .init(label: "Dose", value: record.dose),
                            .init(label: "Unit", value: record.unit ?? ""),
                            .init(label: "Route", value: record.route ?? ""),
                            .init(label: "Date", value: record.date.displayString),
                            .init(label: "Administered By", value: record.administeredBy ?? ""),
                            .init(label: "Witness", value: record.witness ?? ""),
                            .init(label: "Reason", value: record.reason ?? ""),
                            .init(label: "Notes", value: record.notes ?? ""),
                        ].filter { !$0.value.isEmpty })

                    }

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
                    .contentShape(Rectangle())

                    .onTapGesture {

                        onOpenRecord?("Weight", record.id, [
                            .init(label: "Date", value: record.date.displayString),
                            .init(label: "Weight (kg)", value: String(format: "%.1f kg", record.weightKg)),
                            .init(label: "Notes", value: record.notes ?? ""),
                        ].filter { !$0.value.isEmpty })

                    }

                    .recordSwipeDelete(title: "Weight Entry") {
                        viewModel.deleteWeight(record.id)
                    }
                }
            }
        }
        .listStyle(.insetGrouped)
    }
}
