import SwiftUI
import Shared

struct FarrierVisitEditView: View {
    @StateObject private var viewModel: FarrierVisitEditViewModel
    @Environment(\.dismiss) private var dismiss

    init(patientId: Int64, farrierVisitId: Int64?) {
        _viewModel = StateObject(wrappedValue: FarrierVisitEditViewModel(patientId: patientId, farrierVisitId: farrierVisitId))
    }

    var body: some View {
        Group {
            if viewModel.state.form?.isLoading == true {
                RecordFormStyle.loadingView(subject: "farrier visit")
            } else if let form = viewModel.state.form {
                formView(form)
            } else {
                RecordFormStyle.notFoundView(subject: "Farrier visit")
            }
        }
        .navigationTitle(viewModel.form?.isEditing == true ? "Edit Farrier Visit" : "New Farrier Visit")
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
            if let errorMessage = viewModel.state.form?.dateError ?? viewModel.state.form?.nextDueDateError {
                RecordFormStyle.errorBanner(message: errorMessage) {
                    viewModel.dismissError()
                }
            }
        }
        .onAppear {
            viewModel.onSaved = { dismiss() }
        }
    }

    private let trimOrShoeOptions = ["Trim", "Full set", "Fronts only", "Reset", "Unknown"]

    private func formView(_ form: FarrierVisitFormState) -> some View {
        List {
            Section {
                RecordFormStyle.dateField("YYYY-MM-DD", value: form.date) {
                    viewModel.onDateChange($0)
                }

                if let dateError = form.dateError {
                    RecordFormStyle.errorText(dateError)
                }

                RecordFormStyle.textField("Farrier", value: form.farrier) {
                    viewModel.onFarrierChange($0)
                }
            } header: {
                RecordFormStyle.sectionHeader("Visit")
            }

            Section {
                Picker("Work", selection: Binding(
                    get: { form.trimOrShoe },
                    set: { viewModel.onTrimOrShoeChange($0) }
                )) {
                    Text("None").tag(String?.none)
                    ForEach(trimOrShoeOptions, id: \.self) { option in
                        Text(option).tag(String?.some(option))
                    }
                }
                .pickerStyle(.menu)

                RecordFormStyle.textField("Shoe type", value: form.shoeType) {
                    viewModel.onShoeTypeChange($0)
                }

                TextField("Findings", text: Binding(
                    get: { form.findings ?? "" },
                    set: { viewModel.onFindingsChange($0) }
                ), axis: .vertical)
                .lineLimit(3...6)
                .textCase(nil)
            } header: {
                RecordFormStyle.sectionHeader("Work Performed")
            }

            Section {
                RecordFormStyle.dateField("YYYY-MM-DD", value: form.nextDueDate ?? "") {
                    viewModel.onNextDueDateChange($0)
                }

                if let nextDueDateError = form.nextDueDateError {
                    RecordFormStyle.errorText(nextDueDateError)
                }
            } header: {
                RecordFormStyle.sectionHeader("Next Due")
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
final class FarrierVisitEditViewModel: RecordFormViewModel<FarrierVisitEditStoreState> {
    private let store: FarrierVisitEditStore

    init(patientId: Int64, farrierVisitId: Int64?) {
        let store = IosEditStores.shared.farrierVisitEditStore(
            patientId: patientId,
            farrierVisitId: farrierVisitId.map { KotlinLong(longLong: $0) }
        )
        self.store = store
        super.init(
            initial: store.state.current,
            subscribe: { store.state.subscribe(onEach: $0) },
            isSaving: { $0.form?.isSaving == true },
            hasError: { $0.form?.dateError != nil || $0.form?.nextDueDateError != nil }
        )
    }

    var form: FarrierVisitFormState? { state.form }

    func onDateChange(_ value: String) { store.onDateChange(date: value) }
    func onTrimOrShoeChange(_ value: String?) { store.onTrimOrShoeChange(trimOrShoe: value ?? "") }
    func onShoeTypeChange(_ value: String) { store.onShoeTypeChange(shoeType: value) }
    func onFindingsChange(_ value: String) { store.onFindingsChange(findings: value) }
    func onNextDueDateChange(_ value: String) { store.onNextDueDateChange(nextDueDate: value) }
    func onFarrierChange(_ value: String) { store.onFarrierChange(farrier: value) }
    func onNotesChange(_ value: String) { store.onNotesChange(notes: value) }
    func save() { store.save() }
    func dismissError() { store.dismissError() }
}
