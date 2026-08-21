import SwiftUI
import Shared

struct SubstanceEditView: View {
    @StateObject private var viewModel: SubstanceEditViewModel
    @Environment(\.dismiss) private var dismiss

    init(patientId: Int64, substanceId: Int64?) {
        _viewModel = StateObject(wrappedValue: SubstanceEditViewModel(patientId: patientId, substanceId: substanceId))
    }

    var body: some View {
        Group {
            if viewModel.state.form?.isLoading == true {
                RecordFormStyle.loadingView(subject: "controlled substance")
            } else if let form = viewModel.state.form {
                formView(form)
            } else {
                RecordFormStyle.notFoundView(subject: "Controlled substance")
            }
        }
        .navigationTitle(viewModel.form?.isEditing == true ? "Edit Controlled Substance" : "New Controlled Substance")
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
            if let errorMessage = viewModel.state.form?.drugNameError
                ?? viewModel.state.form?.doseError
                ?? viewModel.state.form?.dateError {
                RecordFormStyle.errorBanner(message: errorMessage) {
                    viewModel.dismissError()
                }
            }
        }
        .onAppear {
            viewModel.onSaved = { dismiss() }
        }
    }

    private func formView(_ form: ControlledSubstanceFormState) -> some View {
        List {
            Section {
                TextField("Drug name", text: Binding(
                    get: { form.drugName },
                    set: { viewModel.onDrugNameChange($0) }
                ))
                .textCase(nil)

                if let drugNameError = form.drugNameError {
                    RecordFormStyle.errorText(drugNameError)
                }

                TextField("Dose", text: Binding(
                    get: { form.dose },
                    set: { viewModel.onDoseChange($0) }
                ))
                .keyboardType(.decimalPad)
                .textCase(nil)

                if let doseError = form.doseError {
                    RecordFormStyle.errorText(doseError)
                }

                RecordFormStyle.textField("Unit", value: form.unit) {
                    viewModel.onUnitChange($0)
                }

                RecordFormStyle.textField("Route", value: form.route) {
                    viewModel.onRouteChange($0)
                }

                RecordFormStyle.dateField("YYYY-MM-DD", value: form.date) {
                    viewModel.onDateChange($0)
                }

                if let dateError = form.dateError {
                    RecordFormStyle.errorText(dateError)
                }
            } header: {
                RecordFormStyle.sectionHeader("Administration")
            }

            Section {
                RecordFormStyle.textField("Administered by", value: form.administeredBy) {
                    viewModel.onAdministeredByChange($0)
                }

                RecordFormStyle.textField("Witness", value: form.witness) {
                    viewModel.onWitnessChange($0)
                }
            } header: {
                RecordFormStyle.sectionHeader("Personnel")
            }

            Section {
                TextField("Reason", text: Binding(
                    get: { form.reason ?? "" },
                    set: { viewModel.onReasonChange($0) }
                ), axis: .vertical)
                .lineLimit(2...5)
                .textCase(nil)

                RecordFormStyle.notesField(value: form.notes) {
                    viewModel.onNotesChange($0)
                }
            } header: {
                RecordFormStyle.sectionHeader("Reason & Notes")
            }
        }
    }
}

@MainActor
final class SubstanceEditViewModel: RecordFormViewModel<SubstanceEditStoreState> {
    private let store: SubstanceEditStore

    init(patientId: Int64, substanceId: Int64?) {
        let store = RecordStores.substanceEditStore(patientId: patientId, substanceId: substanceId)
        self.store = store
        super.init(
            initial: store.state.current,
            subscribe: { store.state.subscribe(onEach: $0) },
            isSaving: { $0.form?.isSaving == true },
            hasError: { $0.form?.drugNameError != nil || $0.form?.doseError != nil || $0.form?.dateError != nil }
        )
    }

    var form: ControlledSubstanceFormState? { state.form }

    func onDrugNameChange(_ value: String) { store.onDrugNameChange(value: value) }
    func onDoseChange(_ value: String) { store.onDoseChange(value: value) }
    func onUnitChange(_ value: String) { store.onUnitChange(value: value) }
    func onRouteChange(_ value: String) { store.onRouteChange(value: value) }
    func onAdministeredByChange(_ value: String) { store.onAdministeredByChange(value: value) }
    func onWitnessChange(_ value: String) { store.onWitnessChange(value: value) }
    func onDateChange(_ value: String) { store.onDateChange(value: value) }
    func onReasonChange(_ value: String) { store.onReasonChange(value: value) }
    func onNotesChange(_ value: String) { store.onNotesChange(value: value) }
    func save() { store.save() }
    func dismissError() { store.dismissError() }
}
