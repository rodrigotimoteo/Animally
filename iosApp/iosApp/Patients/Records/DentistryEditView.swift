import SwiftUI
import Shared

struct DentistryEditView: View {
    @StateObject private var viewModel: DentistryEditViewModel
    @Environment(\.dismiss) private var dismiss

    init(patientId: Int64, dentistryId: Int64?) {
        _viewModel = StateObject(wrappedValue: DentistryEditViewModel(patientId: patientId, dentistryId: dentistryId))
    }

    var body: some View {
        Group {
            if viewModel.state.form?.isLoading == true {
                RecordFormStyle.loadingView(subject: "dentistry record")
            } else if let form = viewModel.state.form {
                formView(form)
            } else {
                RecordFormStyle.notFoundView(subject: "Dentistry record")
            }
        }
        .navigationTitle(viewModel.form?.isEditing == true ? "Edit Dentistry" : "New Dentistry")
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
            if let errorMessage = viewModel.state.form?.dateError ?? viewModel.state.form?.nextDueDateError {
                RecordFormStyle.errorBanner(message: errorMessage) {
                    viewModel.dismissError()
                }
            }
        }
        .onAppear {
            viewModel.onSaved = { dismiss() }
        }
    }

    private func formView(_ form: DentistryFormState) -> some View {
        List {
            Section {
                RecordFormStyle.dateField("YYYY-MM-DD", value: form.date) {
                    viewModel.onDateChange($0)
                }

                if let dateError = form.dateError {
                    RecordFormStyle.errorText(dateError)
                }

                RecordFormStyle.textField("Veterinarian", value: form.vetName) {
                    viewModel.onVetNameChange($0)
                }
            } header: {
                RecordFormStyle.sectionHeader("Exam")
            }

            Section {
                TextField("Findings", text: Binding(
                    get: { form.findings ?? "" },
                    set: { viewModel.onFindingsChange($0) }
                ), axis: .vertical)
                .lineLimit(3...6)
                .textCase(nil)

                TextField("Treatment", text: Binding(
                    get: { form.treatment ?? "" },
                    set: { viewModel.onTreatmentChange($0) }
                ), axis: .vertical)
                .lineLimit(3...6)
                .textCase(nil)
            } header: {
                RecordFormStyle.sectionHeader("Findings & Treatment")
            }

            Section {
                RecordFormStyle.dateField("YYYY-MM-DD", value: form.nextDueDate ?? "") {
                    viewModel.onNextDueDateChange($0)
                }

                if let nextDueDateError = form.nextDueDateError {
                    RecordFormStyle.errorText(nextDueDateError)
                }
            } header: {
                RecordFormStyle.sectionHeader("Next Due")
            }

            Section {
                RecordFormStyle.notesField(value: form.notes) {
                    viewModel.onNotesChange($0)
                }
            } header: {
                RecordFormStyle.sectionHeader("Notes")
            }
        }
    }
}

@MainActor
final class DentistryEditViewModel: RecordFormViewModel<DentistryEditStoreState> {
    private let store: DentistryEditStore

    init(patientId: Int64, dentistryId: Int64?) {
        let store = IosEditStores.shared.dentistryEditStore(
            patientId: patientId,
            dentistryId: dentistryId.map { KotlinLong(longLong: $0) }
        )
        self.store = store
        super.init(
            initial: store.state.current,
            subscribe: { store.state.subscribe(onEach: $0) },
            isSaving: { $0.form?.isSaving == true },
            hasError: { $0.form?.dateError != nil || $0.form?.nextDueDateError != nil }
        )
    }

    var form: DentistryFormState? { state.form }

    func onDateChange(_ value: String) { store.onDateChange(date: value) }
    func onFindingsChange(_ value: String) { store.onFindingsChange(findings: value) }
    func onTreatmentChange(_ value: String) { store.onTreatmentChange(treatment: value) }
    func onNextDueDateChange(_ value: String) { store.onNextDueDateChange(nextDueDate: value) }
    func onVetNameChange(_ value: String) { store.onVetNameChange(vetName: value) }
    func onNotesChange(_ value: String) { store.onNotesChange(notes: value) }
    func save() { store.save() }
    func dismissError() { store.dismissError() }
}
