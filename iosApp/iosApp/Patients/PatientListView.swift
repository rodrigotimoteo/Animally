import SwiftUI
import Shared

struct PatientListView: View {
    @StateObject private var viewModel = PatientListViewModel()
    @State private var showSettings = false
    @State private var searchText = ""

    var body: some View {
        Group {
            if viewModel.state.isLoading && viewModel.state.patients.isEmpty {
                loadingView
            } else if viewModel.state.patients.isEmpty {
                emptyView
            } else {
                listView
            }
        }
        .navigationTitle("Patients")
        .toolbar {
            ToolbarItem(placement: .topBarLeading) {
                Button {
                    showSettings = true
                } label: {
                    Image(systemName: "gearshape")
                        .accessibilityLabel("Settings")
                }
            }
            ToolbarItem(placement: .topBarTrailing) {
                NavigationLink(value: Route.patientEdit(nil)) {
                    Image(systemName: "plus")
                        .accessibilityLabel("Add patient")
                }
            }
        }
        .sheet(isPresented: $showSettings) {
            SettingsView()
        }
        .overlay(alignment: .top) {
            if let errorMessage = viewModel.state.errorMessage {
                errorBanner(message: errorMessage)
            }
        }
        .onAppear {
            viewModel.load()
        }
        .searchable(text: $searchText, prompt: "Search patients")
        .onChange(of: searchText) { _, newValue in
            viewModel.setSearchQuery(newValue)
        }
    }

    private var listView: some View {
        List {
            if displayedPatients.isEmpty {
                ContentUnavailableView.search(text: searchText)
            } else {
                Section {
                    ForEach(displayedPatients, id: \.id) { patient in
                        NavigationLink(value: Route.patientDetail(patient.id)) {
                            PatientRowView(patient: patient)
                        }
                        .accessibilityIdentifier("patient_row_\(patient.id)")
                        .confirmationSwipeDelete(
                            title: patient.name,
                            message: "The patient will be removed from the active list. Existing history is kept."
                        ) {
                            viewModel.delete(patientId: patient.id)
                        }
                    }
                }
                header: {
                    Text("\(displayedPatients.count) patient\(displayedPatients.count == 1 ? "" : "s")")
                        .textCase(nil)
                }
            }
        }
        .listStyle(.insetGrouped)
        .refreshable {
            viewModel.load()
        }
    }

    private var loadingView: some View {
        VStack(spacing: 16) {
            ProgressView()
                .scaleEffect(1.2)
            Text("Loading patients…")
                .font(.subheadline)
                .foregroundStyle(Theme.textSecondary)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    private var emptyView: some View {
        VStack(spacing: 20) {
            Image(systemName: "pawprint.circle")
                .font(.system(size: 64))
                .foregroundStyle(Theme.forestGreen.opacity(0.6))
            Text("No patients yet")
                .font(.title2.weight(.semibold))
                .foregroundStyle(Theme.textPrimary)
            Text("Tap + to add one")
                .font(.subheadline)
                .foregroundStyle(Theme.textSecondary)
            NavigationLink(value: Route.patientEdit(nil)) {
                Label("Add your first patient", systemImage: "plus")
                    .font(.subheadline.weight(.semibold))
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    private func errorBanner(message: String) -> some View {
        HStack(spacing: 12) {
            Image(systemName: "exclamationmark.triangle.fill")
                .foregroundStyle(Theme.amber)
            Text(message)
                .font(.subheadline)
                .foregroundStyle(Theme.textPrimary)
                .lineLimit(2)
            Spacer()
            Button {
                viewModel.dismissError()
            } label: {
                Image(systemName: "xmark")
                    .font(.caption.weight(.bold))
                    .foregroundStyle(Theme.textSecondary)
                    .accessibilityLabel("Dismiss error")
            }
        }
        .padding(12)
        .background(Theme.surfaceElevated)
        .clipShape(RoundedRectangle(cornerRadius: 12))
        .shadow(color: .black.opacity(0.08), radius: 8, y: 2)
        .padding(.horizontal)
        .padding(.top, 8)
        .transition(.move(edge: .top).combined(with: .opacity))
    }

    private var displayedPatients: [Patient_] {
        viewModel.state.visiblePatients
    }
}

struct PatientRowView: View {
    let patient: Patient_

    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: "pawprint.fill")
                .font(.title2)
                .foregroundStyle(Theme.forestGreen)
                .frame(width: 44, height: 44)
                .background(Theme.forestGreen.opacity(0.12))
                .clipShape(Circle())

            VStack(alignment: .leading, spacing: 4) {
                Text(patient.name)
                    .font(.headline)
                    .foregroundStyle(Theme.textPrimary)

                if let breed = patient.breed, !breed.isEmpty {
                    Text("\(breed) · \(patient.species)")
                        .font(.subheadline)
                        .foregroundStyle(Theme.textSecondary)
                } else if let microchipId = patient.microchipId, !microchipId.isEmpty {
                    Text("Microchip: \(microchipId)")
                        .font(.subheadline)
                        .foregroundStyle(Theme.textSecondary)
                } else {
                    Text(patient.species)
                        .font(.subheadline)
                        .foregroundStyle(Theme.textSecondary)
                }
                if let stableLocation = patient.stableLocation, !stableLocation.isEmpty {
                    Label(stableLocation, systemImage: "mappin.and.ellipse")
                        .font(.caption)
                        .foregroundStyle(Theme.textTertiary)
                }
            }

            Spacer()
        }
        .padding(.vertical, 4)
        .accessibilityElement(children: .combine)
        .accessibilityLabel("Patient \(patient.name)")
        .accessibilityHint("Opens patient details")
    }
}
