import SwiftUI
import Shared

struct PatientEditView: View {
    @StateObject private var viewModel: PatientEditViewModel
    @Environment(\.dismiss) private var dismiss

    private let genderOptions = ["Mare", "Gelding", "Stallion", "Filly", "Colt", "Unknown"]

    init(patientId: Int64?, preselectedOwnerId: Int64? = nil) {
        _viewModel = StateObject(wrappedValue: PatientEditViewModel(patientId: patientId, preselectedOwnerId: preselectedOwnerId))
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
        .navigationTitle(viewModel.form?.isEditing == true ? "Edit Patient" : "New Patient")
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
            if let errorMessage = viewModel.state.form?.nameError ?? viewModel.state.form?.uelnError {
                errorBanner(message: errorMessage)
            }
        }
        .onAppear {
            viewModel.onSaved = { dismiss() }
        }
    }

    // MARK: - Form

    private func formView(_ form: PatientFormState) -> some View {
        return List {
            nameSection(form)
            basicInfoSection(form)
            identificationSection(form)
            cogginsSection(form)
            ownerSection(form)
            notesSection(form)
        }
    }

    private func nameSection(_ form: PatientFormState) -> some View {
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
    }

    private func basicInfoSection(_ form: PatientFormState) -> some View {
        Section {
            TextField("Species", text: Binding(
                get: { form.species },
                set: { viewModel.onSpeciesChange($0) }
            ))
            .textCase(nil)

            TextField("Breed", text: Binding(
                get: { form.breed ?? "" },
                set: { viewModel.onBreedChange($0) }
            ))
            .textCase(nil)

            Picker("Gender", selection: Binding(
                get: { form.gender },
                set: { viewModel.onGenderChange($0) }
            )) {
                Text("None").tag(String?.none)
                ForEach(genderOptions, id: \.self) { gender in
                    Text(gender).tag(String?.some(gender))
                }
            }
            .pickerStyle(.menu)

            TextField("YYYY-MM-DD", text: Binding(
                get: { form.dateOfBirth ?? "" },
                set: { viewModel.onDateOfBirthChange($0) }
            ))
            .keyboardType(.numbersAndPunctuation)
            .textCase(nil)
        } header: {
            sectionHeader("Basic Information")
        }
    }

    private func identificationSection(_ form: PatientFormState) -> some View {
        Section {
            TextField("Microchip", text: Binding(
                get: { form.microchipId ?? "" },
                set: { viewModel.onMicrochipIdChange($0) }
            ))
            .textCase(nil)

            TextField("UELN", text: Binding(
                get: { form.ueln ?? "" },
                set: { viewModel.onUelnChange($0) }
            ))
            .keyboardType(.numberPad)
            .textCase(nil)

            if let uelnError = form.uelnError {
                Text(uelnError)
                    .font(.caption)
                    .foregroundStyle(.red)
            }

            TextField("Registration №", text: Binding(
                get: { form.registrationNumber ?? "" },
                set: { viewModel.onRegistrationNumberChange($0) }
            ))
            .textCase(nil)

            TextField("Stable location", text: Binding(
                get: { form.stableLocation ?? "" },
                set: { viewModel.onStableLocationChange($0) }
            ))
            .textCase(nil)
        } header: {
            sectionHeader("Identification")
        }
    }

    private func cogginsSection(_ form: PatientFormState) -> some View {
        Section {
            TextField("Test date (YYYY-MM-DD)", text: Binding(
                get: { form.cogginsTestDate ?? "" },
                set: { viewModel.onCogginsTestDateChange($0) }
            ))
            .keyboardType(.numbersAndPunctuation)
            .textCase(nil)

            TextField("Result", text: Binding(
                get: { form.cogginsResult ?? "" },
                set: { viewModel.onCogginsResultChange($0) }
            ))
            .textCase(nil)

            TextField("Expiry date (YYYY-MM-DD)", text: Binding(
                get: { form.cogginsExpiryDate ?? "" },
                set: { viewModel.onCogginsExpiryDateChange($0) }
            ))
            .keyboardType(.numbersAndPunctuation)
            .textCase(nil)
        } header: {
            sectionHeader("Coggins")
        }
    }

    private func ownerSection(_ form: PatientFormState) -> some View {
        Section {
            Picker("Owner", selection: Binding(
                get: { form.ownerId.map { $0.int64Value } },
                set: { viewModel.onOwnerChange($0) }
            )) {
                Text("None").tag(Int64?.none)
                ForEach(viewModel.state.owners, id: \.id) { owner in
                    Text(owner.name).tag(Int64?.some(owner.id))
                }
            }
            .pickerStyle(.menu)
        } header: {
            sectionHeader("Owner")
        }
    }

    private func notesSection(_ form: PatientFormState) -> some View {
        Section {
            TextField("Notes", text: Binding(
                get: { form.notes ?? "" },
                set: { viewModel.onNotesChange($0) }
            ), axis: .vertical)
            .lineLimit(3...6)
            .textCase(nil)
        } header: {
            sectionHeader("Notes")
        }
    }

    // MARK: - States

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
            Text("Loading patient…")
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
            Text("Patient not found")
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
