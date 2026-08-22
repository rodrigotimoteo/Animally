import SwiftUI
import Shared

struct ImagingEditView: View {
    @StateObject private var viewModel: ImagingEditViewModel
    @Environment(\.dismiss) private var dismiss

    init(patientId: Int64, imagingId: Int64?) {
        _viewModel = StateObject(wrappedValue: ImagingEditViewModel(patientId: patientId, imagingId: imagingId))
    }

    var body: some View {
        Group {
            if viewModel.state.form?.isLoading == true {
                RecordFormStyle.loadingView(subject: "imaging study")
            } else if let form = viewModel.state.form {
                formView(form)
            } else {
                RecordFormStyle.notFoundView(subject: "Imaging study")
            }
        }
        .navigationTitle(viewModel.form?.isEditing == true ? "Edit Imaging" : "New Imaging")
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
            if let errorMessage = viewModel.state.form?.typeError ?? viewModel.state.form?.dateError {
                RecordFormStyle.errorBanner(message: errorMessage) {
                    viewModel.dismissError()
                }
            }
        }
        .onAppear {
            viewModel.onSaved = { dismiss() }
        }
    }

    private func formView(_ form: ImagingFormState) -> some View {
        List {
            Section {
                TextField("Type", text: Binding(
                    get: { form.type },
                    set: { viewModel.onTypeChange($0) }
                ))
                .textCase(nil)

                if let typeError = form.typeError {
                    RecordFormStyle.errorText(typeError)
                }

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
                RecordFormStyle.sectionHeader("Study")
            }

            Section {
                TextField("Findings", text: Binding(
                    get: { form.findings ?? "" },
                    set: { viewModel.onFindingsChange($0) }
                ), axis: .vertical)
                .lineLimit(3...8)
                .textCase(nil)

                ImageAttachmentField(title: "Imaging Study", imagePath: form.imageUris) {
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
final class ImagingEditViewModel: RecordFormViewModel<ImagingEditStoreState> {
    private let store: ImagingEditStore

    init(patientId: Int64, imagingId: Int64?) {
        let store = RecordStores.imagingEditStore(patientId: patientId, imagingId: imagingId)
        self.store = store
        super.init(
            initial: store.state.current,
            subscribe: { store.state.subscribe(onEach: $0) },
            isSaving: { $0.form?.isSaving == true },
            hasError: { $0.form?.typeError != nil || $0.form?.dateError != nil }
        )
    }

    var form: ImagingFormState? { state.form }

    func onTypeChange(_ value: String) { store.onTypeChange(value: value) }
    func onDateChange(_ value: String) { store.onDateChange(value: value) }
    func onFindingsChange(_ value: String) { store.onFindingsChange(value: value) }
    func onImageUrisChange(_ value: String) { store.onImageUrisChange(value: value) }
    func onVetNameChange(_ value: String) { store.onVetNameChange(value: value) }
    func onNotesChange(_ value: String) { store.onNotesChange(value: value) }
    func save() { store.save() }
    func dismissError() { store.dismissError() }
}
