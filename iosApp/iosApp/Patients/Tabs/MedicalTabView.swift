import SwiftUI
import Shared

struct MedicalTabView: View {
    let patientId: Int64
    let refreshToken: Int
    @StateObject private var viewModel: MedicalTabViewModel
    /// Fires when a record row is tapped; carries the display type, record id,
    /// and the field rows shown on the read-only detail screen.
    var onOpenRecord: ((String, Int64, [RecordDetailNav.FieldRow]) -> Void)? = nil

    init(
        patientId: Int64,
        refreshToken: Int = 0,
        onOpenRecord: ((String, Int64, [RecordDetailNav.FieldRow]) -> Void)? = nil,
    ) {
        self.patientId = patientId
        _viewModel = StateObject(wrappedValue: MedicalTabViewModel(patientId: patientId))
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
                    icon: "cross.case.fill",
                    message: "No medical records yet"
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
        viewModel.consultations.count +
        viewModel.lamenessRecords.count +
        viewModel.surgeries.count +
        viewModel.medications.count +
        viewModel.substances.count +
        viewModel.weights.count
    }

    private var recordList: some View {
        List {
            recordSection(
                RecordSectionSpec(
                    title: "Consultations",
                    icon: "stethoscope",
                    items: viewModel.consultations,
                    recordId: { $0.id },
                    rowTitle: { $0.assessment.isEmpty ? "Consultation" : $0.assessment },
                    rowSubtitle: { $0.vetName },
                    rowDate: { $0.date.displayString },
                    displayType: "Consultation",
                    fields: { record in [
                        .init(label: "Date", value: record.date.displayString),
                        .init(label: "Subjective", value: record.subjective),
                        .init(label: "Objective", value: record.objective),
                        .init(label: "Assessment", value: record.assessment),
                        .init(label: "Plan", value: record.plan),
                        .init(label: "Veterinarian", value: record.vetName ?? ""),
                        .init(label: "Next Visit", value: record.nextVisitDate?.displayString ?? ""),
                    ] },
                    onDelete: { viewModel.deleteConsultation($0.id) }
                ),
                onOpenRecord: onOpenRecord
            )

            recordSection(
                RecordSectionSpec(
                    title: "Lameness Evaluations",
                    icon: "figure.run",
                    items: viewModel.lamenessRecords,
                    recordId: { $0.id },
                    rowTitle: { "AAEP Grade \($0.gradeAAEP)" },
                    rowSubtitle: { $0.limbLocation },
                    rowDate: { $0.date.displayString },
                    displayType: "Lameness",
                    fields: { record in [
                        .init(label: "Date", value: record.date.displayString),
                        .init(label: "AAEP Grade", value: "\(record.gradeAAEP)"),
                        .init(label: "Limb Location", value: record.limbLocation ?? ""),
                        .init(label: "Flexion Test", value: record.flexionTest ?? ""),
                        .init(label: "Diagnosis", value: record.diagnosis ?? ""),
                        .init(label: "Treatment", value: record.treatment ?? ""),
                        .init(label: "Veterinarian", value: record.vetName ?? ""),
                        .init(label: "Notes", value: record.notes ?? ""),
                    ] },
                    onDelete: { viewModel.deleteLameness($0.id) },
                    deleteTitle: "Lameness Evaluation"
                ),
                onOpenRecord: onOpenRecord
            )

            recordSection(
                RecordSectionSpec(
                    title: "Surgeries",
                    icon: "scissors",
                    items: viewModel.surgeries,
                    recordId: { $0.id },
                    rowTitle: { $0.type ?? "Surgery" },
                    rowSubtitle: { $0.surgeon },
                    rowDate: { $0.date.displayString },
                    displayType: "Surgery",
                    fields: { record in [
                        .init(label: "Date", value: record.date.displayString),
                        .init(label: "Type", value: record.type ?? ""),
                        .init(label: "Description", value: record.description ?? ""),
                        .init(label: "Outcome", value: record.outcome ?? ""),
                        .init(label: "Surgeon", value: record.surgeon ?? ""),
                        .init(label: "Anesthesia", value: record.anesthesia ?? ""),
                        .init(label: "Analgesia", value: record.analgesia ?? ""),
                        .init(label: "Complications", value: record.complications ?? ""),
                        .init(label: "Recovery Notes", value: record.recoveryNotes ?? ""),
                    ] },
                    onDelete: { viewModel.deleteSurgery($0.id) }
                ),
                onOpenRecord: onOpenRecord
            )

            recordSection(
                RecordSectionSpec(
                    title: "Medications",
                    icon: "pills",
                    items: viewModel.medications,
                    recordId: { $0.id },
                    rowTitle: { $0.name },
                    rowSubtitle: { $0.dosage },
                    rowDate: { $0.startDate?.displayString },
                    displayType: "Medication",
                    fields: { record in [
                        .init(label: "Name", value: record.name),
                        .init(label: "Dosage", value: record.dosage),
                        .init(label: "Route", value: record.route ?? ""),
                        .init(label: "Frequency", value: record.frequency ?? ""),
                        .init(label: "Start Date", value: record.startDate?.displayString ?? ""),
                        .init(label: "End Date", value: record.endDate?.displayString ?? ""),
                        .init(label: "Prescribed By", value: record.prescribedBy ?? ""),
                        .init(label: "Notes", value: record.notes ?? ""),
                    ] },
                    onDelete: { viewModel.deleteMedication($0.id) }
                ),
                onOpenRecord: onOpenRecord
            )

            recordSection(
                RecordSectionSpec(
                    title: "Controlled Substances",
                    icon: "lock.shield.fill",
                    items: viewModel.substances,
                    recordId: { $0.id },
                    rowTitle: { $0.drugName },
                    rowSubtitle: { $0.dose + ($0.unit.map { " \($0)" } ?? "") },
                    rowDate: { $0.date.displayString },
                    displayType: "Controlled Substance",
                    fields: { record in [
                        .init(label: "Drug Name", value: record.drugName),
                        .init(label: "Dose", value: record.dose),
                        .init(label: "Unit", value: record.unit ?? ""),
                        .init(label: "Route", value: record.route ?? ""),
                        .init(label: "Date", value: record.date.displayString),
                        .init(label: "Administered By", value: record.administeredBy ?? ""),
                        .init(label: "Witness", value: record.witness ?? ""),
                        .init(label: "Reason", value: record.reason ?? ""),
                        .init(label: "Notes", value: record.notes ?? ""),
                    ] },
                    onDelete: { viewModel.deleteSubstance($0.id) }
                ),
                onOpenRecord: onOpenRecord
            )

            recordSection(
                RecordSectionSpec(
                    title: "Weight Records",
                    icon: "scalemass.fill",
                    items: viewModel.weights,
                    recordId: { $0.id },
                    rowTitle: { String(format: "%.1f kg", $0.weightKg) },
                    rowSubtitle: { _ in nil },
                    rowDate: { $0.date.displayString },
                    displayType: "Weight",
                    fields: { record in [
                        .init(label: "Date", value: record.date.displayString),
                        .init(label: "Weight (kg)", value: String(format: "%.1f kg", record.weightKg)),
                        .init(label: "Notes", value: record.notes ?? ""),
                    ] },
                    onDelete: { viewModel.deleteWeight($0.id) },
                    deleteTitle: "Weight Entry"
                ),
                onOpenRecord: onOpenRecord
            )
        }
        .listStyle(.insetGrouped)
    }
}
