import SwiftUI
import Shared

struct MedicationEditView: View {
    @StateObject private var viewModel: MedicationEditViewModel
    @Environment(\.dismiss) private var dismiss

    init(patientId: Int64, medicationId: Int64?, prefill: RecordPrefill? = nil) {
        _viewModel = StateObject(wrappedValue: MedicationEditViewModel(patientId: patientId, medicationId: medicationId, prefill: prefill))
    }

    var body: some View {
        Group {
            if viewModel.state.form?.isLoading == true {
                RecordFormStyle.loadingView(subject: "medication")
            } else if let form = viewModel.state.form {
                formView(form)
            } else {
                RecordFormStyle.notFoundView(subject: "Medication")
            }
        }
        .navigationTitle(viewModel.form?.isEditing == true ? "Edit Medication" : "New Medication")
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
            if let errorMessage = viewModel.state.form?.nameError
                ?? viewModel.state.form?.dosageError
                ?? viewModel.state.form?.startDateError
                ?? viewModel.state.form?.endDateError {
                RecordFormStyle.errorBanner(message: errorMessage) {
                    viewModel.dismissError()
                }
            }
        }
        .onAppear {
            viewModel.onSaved = { dismiss() }
            viewModel.applyPrefillIfNeeded()
        }
        .onChange(of: viewModel.hasForm) { _, _ in
            viewModel.applyPrefillIfNeeded()
        }
    }

    private func formView(_ form: MedicationFormState) -> some View {
        List {
            Section {
                TextField("Name", text: Binding(
                    get: { form.name },
                    set: { viewModel.onNameChange($0) }
                ))
                .textCase(nil)

                if let nameError = form.nameError {
                    RecordFormStyle.errorText(nameError)
                }

                RecordFormStyle.textField("Dosage", value: form.dosage, onChange: { viewModel.onDosageChange($0) })

                if let dosageError = form.dosageError {
                    RecordFormStyle.errorText(dosageError)
                }

                RecordFormStyle.textField("Route", value: form.route) {
                    viewModel.onRouteChange($0)
                }

                RecordFormStyle.textField("Frequency", value: form.frequency) {
                    viewModel.onFrequencyChange($0)
                }

                RecordFormStyle.textField("Prescribed by", value: form.prescribedBy) {
                    viewModel.onPrescribedByChange($0)
                }
            } header: {
                RecordFormStyle.sectionHeader("Medication")
            }

            Section {
                RecordFormStyle.dateField("YYYY-MM-DD", value: form.startDate ?? "") {
                    viewModel.onStartDateChange($0)
                }

                if let startDateError = form.startDateError {
                    RecordFormStyle.errorText(startDateError)
                }

                RecordFormStyle.dateField("YYYY-MM-DD", value: form.endDate ?? "") {
                    viewModel.onEndDateChange($0)
                }

                if let endDateError = form.endDateError {
                    RecordFormStyle.errorText(endDateError)
                }
            } header: {
                RecordFormStyle.sectionHeader("Schedule")
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
final class MedicationEditViewModel: RecordFormViewModel<MedicationEditStoreState> {
    private let store: MedicationEditStore
    private let prefill: RecordPrefill?
    private var prefillApplied = false

    init(patientId: Int64, medicationId: Int64?, prefill: RecordPrefill? = nil) {
        let store = RecordStores.medicationEditStore(patientId: patientId, medicationId: medicationId)
        self.store = store
        self.prefill = prefill
        super.init(
            initial: store.state.current,
            subscribe: { store.state.subscribe(onEach: $0) },
            isSaving: { $0.form?.isSaving == true },
            hasError: { $0.form?.nameError != nil || $0.form?.dosageError != nil || $0.form?.startDateError != nil || $0.form?.endDateError != nil }
        )
    }

    var form: MedicationFormState? { state.form }

    var hasForm: Bool { state.form != nil }

    /// Applies dictated values once the Kotlin form has loaded. Runs at most
    /// once; user edits afterwards always win.
    func applyPrefillIfNeeded() {
        guard let prefill, !prefillApplied, state.form != nil else { return }
        prefillApplied = true
        if let medicationName = prefill.medicationName { onNameChange(medicationName) }
        if let medicationDosage = prefill.medicationDosage { onDosageChange(medicationDosage) }
        if let date = prefill.date { onStartDateChange(date) }
        if let notes = prefill.notes { onNotesChange(notes) }
    }

    func onNameChange(_ value: String) { store.onNameChange(value: value) }
    func onDosageChange(_ value: String) { store.onDosageChange(value: value) }
    func onRouteChange(_ value: String) { store.onRouteChange(value: value) }
    func onFrequencyChange(_ value: String) { store.onFrequencyChange(value: value) }
    func onStartDateChange(_ value: String) { store.onStartDateChange(value: value) }
    func onEndDateChange(_ value: String) { store.onEndDateChange(value: value) }
    func onPrescribedByChange(_ value: String) { store.onPrescribedByChange(value: value) }
    func onNotesChange(_ value: String) { store.onNotesChange(value: value) }
    func save() { store.save() }
    func dismissError() { store.dismissError() }
}
