import SwiftUI
import Shared

struct PreventiveTabView: View {
    @StateObject private var viewModel: PreventiveTabViewModel
    /// Fires when a record row is tapped; carries the display type, record id,
    /// and the field rows shown on the read-only detail screen.
    var onOpenRecord: ((String, Int64, [RecordDetailNav.FieldRow]) -> Void)? = nil

    init(
        patientId: Int64,
        onOpenRecord: ((String, Int64, [RecordDetailNav.FieldRow]) -> Void)? = nil,
    ) {
        _viewModel = StateObject(wrappedValue: PreventiveTabViewModel(patientId: patientId))
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
    }

    private var totalRecords: Int {
        viewModel.vaccinations.count +
        viewModel.dewormings.count +
        viewModel.dentistryRecords.count +
        viewModel.farrierVisits.count
    }

    private var recordList: some View {
        List {
            // Vaccinations
            RecordSection(title: "Vaccinations", icon: "syringe", count: viewModel.vaccinations.count) {
                ForEach(viewModel.vaccinations, id: \.id) { record in
                    VStack(alignment: .leading, spacing: 6) {
                        RecordRowView(
                            icon: "syringe",
                            iconTint: Theme.forestGreen,
                            title: record.vaccineName,
                            subtitle: record.vetName,
                            date: record.dateAdministered.displayString
                        )
                        if let nextDue = record.nextDueDate {
                            HStack(spacing: 4) {
                                Image(systemName: "calendar")
                                    .font(.caption2)
                                Text("Next due: \(nextDue.displayString)")
                                    .font(.caption2)
                            }
                            .foregroundStyle(Theme.amber)
                            .padding(.leading, 48)
                        }
                    }
                    .contentShape(Rectangle())
                    .onTapGesture {
                        onOpenRecord?("Vaccination", record.id, [
                            .init(label: "Date Administered", value: record.dateAdministered.displayString),
                            .init(label: "Vaccine", value: record.vaccineName),
                            .init(label: "Batch Number", value: record.batchNumber ?? ""),
                            .init(label: "Site", value: record.site ?? ""),
                            .init(label: "Veterinarian", value: record.vetName ?? ""),
                            .init(label: "Next Due", value: record.nextDueDate?.displayString ?? ""),
                            .init(label: "Notes", value: record.notes ?? ""),
                        ].filter { !$0.value.isEmpty })
                    }
                    .recordSwipeDelete(title: "Vaccination") {
                        viewModel.deleteVaccination(record.id)
                    }
                }
            }

            // Dewormings
            RecordSection(title: "Dewormings", icon: "pills.fill", count: viewModel.dewormings.count) {
                ForEach(viewModel.dewormings, id: \.id) { record in
                    VStack(alignment: .leading, spacing: 6) {
                        RecordRowView(
                            icon: "pills.fill",
                            iconTint: Theme.forestGreen,
                            title: record.product,
                            subtitle: record.dose,
                            date: record.dateAdministered.displayString
                        )
                        if let nextDue = record.nextDueDate {
                            HStack(spacing: 4) {
                                Image(systemName: "calendar")
                                    .font(.caption2)
                                Text("Next due: \(nextDue.displayString)")
                                    .font(.caption2)
                            }
                            .foregroundStyle(Theme.amber)
                            .padding(.leading, 48)
                        }
                    }
                    .contentShape(Rectangle())
                    .onTapGesture {
                        onOpenRecord?("Deworming", record.id, [
                            .init(label: "Product", value: record.product),
                            .init(label: "Dose", value: record.dose ?? ""),
                            .init(label: "Date Administered", value: record.dateAdministered.displayString),
                            .init(label: "Next Due", value: record.nextDueDate?.displayString ?? ""),
                            .init(label: "Veterinarian", value: record.vetName ?? ""),
                            .init(label: "Notes", value: record.notes ?? ""),
                        ].filter { !$0.value.isEmpty })
                    }
                    .recordSwipeDelete(title: "Deworming") {
                        viewModel.deleteDeworming(record.id)
                    }
                }
            }

            // Dentistry
            RecordSection(title: "Dentistry", icon: "mouth.fill", count: viewModel.dentistryRecords.count) {
                ForEach(viewModel.dentistryRecords, id: \.id) { record in
                    VStack(alignment: .leading, spacing: 6) {
                        RecordRowView(
                            icon: "mouth.fill",
                            iconTint: Theme.forestGreen,
                            title: record.treatment ?? "Dental check",
                            subtitle: record.findings,
                            date: record.date.displayString
                        )
                        if let nextDue = record.nextDueDate {
                            HStack(spacing: 4) {
                                Image(systemName: "calendar")
                                    .font(.caption2)
                                Text("Next due: \(nextDue.displayString)")
                                    .font(.caption2)
                            }
                            .foregroundStyle(Theme.amber)
                            .padding(.leading, 48)
                        }
                    }
                    .contentShape(Rectangle())
                    .onTapGesture {
                        onOpenRecord?("Dentistry", record.id, [
                            .init(label: "Date", value: record.date.displayString),
                            .init(label: "Treatment", value: record.treatment ?? ""),
                            .init(label: "Findings", value: record.findings ?? ""),
                            .init(label: "Next Due", value: record.nextDueDate?.displayString ?? ""),
                            .init(label: "Veterinarian", value: record.vetName ?? ""),
                            .init(label: "Notes", value: record.notes ?? ""),
                        ].filter { !$0.value.isEmpty })
                    }
                    .recordSwipeDelete(title: "Dentistry Record") {
                        viewModel.deleteDentistry(record.id)
                    }
                }
            }

            // Farrier
            RecordSection(title: "Farrier Visits", icon: "figure.walk", count: viewModel.farrierVisits.count) {
                ForEach(viewModel.farrierVisits, id: \.id) { record in
                    VStack(alignment: .leading, spacing: 6) {
                        RecordRowView(
                            icon: "figure.walk",
                            iconTint: Theme.forestGreen,
                            title: record.trimOrShoe ?? "Farrier visit",
                            subtitle: record.farrier,
                            date: record.date.displayString
                        )
                        if let nextDue = record.nextDueDate {
                            HStack(spacing: 4) {
                                Image(systemName: "calendar")
                                    .font(.caption2)
                                Text("Next due: \(nextDue.displayString)")
                                    .font(.caption2)
                            }
                            .foregroundStyle(Theme.amber)
                            .padding(.leading, 48)
                        }
                    }
                    .contentShape(Rectangle())
                    .onTapGesture {
                        onOpenRecord?("Farrier", record.id, [
                            .init(label: "Date", value: record.date.displayString),
                            .init(label: "Trim or Shoe", value: record.trimOrShoe ?? ""),
                            .init(label: "Shoe Type", value: record.shoeType ?? ""),
                            .init(label: "Findings", value: record.findings ?? ""),
                            .init(label: "Next Due", value: record.nextDueDate?.displayString ?? ""),
                            .init(label: "Farrier", value: record.farrier ?? ""),
                            .init(label: "Notes", value: record.notes ?? ""),
                        ].filter { !$0.value.isEmpty })
                    }
                    .recordSwipeDelete(title: "Farrier Visit") {
                        viewModel.deleteFarrierVisit(record.id)
                    }
                }
            }
        }
        .listStyle(.insetGrouped)
    }
}
