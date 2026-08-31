import SwiftUI
import Shared

struct PreventiveTabView: View {
    let patientId: Int64
    @StateObject private var viewModel: PreventiveTabViewModel
    var onOpenRecord: ((String, Int64) -> Void)? = nil

    init(
        patientId: Int64,
        onOpenRecord: ((String, Int64) -> Void)? = nil,
    ) {
        self.patientId = patientId
        _viewModel = StateObject(wrappedValue: PreventiveTabViewModel(patientId: patientId))
        self.onOpenRecord = onOpenRecord
    }

    var body: some View {
        Group {
            if viewModel.isLoading {
                ProgressView("Loading records…")
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                    .accessibilityLabel("Loading preventive records")
            } else if totalRecords == 0 {
                TabEmptyStateView(
                    icon: "shield.lefthalf.filled",
                    message: "No preventive care records yet"
                )
            } else {
                recordList
            }
        }
    }

    private var totalRecords: Int {
        viewModel.vaccinations.totalCount +
        viewModel.dewormings.totalCount +
        viewModel.dentistryRecords.totalCount +
        viewModel.farrierVisits.totalCount
    }

    private var recordList: some View {
        List {
            recordSection(
                RecordSectionSpec(
                    title: "Vaccinations",
                    icon: "syringe.fill",
                    items: viewModel.vaccinations.visibleItems,
                    recordId: { $0.id },
                    rowTitle: { $0.vaccineName },
                    rowSubtitle: { _ in nil },
                    rowDate: { $0.dateAdministered.displayString },
                    displayType: "Vaccination",
                    onDelete: { viewModel.deleteVaccination($0.id) },
                    extraLine: { $0.nextDueDate?.displayString },
                    display: viewModel.display(for: .vaccinations)
                ),
                onOpenRecord: onOpenRecord
            )

            recordSection(
                RecordSectionSpec(
                    title: "Dewormings",
                    icon: "pills.fill",
                    items: viewModel.dewormings.visibleItems,
                    recordId: { $0.id },
                    rowTitle: { $0.product },
                    rowSubtitle: { $0.dose },
                    rowDate: { $0.dateAdministered.displayString },
                    displayType: "Deworming",
                    onDelete: { viewModel.deleteDeworming($0.id) },
                    extraLine: { $0.nextDueDate?.displayString },
                    display: viewModel.display(for: .dewormings)
                ),
                onOpenRecord: onOpenRecord
            )

            recordSection(
                RecordSectionSpec(
                    title: "Dentistry",
                    icon: "mouth.fill",
                    items: viewModel.dentistryRecords.visibleItems,
                    recordId: { $0.id },
                    rowTitle: { $0.treatment ?? "Dental check" },
                    rowSubtitle: { $0.findings },
                    rowDate: { $0.date.displayString },
                    displayType: "Dentistry",
                    onDelete: { viewModel.deleteDentistry($0.id) },
                    deleteTitle: "Dentistry Record",
                    extraLine: { $0.nextDueDate?.displayString },
                    display: viewModel.display(for: .dentistry)
                ),
                onOpenRecord: onOpenRecord
            )

            recordSection(
                RecordSectionSpec(
                    title: "Farrier Visits",
                    icon: "figure.walk",
                    items: viewModel.farrierVisits.visibleItems,
                    recordId: { $0.id },
                    rowTitle: { $0.trimOrShoe ?? "Farrier visit" },
                    rowSubtitle: { $0.farrier },
                    rowDate: { $0.date.displayString },
                    displayType: "Farrier",
                    onDelete: { viewModel.deleteFarrierVisit($0.id) },
                    deleteTitle: "Farrier Visit",
                    extraLine: { $0.nextDueDate?.displayString },
                    display: viewModel.display(for: .farrier)
                ),
                onOpenRecord: onOpenRecord
            )
        }
        .listStyle(.insetGrouped)
        .accessibilityIdentifier("preventive_tab_list")
    }
}
