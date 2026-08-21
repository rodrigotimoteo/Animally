import SwiftUI
import Shared

struct SurgeryEditView: View {
    @StateObject private var viewModel: SurgeryEditViewModel
    @Environment(\.dismiss) private var dismiss

    init(patientId: Int64, surgeryId: Int64?) {
        _viewModel = StateObject(wrappedValue: SurgeryEditViewModel(patientId: patientId, surgeryId: surgeryId))
    }

    var body: some View {
        Group {
            if viewModel.state.form?.isLoading == true {
                RecordFormStyle.loadingView(subject: "surgery")
            } else if let form = viewModel.state.form {
                formView(form)
            } else {
                RecordFormStyle.notFoundView(subject: "Surgery")
            }
        }
        .navigationTitle(viewModel.form?.isEditing == true ? "Edit Surgery" : "New Surgery")
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
            if let errorMessage = viewModel.state.form?.dateError {
                RecordFormStyle.errorBanner(message: errorMessage) {
                    viewModel.dismissError()
                }
            }
        }
        .onAppear {
            viewModel.onSaved = { dismiss() }
        }
    }

    private func formView(_ form: SurgeryFormState) -> some View {
        List {
            Section {
                RecordFormStyle.dateField("YYYY-MM-DD", value: form.date) {
                    viewModel.onDateChange($0)
                }

                if let dateError = form.dateError {
                    RecordFormStyle.errorText(dateError)
                }

                RecordFormStyle.textField("Type", value: form.type) {
                    viewModel.onTypeChange($0)
                }

                RecordFormStyle.textField("Surgeon", value: form.surgeon) {
                    viewModel.onSurgeonChange($0)
                }
            } header: {
                RecordFormStyle.sectionHeader("Procedure")
            }

            Section {
                TextField("Description", text: Binding(
                    get: { form.description ?? "" },
                    set: { viewModel.onDescriptionChange($0) }
                ), axis: .vertical)
                .lineLimit(3...6)
                .textCase(nil)

                RecordFormStyle.textField("Anesthesia", value: form.anesthesia) {
                    viewModel.onAnesthesiaChange($0)
                }

                RecordFormStyle.textField("Analgesia", value: form.analgesia) {
                    viewModel.onAnalgesiaChange($0)
                }
            } header: {
                RecordFormStyle.sectionHeader("Details")
            }

            Section {
                TextField("Outcome", text: Binding(
                    get: { form.outcome ?? "" },
                    set: { viewModel.onOutcomeChange($0) }
                ), axis: .vertical)
                .lineLimit(3...6)
                .textCase(nil)

                TextField("Complications", text: Binding(
                    get: { form.complications ?? "" },
                    set: { viewModel.onComplicationsChange($0) }
                ), axis: .vertical)
                .lineLimit(2...5)
                .textCase(nil)

                TextField("Recovery notes", text: Binding(
                    get: { form.recoveryNotes ?? "" },
                    set: { viewModel.onRecoveryNotesChange($0) }
                ), axis: .vertical)
                .lineLimit(2...5)
                .textCase(nil)
            } header: {
                RecordFormStyle.sectionHeader("Outcome & Recovery")
            }
        }
    }
}

@MainActor
final class SurgeryEditViewModel: RecordFormViewModel<SurgeryEditStoreState> {
    private let store: SurgeryEditStore

    init(patientId: Int64, surgeryId: Int64?) {
        let store = RecordStores.surgeryEditStore(patientId: patientId, surgeryId: surgeryId)
        self.store = store
        super.init(
            initial: store.state.current,
            subscribe: { store.state.subscribe(onEach: $0) },
            isSaving: { $0.form?.isSaving == true },
            hasError: { $0.form?.dateError != nil }
        )
    }

    var form: SurgeryFormState? { state.form }

    func onDateChange(_ value: String) { store.onDateChange(date: value) }
    func onTypeChange(_ value: String) { store.onTypeChange(value: value) }
    func onDescriptionChange(_ value: String) { store.onDescriptionChange(value: value) }
    func onOutcomeChange(_ value: String) { store.onOutcomeChange(value: value) }
    func onSurgeonChange(_ value: String) { store.onSurgeonChange(value: value) }
    func onAnesthesiaChange(_ value: String) { store.onAnesthesiaChange(value: value) }
    func onAnalgesiaChange(_ value: String) { store.onAnalgesiaChange(value: value) }
    func onComplicationsChange(_ value: String) { store.onComplicationsChange(value: value) }
    func onRecoveryNotesChange(_ value: String) { store.onRecoveryNotesChange(value: value) }
    func save() { store.save() }
    func dismissError() { store.dismissError() }
}
