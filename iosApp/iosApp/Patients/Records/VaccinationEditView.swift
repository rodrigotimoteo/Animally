import SwiftUI
import Shared

struct VaccinationEditView: View {
    @StateObject private var viewModel: VaccinationEditViewModel
    @Environment(\.dismiss) private var dismiss

    init(patientId: Int64, vaccinationId: Int64?) {
        _viewModel = StateObject(wrappedValue: VaccinationEditViewModel(patientId: patientId, vaccinationId: vaccinationId))
    }

    var body: some View {
        Group {
            if viewModel.state.form?.isLoading == true {
                RecordFormStyle.loadingView(subject: "vaccination")
            } else if let form = viewModel.state.form {
                formView(form)
            } else {
                RecordFormStyle.notFoundView(subject: "Vaccination")
            }
        }
        .navigationTitle(viewModel.form?.isEditing == true ? "Edit Vaccination" : "New Vaccination")
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
            if let errorMessage = viewModel.state.form?.vaccineNameError ?? viewModel.state.form?.dateError {
                RecordFormStyle.errorBanner(message: errorMessage) {
                    viewModel.dismissError()
                }
            }
        }
        .onAppear {
            viewModel.onSaved = { dismiss() }
        }
    }

    private func formView(_ form: VaccinationFormState) -> some View {
        List {
            Section {
                TextField("Vaccine name", text: Binding(
                    get: { form.vaccineName },
                    set: { viewModel.onVaccineNameChange($0) }
                ))
                .textCase(nil)

                if let vaccineNameError = form.vaccineNameError {
                    RecordFormStyle.errorText(vaccineNameError)
                }

                RecordFormStyle.dateField("YYYY-MM-DD", value: form.dateAdministered) {
                    viewModel.onDateAdministeredChange($0)
                }

                if let dateError = form.dateError {
                    RecordFormStyle.errorText(dateError)
                }

                RecordFormStyle.textField("Veterinarian", value: form.vetName) {
                    viewModel.onVetNameChange($0)
                }
            } header: {
                RecordFormStyle.sectionHeader("Vaccine")
            }

            Section {
                RecordFormStyle.textField("Batch number", value: form.batchNumber) {
                    viewModel.onBatchNumberChange($0)
                }

                RecordFormStyle.textField("Site", value: form.site) {
                    viewModel.onSiteChange($0)
                }
            } header: {
                RecordFormStyle.sectionHeader("Batch & Site")
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
final class VaccinationEditViewModel: RecordFormViewModel<VaccinationEditStoreState> {
    private let store: VaccinationEditStore

    init(patientId: Int64, vaccinationId: Int64?) {
        let store = IosEditStores.shared.vaccinationEditStore(
            patientId: patientId,
            vaccinationId: vaccinationId.map { KotlinLong(longLong: $0) }
        )
        self.store = store
        super.init(
            initial: store.state.current,
            subscribe: { store.state.subscribe(onEach: $0) },
            isSaving: { $0.form?.isSaving == true },
            hasError: { $0.form?.vaccineNameError != nil || $0.form?.dateError != nil }
        )
    }

    var form: VaccinationFormState? { state.form }

    func onVaccineNameChange(_ value: String) { store.onVaccineNameChange(vaccineName: value) }
    func onDateAdministeredChange(_ value: String) { store.onDateAdministeredChange(dateAdministered: value) }
    func onVetNameChange(_ value: String) { store.onVetNameChange(vetName: value) }
    func onBatchNumberChange(_ value: String) { store.onBatchNumberChange(batchNumber: value) }
    func onSiteChange(_ value: String) { store.onSiteChange(site: value) }
    func onNotesChange(_ value: String) { store.onNotesChange(notes: value) }
    func save() { store.save() }
    func dismissError() { store.dismissError() }
}
