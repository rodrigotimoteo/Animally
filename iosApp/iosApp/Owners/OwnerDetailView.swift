import SwiftUI
import Shared

struct OwnerDetailView: View {
    @StateObject private var viewModel: OwnerDetailViewModel
    @State private var navigationPath = NavigationPath()

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

                            Image(systemName: "chevron.right")
                                .font(.caption.weight(.semibold))
                                .foregroundStyle(Theme.textTertiary)
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
