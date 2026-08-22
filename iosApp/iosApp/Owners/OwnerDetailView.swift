import SwiftUI
import Shared

struct OwnerDetailView: View {
    @StateObject private var viewModel: OwnerDetailViewModel
    @State private var showLinkPatientSheet = false

    init(ownerId: Int64) {
        _viewModel = StateObject(wrappedValue: OwnerDetailViewModel(ownerId: ownerId))
    }

    var body: some View {
        Group {
            if viewModel.state.isLoading && viewModel.state.owner == nil {
                loadingView
            } else if let owner = viewModel.state.owner {
                content(owner: owner)
            } else {
                notFoundView
            }
        }
        .navigationTitle(viewModel.state.owner?.name ?? "Owner")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                NavigationLink(value: Route.ownerEdit(viewModel.state.owner?.id)) {
                    Image(systemName: "pencil")
                        .accessibilityLabel("Edit owner")
                }
            }
            ToolbarItem(placement: .topBarTrailing) {
                Button {
                    showLinkPatientSheet = true
                } label: {
                    Image(systemName: "plus")
                        .accessibilityLabel("Link existing patient")
                }
            }
        }
        .sheet(isPresented: $showLinkPatientSheet) {
            LinkPatientSheet(ownerId: viewModel.state.owner?.id ?? 0)
        }
        .onChange(of: showLinkPatientSheet) { _, isPresented in
            // Refresh the patient list after the sheet closes (linked or cancelled).
            if !isPresented {
                viewModel.load()
            }
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

    private func content(owner: Owner_) -> some View {
        ScrollView {
            VStack(spacing: 20) {
                ownerHeader
                contactSection(owner: owner)
                patientsSection
            }
            .padding()
        }
    }

    private var ownerHeader: some View {
        VStack(spacing: 12) {
            Image(systemName: "person.crop.circle.fill")
                .font(.system(size: 80))
                .foregroundStyle(Theme.forestGreen)
            Text(viewModel.state.owner?.name ?? "")
                .font(.largeTitle.weight(.bold))
                .foregroundStyle(Theme.textPrimary)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 20)
    }

    private func contactSection(owner: Owner_) -> some View {
        Section {
            infoRow(label: "Phone", value: owner.phone)
            infoRow(label: "Email", value: owner.email)
            infoRow(label: "Address", value: owner.address)
        } header: {
            sectionHeader("Contact")
        }
    }

    private var patientsSection: some View {
        Section {
            if viewModel.state.patients.isEmpty {
                Text("No patients linked")
                    .font(.subheadline)
                    .foregroundStyle(Theme.textTertiary)
            } else {
                ForEach(viewModel.state.patients, id: \.id) { patient in
                    NavigationLink(value: Route.patientDetail(patient.id)) {
                        HStack(spacing: 12) {
                            Image(systemName: "pawprint.fill")
                                .font(.body)
                                .foregroundStyle(Theme.forestGreen)
                                .frame(width: 32, height: 32)
                                .background(Theme.forestGreen.opacity(0.12))
                                .clipShape(Circle())

                            VStack(alignment: .leading, spacing: 2) {
                                Text(patient.name)
                                    .font(.subheadline.weight(.semibold))
                                    .foregroundStyle(Theme.textPrimary)
                                if let breed = patient.breed, !breed.isEmpty {
                                    Text(breed)
                                        .font(.caption)
                                        .foregroundStyle(Theme.textSecondary)
                                }
                            }

                            Spacer()
                        }
                        .padding(.vertical, 2)
                    }
                    .buttonStyle(.plain)
                }
            }
        } header: {
            sectionHeader("Patients")
        }
    }

    private func sectionHeader(_ title: String) -> some View {
        Text(title)
            .font(.subheadline.weight(.semibold))
            .foregroundStyle(Theme.forestGreen)
            .textCase(nil)
    }

    private func infoRow(label: String, value: String?) -> some View {
        HStack {
            Text(label)
                .foregroundStyle(Theme.textSecondary)
            Spacer()
            Text(value ?? "—")
                .foregroundStyle(value != nil ? Theme.textPrimary : Theme.textTertiary)
        }
        .font(.subheadline)
    }

    private var loadingView: some View {
        VStack(spacing: 16) {
            ProgressView()
                .scaleEffect(1.2)
            Text("Loading owner…")
                .font(.subheadline)
                .foregroundStyle(Theme.textSecondary)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    private var notFoundView: some View {
        VStack(spacing: 20) {
            Image(systemName: "exclamationmark.triangle")
                .font(.system(size: 64))
                .foregroundStyle(Theme.amber)
            Text("Owner not found")
                .font(.title2.weight(.semibold))
                .foregroundStyle(Theme.textPrimary)
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

/// Sheet listing patients that can be linked to this owner.
private struct LinkPatientSheet: View {
    @StateObject private var viewModel: LinkPatientViewModel
    @Environment(\.dismiss) private var dismiss

    init(ownerId: Int64) {
        _viewModel = StateObject(wrappedValue: LinkPatientViewModel(ownerId: ownerId))
    }

    var body: some View {
        NavigationStack {
            Group {
                if viewModel.sections.isEmpty {
                    emptyView
                } else {
                    listView
                }
            }
            .navigationTitle("Link Patient")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("Done") {
                        dismiss()
                    }
                }
            }
            .overlay(alignment: .top) {
                if let errorMessage = viewModel.errorMessage {
                    errorBanner(message: errorMessage)
                }
            }
        }
        .onAppear {
            viewModel.onLinked = {
                dismiss()
            }
        }
    }

    private var listView: some View {
        List {
            ForEach(viewModel.sections) { section in
                Section(section.title) {
                    ForEach(section.patients, id: \.id) { patient in
                        Button {
                            viewModel.link(patient)
                        } label: {
                            HStack(spacing: 12) {
                                Image(systemName: "pawprint.fill")
                                    .font(.body)
                                    .foregroundStyle(Theme.forestGreen)
                                    .frame(width: 32, height: 32)
                                    .background(Theme.forestGreen.opacity(0.12))
                                    .clipShape(Circle())

                                VStack(alignment: .leading, spacing: 2) {
                                    Text(patient.name)
                                        .font(.subheadline.weight(.semibold))
                                        .foregroundStyle(Theme.textPrimary)
                                    if let breed = patient.breed, !breed.isEmpty {
                                        Text(breed)
                                            .font(.caption)
                                            .foregroundStyle(Theme.textSecondary)
                                    }
                                }

                                Spacer()

                                if viewModel.linkingPatientId == patient.id {
                                    ProgressView()
                                        .scaleEffect(0.8)
                                } else {
                                    Image(systemName: "link")
                                        .font(.caption.weight(.semibold))
                                        .foregroundStyle(Theme.textTertiary)
                                }
                            }
                        }
                        .disabled(viewModel.linkingPatientId != nil)
                    }
                }
            }
        }
    }

    private var emptyView: some View {
        VStack(spacing: 16) {
            Image(systemName: "pawprint")
                .font(.system(size: 56))
                .foregroundStyle(Theme.textTertiary)
            Text("No patients to link")
                .font(.headline)
                .foregroundStyle(Theme.textPrimary)
            Text("Every patient is already linked to this owner.")
                .font(.subheadline)
                .foregroundStyle(Theme.textSecondary)
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .padding()
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
