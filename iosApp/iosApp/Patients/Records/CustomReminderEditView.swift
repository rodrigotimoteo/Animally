import SwiftUI
import Shared

struct CustomReminderEditView: View {
    @StateObject private var viewModel: CustomReminderEditViewModel
    @Environment(\.dismiss) private var dismiss

    init(patientId: Int64, customReminderId: Int64?) {
        _viewModel = StateObject(wrappedValue: CustomReminderEditViewModel(patientId: patientId, customReminderId: customReminderId))
    }

    var body: some View {
        Group {
            if viewModel.state.form?.isLoading == true {
                RecordFormStyle.loadingView(subject: "reminder")
            } else if let form = viewModel.state.form {
                formView(form)
            } else {
                RecordFormStyle.notFoundView(subject: "Reminder")
            }
        }
        .navigationTitle(viewModel.form?.isEditing == true ? "Edit Reminder" : "New Reminder")
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
            if let errorMessage = viewModel.state.form?.titleError
                ?? viewModel.state.form?.dueDateError
                ?? viewModel.state.form?.linkedRecordIdError {
                RecordFormStyle.errorBanner(message: errorMessage) {
                    viewModel.dismissError()
                }
            }
        }
        .onAppear {
            viewModel.onSaved = { dismiss() }
        }
    }

    private func formView(_ form: CustomReminderFormState) -> some View {
        List {
            Section {
                TextField("Title", text: Binding(
                    get: { form.title },
                    set: { viewModel.onTitleChange($0) }
                ))
                .textCase(nil)

                if let titleError = form.titleError {
                    RecordFormStyle.errorText(titleError)
                }

                RecordFormStyle.dateField("YYYY-MM-DD", value: form.dueDate) {
                    viewModel.onDueDateChange($0)
                }

                if let dueDateError = form.dueDateError {
                    RecordFormStyle.errorText(dueDateError)
                }
            } header: {
                RecordFormStyle.sectionHeader("Reminder")
            }

            Section {
                RecordFormStyle.textField("Linked record type", value: form.linkedRecordType) {
                    viewModel.onLinkedRecordTypeChange($0)
                }

                TextField("Linked record ID", text: Binding(
                    get: { form.linkedRecordId ?? "" },
                    set: { viewModel.onLinkedRecordIdChange($0) }
                ))
                .keyboardType(.numberPad)
                .textCase(nil)

                if let linkedRecordIdError = form.linkedRecordIdError {
                    RecordFormStyle.errorText(linkedRecordIdError)
                }
            } header: {
                RecordFormStyle.sectionHeader("Linked Record (optional)")
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
final class CustomReminderEditViewModel: RecordFormViewModel<CustomReminderEditStoreState> {
    private let store: CustomReminderEditStore

    init(patientId: Int64, customReminderId: Int64?) {
        let store = RecordStores.customReminderEditStore(patientId: patientId, customReminderId: customReminderId)
        self.store = store
        super.init(
            initial: store.state.current,
            subscribe: { store.state.subscribe(onEach: $0) },
            isSaving: { $0.form?.isSaving == true },
            hasError: { $0.form?.titleError != nil || $0.form?.dueDateError != nil || $0.form?.linkedRecordIdError != nil }
        )
    }

    var form: CustomReminderFormState? { state.form }

    func onTitleChange(_ value: String) { store.onTitleChange(value: value) }
    func onDueDateChange(_ value: String) { store.onDueDateChange(value: value) }
    func onLinkedRecordTypeChange(_ value: String) { store.onLinkedRecordTypeChange(value: value) }
    func onLinkedRecordIdChange(_ value: String) { store.onLinkedRecordIdChange(value: value) }
    func onNotesChange(_ value: String) { store.onNotesChange(value: value) }
    func save() { store.save() }
    func dismissError() { store.dismissError() }
}
