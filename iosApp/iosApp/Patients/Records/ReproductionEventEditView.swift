import SwiftUI
import Shared

struct ReproductionEventEditView: View {
    @StateObject private var viewModel: ReproductionEventEditViewModel
    @Environment(\.dismiss) private var dismiss

    private let eventTypes = ["Heat", "Breeding", "Pregnancy Check", "Foaling", "Initial Exam"]
    private let breedingTypes = [
        ("Natural cover", "NATURAL_COVER"),
        ("Artificial insemination", "ARTIFICIAL_INSEMINATION"),
        ("Embryo recipient", "EMBRYO_RECIPIENT"),
    ]

    init(patientId: Int64, reproductionEventId: Int64?) {
        _viewModel = StateObject(wrappedValue: ReproductionEventEditViewModel(patientId: patientId, reproductionEventId: reproductionEventId))
    }

    var body: some View {
        Group {
            if viewModel.state.form?.isLoading == true {
                RecordFormStyle.loadingView(subject: "reproduction event")
            } else if let form = viewModel.state.form {
                formView(form)
            } else {
                RecordFormStyle.notFoundView(subject: "Reproduction event")
            }
        }
        .navigationTitle(viewModel.form?.isEditing == true ? "Edit Event" : "New Event")
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
            if let errorMessage = viewModel.state.form?.eventTypeError ?? viewModel.state.form?.dateError {
                RecordFormStyle.errorBanner(message: errorMessage) {
                    viewModel.dismissError()
                }
            }
        }
        .onAppear {
            viewModel.onSaved = { dismiss() }
        }
    }

    private func formView(_ form: ReproductionEventFormState) -> some View {
        List {
            Section {
                Picker("Event type", selection: Binding(
                    get: { form.eventType.isEmpty ? nil : form.eventType },
                    set: { viewModel.onEventTypeChange($0 ?? "") }
                )) {
                    Text("None").tag(String?.none)
                    ForEach(eventTypes, id: \.self) { type in
                        Text(type).tag(String?.some(type))
                    }
                }
                .pickerStyle(.menu)

                if let eventTypeError = form.eventTypeError {
                    RecordFormStyle.errorText(eventTypeError)
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
                RecordFormStyle.sectionHeader("Event")
            }

            Section {
                TextField("Details", text: Binding(
                    get: { form.details ?? "" },
                    set: { viewModel.onDetailsChange($0) }
                ), axis: .vertical)
                .lineLimit(3...6)
                .textCase(nil)

                RecordFormStyle.notesField(value: form.notes) {
                    viewModel.onNotesChange($0)
                }
            } header: {
                RecordFormStyle.sectionHeader("Details & Notes")
            }

            if form.eventType == "Breeding" {
                Section {
                    RecordFormStyle.textField("Stallion", value: form.stallionName) {
                        viewModel.onStallionNameChange($0)
                    }

                    Picker("Breeding type", selection: Binding(
                        get: { form.breedingType },
                        set: { viewModel.onBreedingTypeChange($0 ?? "") }
                    )) {
                        Text("None").tag(String?.none)
                        ForEach(breedingTypes, id: \.1) { option in
                            Text(option.0).tag(String?.some(option.1))
                        }
                    }
                    .pickerStyle(.menu)
                } header: {
                    RecordFormStyle.sectionHeader("Breeding")
                }
            }

            if form.eventType == "Initial Exam" {
                Section {
                    TextField("Findings", text: Binding(
                        get: { form.initialExamFindings ?? "" },
                        set: { viewModel.onInitialExamFindingsChange($0) }
                    ), axis: .vertical)
                    .lineLimit(3...6)
                    .textCase(nil)
                } header: {
                    RecordFormStyle.sectionHeader("Initial Exam Findings")
                }
            }
        }
    }
}

@MainActor
final class ReproductionEventEditViewModel: RecordFormViewModel<ReproductionEventEditStoreState> {
    private let store: ReproductionEventEditStore

    init(patientId: Int64, reproductionEventId: Int64?) {
        let store = RecordStores.reproductionEventEditStore(patientId: patientId, reproductionEventId: reproductionEventId)
        self.store = store
        super.init(
            initial: store.state.current,
            subscribe: { store.state.subscribe(onEach: $0) },
            isSaving: { $0.form?.isSaving == true },
            hasError: { $0.form?.eventTypeError != nil || $0.form?.dateError != nil }
        )
    }

    var form: ReproductionEventFormState? { state.form }

    func onEventTypeChange(_ value: String) { store.onEventTypeChange(value: value) }
    func onDateChange(_ value: String) { store.onDateChange(date: value) }
    func onDetailsChange(_ value: String) { store.onDetailsChange(value: value) }
    func onInitialExamFindingsChange(_ value: String) { store.onInitialExamFindingsChange(value: value) }
    func onStallionNameChange(_ value: String) { store.onStallionNameChange(value: value) }
    func onBreedingTypeChange(_ value: String) { store.onBreedingTypeChange(value: value) }
    func onVetNameChange(_ value: String) { store.onVetNameChange(value: value) }
    func onNotesChange(_ value: String) { store.onNotesChange(value: value) }
    func save() { store.save() }
    func dismissError() { store.dismissError() }
}
