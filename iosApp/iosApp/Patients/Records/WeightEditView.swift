import SwiftUI
import Shared

struct WeightEditView: View {
    @StateObject private var viewModel: WeightEditViewModel
    @Environment(\.dismiss) private var dismiss

    init(patientId: Int64, weightId: Int64?, prefill: RecordPrefill? = nil) {
        _viewModel = StateObject(wrappedValue: WeightEditViewModel(patientId: patientId, weightId: weightId, prefill: prefill))
    }

    var body: some View {
        Group {
            if viewModel.state.form?.isLoading == true {
                RecordFormStyle.loadingView(subject: "weight entry")
            } else if let form = viewModel.state.form {
                formView(form)
            } else {
                RecordFormStyle.notFoundView(subject: "Weight entry")
            }
        }
        .navigationTitle(viewModel.form?.isEditing == true ? "Edit Weight" : "New Weight")
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
            if let errorMessage = viewModel.state.form?.weightError ?? viewModel.state.form?.dateError {
                RecordFormStyle.errorBanner(message: errorMessage) {
                    viewModel.dismissError()
                }
            }
        }
        .onAppear {
            viewModel.onSaved = { dismiss() }
            viewModel.applyPrefillIfNeeded()
        }
        .onChange(of: viewModel.hasForm) { _ in
            viewModel.applyPrefillIfNeeded()
        }
    }

    private func formView(_ form: WeightFormState) -> some View {
        List {
            Section {
                TextField("Weight (kg)", text: Binding(
                    get: { form.weightKg },
                    set: { viewModel.onWeightKgChange($0) }
                ))
                .keyboardType(.decimalPad)
                .textCase(nil)

                if let weightError = form.weightError {
                    RecordFormStyle.errorText(weightError)
                }

                RecordFormStyle.dateField("YYYY-MM-DD", value: form.date) {
                    viewModel.onDateChange($0)
                }

                if let dateError = form.dateError {
                    RecordFormStyle.errorText(dateError)
                }
            } header: {
                RecordFormStyle.sectionHeader("Measurement")
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
final class WeightEditViewModel: RecordFormViewModel<WeightEditStoreState> {
    private let store: WeightEditStore
    private let prefill: RecordPrefill?
    private var prefillApplied = false

    init(patientId: Int64, weightId: Int64?, prefill: RecordPrefill? = nil) {
        let store = IosEditStores.shared.weightEditStore(
            patientId: patientId,
            weightId: weightId.map { KotlinLong(longLong: $0) }
        )
        self.store = store
        self.prefill = prefill
        super.init(
            initial: store.state.current,
            subscribe: { store.state.subscribe(onEach: $0) },
            isSaving: { $0.form?.isSaving == true },
            hasError: { $0.form?.weightError != nil || $0.form?.dateError != nil }
        )
    }

    var form: WeightFormState? { state.form }

    var hasForm: Bool { state.form != nil }

    /// Applies dictated values once the Kotlin form has loaded. Runs at most
    /// once; user edits afterwards always win.
    func applyPrefillIfNeeded() {
        guard let prefill, !prefillApplied, state.form != nil else { return }
        prefillApplied = true
        if let date = prefill.date { onDateChange(date) }
        if let weightKg = prefill.weightKg { onWeightKgChange(weightKg) }
        if let notes = prefill.notes { onNotesChange(notes) }
    }

    func onWeightKgChange(_ value: String) { store.onWeightKgChange(weightKg: value) }
    func onDateChange(_ value: String) { store.onDateChange(date: value) }
    func onNotesChange(_ value: String) { store.onNotesChange(notes: value) }
    func save() { store.save() }
    func dismissError() { store.dismissError() }
}
