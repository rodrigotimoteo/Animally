import SwiftUI
import Shared

struct IcsiEditView: View {
    @StateObject private var viewModel: IcsiEditViewModel
    @Environment(\.dismiss) private var dismiss

    init(patientId: Int64, icsiId: Int64?) {
        _viewModel = StateObject(wrappedValue: IcsiEditViewModel(patientId: patientId, icsiId: icsiId))
    }

    var body: some View {
        Group {
            if viewModel.state.form?.isLoading == true {
                RecordFormStyle.loadingView(subject: "ICSI record")
            } else if let form = viewModel.state.form {
                formView(form)
            } else {
                RecordFormStyle.notFoundView(subject: "ICSI record")
            }
        }
        .navigationTitle(viewModel.form?.isEditing == true ? "Edit ICSI" : "New ICSI")
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

    private func formView(_ form: IcsiFormState) -> some View {
        List {
            Section {
                RecordFormStyle.dateField("YYYY-MM-DD", value: form.date) {
                    viewModel.onDateChange($0)
                }

                if let dateError = form.dateError {
                    RecordFormStyle.errorText(dateError)
                }

                TextField("Follicles recovered", text: Binding(
                    get: { form.folliclesRecovered },
                    set: { viewModel.onFolliclesRecoveredChange($0) }
                ))
                .keyboardType(.numberPad)
                .textCase(nil)

                RecordFormStyle.textField("Veterinarian", value: form.vetName) {
                    viewModel.onVetNameChange($0)
                }
            } header: {
                RecordFormStyle.sectionHeader("Procedure")
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
final class IcsiEditViewModel: RecordFormViewModel<IcsiEditStoreState> {
    private let store: IcsiEditStore

    init(patientId: Int64, icsiId: Int64?) {
        let store = RecordStores.icsiEditStore(patientId: patientId, icsiId: icsiId)
        self.store = store
        super.init(
            initial: store.state.current,
            subscribe: { store.state.subscribe(onEach: $0) },
            isSaving: { $0.form?.isSaving == true },
            hasError: { $0.form?.dateError != nil }
        )
    }

    var form: IcsiFormState? { state.form }

    func onDateChange(_ value: String) { store.onDateChange(value: value) }
    func onFolliclesRecoveredChange(_ value: String) { store.onFolliclesRecoveredChange(value: value) }
    func onVetNameChange(_ value: String) { store.onVetNameChange(value: value) }
    func onNotesChange(_ value: String) { store.onNotesChange(value: value) }
    func save() { store.save() }
    func dismissError() { store.dismissError() }
}
