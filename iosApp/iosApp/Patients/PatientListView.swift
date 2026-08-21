import SwiftUI
import Shared

struct PatientListView: View {
    @StateObject private var viewModel = PatientListViewModel()
    @State private var showSettings = false

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
    }

    private var listView: some View {
        List {
            ForEach(viewModel.state.patients, id: \.id) { patient in
                NavigationLink(value: Route.patientDetail(patient.id)) {
                    PatientRowView(patient: patient)
                }
                .swipeActions(edge: .trailing, allowsFullSwipe: false) {
                    Button(role: .destructive) {
                        viewModel.delete(patientId: patient.id)
                    } label: {
                        Label("Delete", systemImage: "trash")
                    }
                }
            }
        }
        .listStyle(.insetGrouped)
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
                    Text(breed)
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
            }

            Spacer()

            Image(systemName: "chevron.right")
                .font(.caption.weight(.semibold))
                .foregroundStyle(Theme.textTertiary)
        }
        .padding(.vertical, 4)
        .accessibilityElement(children: .combine)
        .accessibilityLabel("Patient \(patient.name)")
        .accessibilityHint("Opens patient details")
    }
}
