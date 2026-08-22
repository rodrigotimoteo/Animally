import SwiftUI
import Shared

struct PreventiveTabView: View {
    @StateObject private var viewModel: PreventiveTabViewModel
    /// Fires when a record row is tapped; args are the display type name and record id.
    var onEditRecord: ((String, Int64) -> Void)? = nil

    init(
        patientId: Int64,
        onEditRecord: ((String, Int64) -> Void)? = nil,
    ) {
        _viewModel = StateObject(wrappedValue: PreventiveTabViewModel(patientId: patientId))
        self.onEditRecord = onEditRecord
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
                        onEditRecord?("Vaccination", record.id)
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
                        onEditRecord?("Deworming", record.id)
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
                        onEditRecord?("Dentistry", record.id)
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
                        onEditRecord?("Farrier", record.id)
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
