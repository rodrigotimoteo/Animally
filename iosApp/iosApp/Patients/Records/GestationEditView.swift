import SwiftUI
import Shared

struct GestationEditView: View {
    @StateObject private var viewModel: GestationEditViewModel
    @Environment(\.dismiss) private var dismiss

    init(patientId: Int64, gestationId: Int64?) {
        _viewModel = StateObject(wrappedValue: GestationEditViewModel(patientId: patientId, gestationId: gestationId))
    }

    var body: some View {
        Group {
            if viewModel.state.form?.isLoading == true {
                RecordFormStyle.loadingView(subject: "gestation")
            } else if let form = viewModel.state.form {
                formView(form)
            } else {
                RecordFormStyle.notFoundView(subject: "Gestation")
            }
        }
        .navigationTitle(viewModel.form?.isEditing == true ? "Edit Gestation" : "New Gestation")
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
            if let errorMessage = viewModel.state.form?.breedingDateError
                ?? viewModel.state.form?.statusError
                ?? viewModel.state.form?.fetalCountError
                ?? viewModel.state.form?.lastCheckDateError {
                RecordFormStyle.errorBanner(message: errorMessage) {
                    viewModel.dismissError()
                }
            }
        }
        .onAppear {
            viewModel.onSaved = { dismiss() }
        }
    }

    private func formView(_ form: GestationFormState) -> some View {
        List {
            Section {
                RecordFormStyle.dateField("YYYY-MM-DD", value: form.breedingDate) {
                    viewModel.onBreedingDateChange($0)
                }

                if let breedingDateError = form.breedingDateError {
                    RecordFormStyle.errorText(breedingDateError)
                }

                TextField("Status", text: Binding(
                    get: { form.status },
                    set: { viewModel.onStatusChange($0) }
                ))
                .textCase(nil)

                if let statusError = form.statusError {
                    RecordFormStyle.errorText(statusError)
                }
            } header: {
                RecordFormStyle.sectionHeader("Breeding")
            }

            Section {
                TextField("Fetal count", text: Binding(
                    get: { form.fetalCount ?? "" },
                    set: { viewModel.onFetalCountChange($0) }
                ))
                .keyboardType(.numberPad)
                .textCase(nil)

                if let fetalCountError = form.fetalCountError {
                    RecordFormStyle.errorText(fetalCountError)
                }

                RecordFormStyle.dateField("YYYY-MM-DD", value: form.lastCheckDate ?? "") {
                    viewModel.onLastCheckDateChange($0)
                }

                if let lastCheckDateError = form.lastCheckDateError {
                    RecordFormStyle.errorText(lastCheckDateError)
                }
            } header: {
                RecordFormStyle.sectionHeader("Pregnancy Check")
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
final class GestationEditViewModel: RecordFormViewModel<GestationEditStoreState> {
    private let store: GestationEditStore

    init(patientId: Int64, gestationId: Int64?) {
        let store = RecordStores.gestationEditStore(patientId: patientId, gestationId: gestationId)
        self.store = store
        super.init(
            initial: store.state.current,
            subscribe: { store.state.subscribe(onEach: $0) },
            isSaving: { $0.form?.isSaving == true },
            hasError: { $0.form?.breedingDateError != nil || $0.form?.statusError != nil || $0.form?.fetalCountError != nil || $0.form?.lastCheckDateError != nil }
        )
    }

    var form: GestationFormState? { state.form }

    func onBreedingDateChange(_ value: String) { store.onBreedingDateChange(value: value) }
    func onStatusChange(_ value: String) { store.onStatusChange(value: value) }
    func onFetalCountChange(_ value: String) { store.onFetalCountChange(value: value) }
    func onLastCheckDateChange(_ value: String) { store.onLastCheckDateChange(value: value) }
    func onNotesChange(_ value: String) { store.onNotesChange(value: value) }
    func save() { store.save() }
    func dismissError() { store.dismissError() }
}
