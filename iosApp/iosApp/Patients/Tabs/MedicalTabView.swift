import SwiftUI
import Shared

struct MedicalTabView: View {
    let patientId: Int64
    @StateObject private var viewModel: MedicalTabViewModel
    var onOpenRecord: ((String, Int64) -> Void)? = nil

    init(
        patientId: Int64,
        onOpenRecord: ((String, Int64) -> Void)? = nil,
    ) {
        self.patientId = patientId
        _viewModel = StateObject(wrappedValue: MedicalTabViewModel(patientId: patientId))
        self.onOpenRecord = onOpenRecord
    }

    var body: some View {
        Group {
            if viewModel.isLoading {
                ProgressView("Loading records…")
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                    .accessibilityLabel("Loading medical records")
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
        viewModel.consultations.totalCount +
        viewModel.lamenessRecords.totalCount +
        viewModel.surgeries.totalCount +
        viewModel.medications.totalCount +
        viewModel.substances.totalCount +
        viewModel.weights.totalCount
    }

    private var recordList: some View {
        List {
            recordSection(
                RecordSectionSpec(
                    title: "Consultations",
                    icon: "stethoscope",
                    items: viewModel.consultations.visibleItems,
                    recordId: { $0.id },
                    rowTitle: { $0.assessment.isEmpty ? "Consultation" : $0.assessment },
                    rowSubtitle: { $0.vetName },
                    rowDate: { $0.date.displayString },
                    displayType: "Consultation",
                    onDelete: { viewModel.deleteConsultation($0.id) },
                    display: viewModel.display(for: .consultations)
                ),
                onOpenRecord: onOpenRecord
            )

            recordSection(
                RecordSectionSpec(
                    title: "Lameness Evaluations",
                    icon: "figure.run",
                    items: viewModel.lamenessRecords.visibleItems,
                    recordId: { $0.id },
                    rowTitle: { "AAEP Grade \($0.gradeAAEP)" },
                    rowSubtitle: { $0.limbLocation },
                    rowDate: { $0.date.displayString },
                    displayType: "Lameness",
                    onDelete: { viewModel.deleteLameness($0.id) },
                    deleteTitle: "Lameness Evaluation",
                    display: viewModel.display(for: .lameness)
                ),
                onOpenRecord: onOpenRecord
            )

            recordSection(
                RecordSectionSpec(
                    title: "Surgeries",
                    icon: "scissors",
                    items: viewModel.surgeries.visibleItems,
                    recordId: { $0.id },
                    rowTitle: { $0.type ?? "Surgery" },
                    rowSubtitle: { $0.surgeon },
                    rowDate: { $0.date.displayString },
                    displayType: "Surgery",
                    onDelete: { viewModel.deleteSurgery($0.id) },
                    display: viewModel.display(for: .surgeries)
                ),
                onOpenRecord: onOpenRecord
            )

            recordSection(
                RecordSectionSpec(
                    title: "Medications",
                    icon: "pills",
                    items: viewModel.medications.visibleItems,
                    recordId: { $0.id },
                    rowTitle: { $0.name },
                    rowSubtitle: { $0.dosage },
                    rowDate: { $0.startDate?.displayString },
                    displayType: "Medication",
                    onDelete: { viewModel.deleteMedication($0.id) },
                    display: viewModel.display(for: .medications)
                ),
                onOpenRecord: onOpenRecord
            )

            recordSection(
                RecordSectionSpec(
                    title: "Controlled Substances",
                    icon: "lock.shield.fill",
                    items: viewModel.substances.visibleItems,
                    recordId: { $0.id },
                    rowTitle: { $0.drugName },
                    rowSubtitle: { $0.dose + ($0.unit.map { " \($0)" } ?? "") },
                    rowDate: { $0.date.displayString },
                    displayType: "Controlled Substance",
                    onDelete: { viewModel.deleteSubstance($0.id) },
                    display: viewModel.display(for: .substances)
                ),
                onOpenRecord: onOpenRecord
            )

            recordSection(
                RecordSectionSpec(
                    title: "Weight Records",
                    icon: "scalemass.fill",
                    items: viewModel.weights.visibleItems,
                    recordId: { $0.id },
                    rowTitle: { String(format: "%.1f kg", locale: Locale(identifier: "en_US_POSIX"), $0.weightKg) },
                    rowSubtitle: { _ in nil },
                    rowDate: { $0.date.displayString },
                    displayType: "Weight",
                    onDelete: { viewModel.deleteWeight($0.id) },
                    deleteTitle: "Weight Entry",
                    display: viewModel.display(for: .weights)
                ),
                onOpenRecord: onOpenRecord
            )
        }
        .listStyle(.insetGrouped)
        .accessibilityIdentifier("medical_tab_list")
    }
}
