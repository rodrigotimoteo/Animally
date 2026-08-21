import SwiftUI
import Shared

struct DewormingEditView: View {
    @StateObject private var viewModel: DewormingEditViewModel
    @Environment(\.dismiss) private var dismiss

    init(patientId: Int64, dewormingId: Int64?) {
        _viewModel = StateObject(wrappedValue: DewormingEditViewModel(patientId: patientId, dewormingId: dewormingId))
    }

    var body: some View {
        Group {
            if viewModel.state.form?.isLoading == true {
                RecordFormStyle.loadingView(subject: "deworming record")
            } else if let form = viewModel.state.form {
                formView(form)
            } else {
                RecordFormStyle.notFoundView(subject: "Deworming record")
            }
        }
        .navigationTitle(viewModel.form?.isEditing == true ? "Edit Deworming" : "New Deworming")
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
            if let errorMessage = viewModel.state.form?.productError
                ?? viewModel.state.form?.dateError
                ?? viewModel.state.form?.nextDueDateError {
                RecordFormStyle.errorBanner(message: errorMessage) {
                    viewModel.dismissError()
                }
            }
        }
        .onAppear {
            viewModel.onSaved = { dismiss() }
        }
    }

    private func formView(_ form: DewormingFormState) -> some View {
        List {
            Section {
                TextField("Product", text: Binding(
                    get: { form.product },
                    set: { viewModel.onProductChange($0) }
                ))
                .textCase(nil)

                if let productError = form.productError {
                    RecordFormStyle.errorText(productError)
                }

                RecordFormStyle.dateField("YYYY-MM-DD", value: form.dateAdministered) {
                    viewModel.onDateAdministeredChange($0)
                }

                if let dateError = form.dateError {
                    RecordFormStyle.errorText(dateError)
                }

                RecordFormStyle.textField("Dose", value: form.dose) {
                    viewModel.onDoseChange($0)
                }

                RecordFormStyle.textField("Veterinarian", value: form.vetName) {
                    viewModel.onVetNameChange($0)
                }
            } header: {
                RecordFormStyle.sectionHeader("Treatment")
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
final class DewormingEditViewModel: RecordFormViewModel<DewormingEditStoreState> {
    private let store: DewormingEditStore

    init(patientId: Int64, dewormingId: Int64?) {
        let store = IosEditStores.shared.dewormingEditStore(
            patientId: patientId,
            dewormingId: dewormingId.map { KotlinLong(longLong: $0) }
        )
        self.store = store
        super.init(
            initial: store.state.current,
            subscribe: { store.state.subscribe(onEach: $0) },
            isSaving: { $0.form?.isSaving == true },
            hasError: { $0.form?.productError != nil || $0.form?.dateError != nil || $0.form?.nextDueDateError != nil }
        )
    }

    var form: DewormingFormState? { state.form }

    func onProductChange(_ value: String) { store.onProductChange(product: value) }
    func onDateAdministeredChange(_ value: String) { store.onDateAdministeredChange(dateAdministered: value) }
    func onNextDueDateChange(_ value: String) { store.onNextDueDateChange(nextDueDate: value) }
    func onDoseChange(_ value: String) { store.onDoseChange(dose: value) }
    func onVetNameChange(_ value: String) { store.onVetNameChange(vetName: value) }
    func onNotesChange(_ value: String) { store.onNotesChange(notes: value) }
    func save() { store.save() }
    func dismissError() { store.dismissError() }
}
