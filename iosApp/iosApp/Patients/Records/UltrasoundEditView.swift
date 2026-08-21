import SwiftUI
import Shared

struct UltrasoundEditView: View {
    @StateObject private var viewModel: UltrasoundEditViewModel
    @Environment(\.dismiss) private var dismiss

    init(patientId: Int64, ultrasoundId: Int64?) {
        _viewModel = StateObject(wrappedValue: UltrasoundEditViewModel(patientId: patientId, ultrasoundId: ultrasoundId))
    }

    var body: some View {
        Group {
            if viewModel.state.form?.isLoading == true {
                RecordFormStyle.loadingView(subject: "ultrasound")
            } else if let form = viewModel.state.form {
                formView(form)
            } else {
                RecordFormStyle.notFoundView(subject: "Ultrasound")
            }
        }
        .navigationTitle(viewModel.form?.isEditing == true ? "Edit Ultrasound" : "New Ultrasound")
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
            if let errorMessage = viewModel.state.form?.dateError ?? viewModel.state.form?.follicleSizeMmError {
                RecordFormStyle.errorBanner(message: errorMessage) {
                    viewModel.dismissError()
                }
            }
        }
        .onAppear {
            viewModel.onSaved = { dismiss() }
        }
    }

    private func formView(_ form: UltrasoundFormState) -> some View {
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
                RecordFormStyle.textField("Ovary status", value: form.ovaryStatus) {
                    viewModel.onOvaryStatusChange($0)
                }

                RecordFormStyle.textField("Uterine status", value: form.uterineStatus) {
                    viewModel.onUterineStatusChange($0)
                }

                TextField("Follicle size (mm)", text: Binding(
                    get: { form.follicleSizeMm ?? "" },
                    set: { viewModel.onFollicleSizeMmChange($0) }
                ))
                .keyboardType(.decimalPad)
                .textCase(nil)

                if let follicleSizeMmError = form.follicleSizeMmError {
                    RecordFormStyle.errorText(follicleSizeMmError)
                }
            } header: {
                RecordFormStyle.sectionHeader("Reproductive Findings")
            }

            Section {
                TextField("Findings", text: Binding(
                    get: { form.findings ?? "" },
                    set: { viewModel.onFindingsChange($0) }
                ), axis: .vertical)
                .lineLimit(3...6)
                .textCase(nil)

                RecordFormStyle.textField("Image URIs", value: form.imageUris) {
                    viewModel.onImageUrisChange($0)
                }
            } header: {
                RecordFormStyle.sectionHeader("Findings & Images")
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
final class UltrasoundEditViewModel: RecordFormViewModel<UltrasoundEditStoreState> {
    private let store: UltrasoundEditStore

    init(patientId: Int64, ultrasoundId: Int64?) {
        let store = RecordStores.ultrasoundEditStore(patientId: patientId, ultrasoundId: ultrasoundId)
        self.store = store
        super.init(
            initial: store.state.current,
            subscribe: { store.state.subscribe(onEach: $0) },
            isSaving: { $0.form?.isSaving == true },
            hasError: { $0.form?.dateError != nil || $0.form?.follicleSizeMmError != nil }
        )
    }

    var form: UltrasoundFormState? { state.form }

    func onDateChange(_ value: String) { store.onDateChange(date: value) }
    func onOvaryStatusChange(_ value: String) { store.onOvaryStatusChange(value: value) }
    func onUterineStatusChange(_ value: String) { store.onUterineStatusChange(value: value) }
    func onFollicleSizeMmChange(_ value: String) { store.onFollicleSizeMmChange(value: value) }
    func onFindingsChange(_ value: String) { store.onFindingsChange(value: value) }
    func onImageUrisChange(_ value: String) { store.onImageUrisChange(value: value) }
    func onVetNameChange(_ value: String) { store.onVetNameChange(value: value) }
    func onNotesChange(_ value: String) { store.onNotesChange(value: value) }
    func save() { store.save() }
    func dismissError() { store.dismissError() }
}
