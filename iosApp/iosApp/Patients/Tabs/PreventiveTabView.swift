import SwiftUI
import Shared

struct PreventiveTabView: View {
    let patientId: Int64
    let refreshToken: Int
    @StateObject private var viewModel: PreventiveTabViewModel
    /// Fires when a record row is tapped; carries the display type, record id,
    /// and the field rows shown on the read-only detail screen.
    var onOpenRecord: ((String, Int64, [RecordDetailNav.FieldRow]) -> Void)? = nil

    init(
        patientId: Int64,
        refreshToken: Int = 0,
        onOpenRecord: ((String, Int64, [RecordDetailNav.FieldRow]) -> Void)? = nil,
    ) {
        self.patientId = patientId
        _viewModel = StateObject(wrappedValue: PreventiveTabViewModel(patientId: patientId))
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
                    icon: "shield.lefthalf.filled",
                    message: "No preventive care records yet"
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
        viewModel.vaccinations.count +
        viewModel.dewormings.count +
        viewModel.dentistryRecords.count +
        viewModel.farrierVisits.count
    }

    private var recordList: some View {
        List {
            recordSection(
                RecordSectionSpec(
                    title: "Vaccinations",
                    icon: "syringe.fill",
                    items: viewModel.vaccinations,
                    recordId: { $0.id },
                    rowTitle: { $0.vaccineName },
                    rowSubtitle: { _ in nil },
                    rowDate: { $0.dateAdministered.displayString },
                    displayType: "Vaccination",
                    fields: { record in [
                        .init(label: "Date Administered", value: record.dateAdministered.displayString),
                        .init(label: "Vaccine", value: record.vaccineName),
                        .init(label: "Batch Number", value: record.batchNumber ?? ""),
                        .init(label: "Site", value: record.site ?? ""),
                        .init(label: "Veterinarian", value: record.vetName ?? ""),
                        .init(label: "Next Due", value: record.nextDueDate?.displayString ?? ""),
                        .init(label: "Notes", value: record.notes ?? ""),
                    ] },
                    onDelete: { viewModel.deleteVaccination($0.id) },
                    extraLine: { $0.nextDueDate?.displayString }
                ),
                onOpenRecord: onOpenRecord
            )

            recordSection(
                RecordSectionSpec(
                    title: "Dewormings",
                    icon: "pills.fill",
                    items: viewModel.dewormings,
                    recordId: { $0.id },
                    rowTitle: { $0.product },
                    rowSubtitle: { $0.dose },
                    rowDate: { $0.dateAdministered.displayString },
                    displayType: "Deworming",
                    fields: { record in [
                        .init(label: "Product", value: record.product),
                        .init(label: "Dose", value: record.dose ?? ""),
                        .init(label: "Date Administered", value: record.dateAdministered.displayString),
                        .init(label: "Next Due", value: record.nextDueDate?.displayString ?? ""),
                        .init(label: "Veterinarian", value: record.vetName ?? ""),
                        .init(label: "Notes", value: record.notes ?? ""),
                    ] },
                    onDelete: { viewModel.deleteDeworming($0.id) },
                    extraLine: { $0.nextDueDate?.displayString }
                ),
                onOpenRecord: onOpenRecord
            )

            recordSection(
                RecordSectionSpec(
                    title: "Dentistry",
                    icon: "mouth.fill",
                    items: viewModel.dentistryRecords,
                    recordId: { $0.id },
                    rowTitle: { $0.treatment ?? "Dental check" },
                    rowSubtitle: { $0.findings },
                    rowDate: { $0.date.displayString },
                    displayType: "Dentistry",
                    fields: { record in [
                        .init(label: "Date", value: record.date.displayString),
                        .init(label: "Treatment", value: record.treatment ?? ""),
                        .init(label: "Findings", value: record.findings ?? ""),
                        .init(label: "Next Due", value: record.nextDueDate?.displayString ?? ""),
                        .init(label: "Veterinarian", value: record.vetName ?? ""),
                        .init(label: "Notes", value: record.notes ?? ""),
                    ] },
                    onDelete: { viewModel.deleteDentistry($0.id) },
                    deleteTitle: "Dentistry Record",
                    extraLine: { $0.nextDueDate?.displayString }
                ),
                onOpenRecord: onOpenRecord
            )

            recordSection(
                RecordSectionSpec(
                    title: "Farrier Visits",
                    icon: "figure.walk",
                    items: viewModel.farrierVisits,
                    recordId: { $0.id },
                    rowTitle: { $0.trimOrShoe ?? "Farrier visit" },
                    rowSubtitle: { $0.farrier },
                    rowDate: { $0.date.displayString },
                    displayType: "Farrier",
                    fields: { record in [
                        .init(label: "Date", value: record.date.displayString),
                        .init(label: "Trim or Shoe", value: record.trimOrShoe ?? ""),
                        .init(label: "Shoe Type", value: record.shoeType ?? ""),
                        .init(label: "Findings", value: record.findings ?? ""),
                        .init(label: "Next Due", value: record.nextDueDate?.displayString ?? ""),
                        .init(label: "Farrier", value: record.farrier ?? ""),
                        .init(label: "Notes", value: record.notes ?? ""),
                    ] },
                    onDelete: { viewModel.deleteFarrierVisit($0.id) },
                    deleteTitle: "Farrier Visit",
                    extraLine: { $0.nextDueDate?.displayString }
                ),
                onOpenRecord: onOpenRecord
            )
        }
        .listStyle(.insetGrouped)
    }
}
