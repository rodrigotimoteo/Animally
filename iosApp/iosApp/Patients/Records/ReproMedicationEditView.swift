import SwiftUI
import Shared

struct ReproMedicationEditView: View {
    @StateObject private var viewModel: ReproMedicationEditViewModel
    @Environment(\.dismiss) private var dismiss

    init(patientId: Int64, reproMedicationId: Int64?) {
        _viewModel = StateObject(wrappedValue: ReproMedicationEditViewModel(patientId: patientId, reproMedicationId: reproMedicationId))
    }

    var body: some View {
        Group {
            if viewModel.state.form?.isLoading == true {
                RecordFormStyle.loadingView(subject: "repro medication")
            } else if let form = viewModel.state.form {
                formView(form)
            } else {
                RecordFormStyle.notFoundView(subject: "Repro medication")
            }
        }
        .navigationTitle(viewModel.form?.isEditing == true ? "Edit Repro Medication" : "New Repro Medication")
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
            if let errorMessage = viewModel.state.form?.medicationError ?? viewModel.state.form?.dateError {
                RecordFormStyle.errorBanner(message: errorMessage) {
                    viewModel.dismissError()
                }
            }
        }
        .onAppear {
            viewModel.onSaved = { dismiss() }
        }
    }

    private func formView(_ form: ReproMedicationFormState) -> some View {
        List {
            Section {
                TextField("Medication", text: Binding(
                    get: { form.medication },
                    set: { viewModel.onMedicationChange($0) }
                ))
                .textCase(nil)

                if let medicationError = form.medicationError {
                    RecordFormStyle.errorText(medicationError)
                }

                RecordFormStyle.dateField("YYYY-MM-DD", value: form.dateAdministered) {
                    viewModel.onDateAdministeredChange($0)
                }

                if let dateError = form.dateError {
                    RecordFormStyle.errorText(dateError)
                }

                RecordFormStyle.textField("Dosage", value: form.dosage) {
                    viewModel.onDosageChange($0)
                }

                RecordFormStyle.textField("Veterinarian", value: form.vetName) {
                    viewModel.onVetNameChange($0)
                }
            } header: {
                RecordFormStyle.sectionHeader("Medication")
            }

            Section {
                TextField("Purpose", text: Binding(
                    get: { form.purpose ?? "" },
                    set: { viewModel.onPurposeChange($0) }
                ), axis: .vertical)
                .lineLimit(2...5)
                .textCase(nil)

                RecordFormStyle.notesField(value: form.notes) {
                    viewModel.onNotesChange($0)
                }
            } header: {
                RecordFormStyle.sectionHeader("Purpose & Notes")
            }
        }
    }
}

@MainActor
final class ReproMedicationEditViewModel: RecordFormViewModel<ReproMedicationEditStoreState> {
    private let store: ReproMedicationEditStore

    init(patientId: Int64, reproMedicationId: Int64?) {
        let store = RecordStores.reproMedicationEditStore(patientId: patientId, reproMedicationId: reproMedicationId)
        self.store = store
        super.init(
            initial: store.state.current,
            subscribe: { store.state.subscribe(onEach: $0) },
            isSaving: { $0.form?.isSaving == true },
            hasError: { $0.form?.medicationError != nil || $0.form?.dateError != nil }
        )
    }

    var form: ReproMedicationFormState? { state.form }

    func onMedicationChange(_ value: String) { store.onMedicationChange(value: value) }
    func onDateAdministeredChange(_ value: String) { store.onDateAdministeredChange(value: value) }
    func onDosageChange(_ value: String) { store.onDosageChange(value: value) }
    func onPurposeChange(_ value: String) { store.onPurposeChange(value: value) }
    func onVetNameChange(_ value: String) { store.onVetNameChange(value: value) }
    func onNotesChange(_ value: String) { store.onNotesChange(value: value) }
    func save() { store.save() }
    func dismissError() { store.dismissError() }
}
