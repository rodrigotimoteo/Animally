import SwiftUI
import Shared

struct EmbryoTransferEditView: View {
    @StateObject private var viewModel: EmbryoTransferEditViewModel
    @Environment(\.dismiss) private var dismiss

    init(patientId: Int64, embryoTransferId: Int64?) {
        _viewModel = StateObject(
            wrappedValue: EmbryoTransferEditViewModel(patientId: patientId, embryoTransferId: embryoTransferId)
        )
    }

    var body: some View {
        Group {
            if viewModel.state.form?.isLoading == true {
                RecordFormStyle.loadingView(subject: "embryo transfer")
            } else if let form = viewModel.state.form {
                formView(form)
            } else {
                RecordFormStyle.notFoundView(subject: "Embryo transfer")
            }
        }
        .navigationTitle(viewModel.form?.isEditing == true ? "Edit Embryo Transfer" : "New Embryo Transfer")
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

    private func formView(_ form: EmbryoTransferFormState) -> some View {
        List {
            Section {
                RecordFormStyle.dateField("YYYY-MM-DD", value: form.date) {
                    viewModel.onDateChange($0)
                }

                if let dateError = form.dateError {
                    RecordFormStyle.errorText(dateError)
                }

                TextField("Embryos collected", text: Binding(
                    get: { form.embryoCount },
                    set: { viewModel.onEmbryoCountChange($0) }
                ))
                .keyboardType(.numberPad)
                .textCase(nil)

                RecordFormStyle.textField("Veterinarian", value: form.vetName) {
                    viewModel.onVetNameChange($0)
                }
            } header: {
                RecordFormStyle.sectionHeader("Collection")
            }

            Section {
                TextField("Recipient mares", text: Binding(
                    get: { form.recipientMares ?? "" },
                    set: { viewModel.onRecipientMaresChange($0) }
                ), axis: .vertical)
                .lineLimit(2...4)
                .textCase(nil)
            } header: {
                RecordFormStyle.sectionHeader("Recipients")
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
final class EmbryoTransferEditViewModel: RecordFormViewModel<EmbryoTransferEditStoreState> {
    private let store: EmbryoTransferEditStore

    init(patientId: Int64, embryoTransferId: Int64?) {
        let store = RecordStores.embryoTransferEditStore(patientId: patientId, embryoTransferId: embryoTransferId)
        self.store = store
        super.init(
            initial: store.state.current,
            subscribe: { store.state.subscribe(onEach: $0) },
            isSaving: { $0.form?.isSaving == true },
            hasError: { $0.form?.dateError != nil }
        )
    }

    var form: EmbryoTransferFormState? { state.form }

    func onDateChange(_ value: String) { store.onDateChange(value: value) }
    func onEmbryoCountChange(_ value: String) { store.onEmbryoCountChange(value: value) }
    func onRecipientMaresChange(_ value: String) { store.onRecipientMaresChange(value: value) }
    func onVetNameChange(_ value: String) { store.onVetNameChange(value: value) }
    func onNotesChange(_ value: String) { store.onNotesChange(value: value) }
    func save() { store.save() }
    func dismissError() { store.dismissError() }
}
