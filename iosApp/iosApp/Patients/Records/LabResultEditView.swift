import SwiftUI
import Shared

struct LabResultEditView: View {
    @StateObject private var viewModel: LabResultEditViewModel
    @Environment(\.dismiss) private var dismiss

    init(patientId: Int64, labResultId: Int64?) {
        _viewModel = StateObject(wrappedValue: LabResultEditViewModel(patientId: patientId, labResultId: labResultId))
    }

    var body: some View {
        Group {
            if viewModel.state.form?.isLoading == true {
                RecordFormStyle.loadingView(subject: "lab result")
            } else if let form = viewModel.state.form {
                formView(form)
            } else {
                RecordFormStyle.notFoundView(subject: "Lab result")
            }
        }
        .navigationTitle(viewModel.form?.isEditing == true ? "Edit Lab Result" : "New Lab Result")
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
            if let errorMessage = viewModel.state.form?.testTypeError ?? viewModel.state.form?.dateError {
                RecordFormStyle.errorBanner(message: errorMessage) {
                    viewModel.dismissError()
                }
            }
        }
        .onAppear {
            viewModel.onSaved = { dismiss() }
        }
    }

    private func formView(_ form: LabResultFormState) -> some View {
        List {
            Section {
                TextField("Test type", text: Binding(
                    get: { form.testType },
                    set: { viewModel.onTestTypeChange($0) }
                ))
                .textCase(nil)

                if let testTypeError = form.testTypeError {
                    RecordFormStyle.errorText(testTypeError)
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
                RecordFormStyle.sectionHeader("Test")
            }

            Section {
                TextField("Results", text: Binding(
                    get: { form.results ?? "" },
                    set: { viewModel.onResultsChange($0) }
                ), axis: .vertical)
                .lineLimit(3...8)
                .textCase(nil)

                RecordFormStyle.textField("Normal range", value: form.normalRange) {
                    viewModel.onNormalRangeChange($0)
                }
            } header: {
                RecordFormStyle.sectionHeader("Results")
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
final class LabResultEditViewModel: RecordFormViewModel<LabResultEditStoreState> {
    private let store: LabResultEditStore

    init(patientId: Int64, labResultId: Int64?) {
        let store = RecordStores.labResultEditStore(patientId: patientId, labResultId: labResultId)
        self.store = store
        super.init(
            initial: store.state.current,
            subscribe: { store.state.subscribe(onEach: $0) },
            isSaving: { $0.form?.isSaving == true },
            hasError: { $0.form?.testTypeError != nil || $0.form?.dateError != nil }
        )
    }

    var form: LabResultFormState? { state.form }

    func onTestTypeChange(_ value: String) { store.onTestTypeChange(value: value) }
    func onDateChange(_ value: String) { store.onDateChange(value: value) }
    func onResultsChange(_ value: String) { store.onResultsChange(value: value) }
    func onNormalRangeChange(_ value: String) { store.onNormalRangeChange(value: value) }
    func onVetNameChange(_ value: String) { store.onVetNameChange(value: value) }
    func onNotesChange(_ value: String) { store.onNotesChange(value: value) }
    func save() { store.save() }
    func dismissError() { store.dismissError() }
}
