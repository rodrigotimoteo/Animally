import SwiftUI
import Shared

struct OwnerEditView: View {
    @StateObject private var viewModel: OwnerEditViewModel
    @Environment(\.dismiss) private var dismiss

    init(ownerId: Int64?) {
        _viewModel = StateObject(wrappedValue: OwnerEditViewModel(ownerId: ownerId))
    }

    var body: some View {
        Group {
            if viewModel.state.form?.isLoading == true {
                loadingView
            } else if let form = viewModel.state.form {
                formView(form)
            } else {
                notFoundView
            }
        }
        .navigationTitle(viewModel.form?.isEditing == true ? "Edit Owner" : "New Owner")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button {
                    viewModel.save()
                } label: {
                    if viewModel.state.form?.isSaving == true {
                        ProgressView()
                            .scaleEffect(0.8)
                    } else {
                        Text("Save").fontWeight(.semibold)
                    }
                }
                .disabled(viewModel.state.form?.isSaving == true)
            }
        }
        .overlay(alignment: .top) {
            if let errorMessage = viewModel.state.form?.nameError {
                errorBanner(message: errorMessage)
            }
        }
        .onAppear {
            viewModel.onSaved = { dismiss() }
        }
    }

    private func formView(_ form: OwnerFormState) -> some View {
        return List {
            Section {
                TextField("Name", text: Binding(
                    get: { form.name },
                    set: { viewModel.onNameChange($0) }
                ))
                .textCase(nil)

                if let nameError = form.nameError {
                    Text(nameError)
                        .font(.caption)
                        .foregroundStyle(.red)
                }
            } header: {
                sectionHeader("Name")
            }

            Section {
                TextField("Phone", text: Binding(
                    get: { form.phone ?? "" },
                    set: { viewModel.onPhoneChange($0) }
                ))
                .keyboardType(.phonePad)
                .textCase(nil)

                TextField("Email", text: Binding(
                    get: { form.email ?? "" },
                    set: { viewModel.onEmailChange($0) }
                ))
                .keyboardType(.emailAddress)
                .textInputAutocapitalization(.never)
                .textCase(nil)

                TextField("Address", text: Binding(
                    get: { form.address ?? "" },
                    set: { viewModel.onAddressChange($0) }
                ))
                .lineLimit(2...4)
                .textCase(nil)
            } header: {
                sectionHeader("Contact")
            }
        }
    }

    private func sectionHeader(_ title: String) -> some View {
        Text(title)
            .font(.subheadline.weight(.semibold))
            .foregroundStyle(Theme.forestGreen)
            .textCase(nil)
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
