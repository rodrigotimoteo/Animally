import SwiftUI
import Shared

struct LamenessEditView: View {
    @StateObject private var viewModel: LamenessEditViewModel
    @Environment(\.dismiss) private var dismiss

    init(patientId: Int64, lamenessId: Int64?) {
        _viewModel = StateObject(wrappedValue: LamenessEditViewModel(patientId: patientId, lamenessId: lamenessId))
    }

    var body: some View {
        Group {
            if viewModel.state.form?.isLoading == true {
                RecordFormStyle.loadingView(subject: "lameness evaluation")
            } else if let form = viewModel.state.form {
                formView(form)
            } else {
                RecordFormStyle.notFoundView(subject: "Lameness evaluation")
            }
        }
        .navigationTitle(viewModel.form?.isEditing == true ? "Edit Lameness" : "New Lameness")
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
            if let errorMessage = viewModel.state.form?.dateError ?? viewModel.state.form?.gradeError {
                RecordFormStyle.errorBanner(message: errorMessage) {
                    viewModel.dismissError()
                }
            }
        }
        .onAppear {
            viewModel.onSaved = { dismiss() }
        }
    }

    private func formView(_ form: LamenessFormState) -> some View {
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
                Picker("AAEP Grade", selection: Binding(
                    get: { form.gradeAAEP.isEmpty ? nil : form.gradeAAEP },
                    set: { viewModel.onGradeAAEPChange($0 ?? "") }
                )) {
                    Text("None").tag(String?.none)
                    ForEach(1...5, id: \.self) { grade in
                        Text("Grade \(grade)").tag(String?.some(String(grade)))
                    }
                }
                .pickerStyle(.menu)

                if let gradeError = form.gradeError {
                    RecordFormStyle.errorText(gradeError)
                }

                RecordFormStyle.textField("Limb location", value: form.limbLocation) {
                    viewModel.onLimbLocationChange($0)
                }

                RecordFormStyle.textField("Flexion test", value: form.flexionTest, onChange: { viewModel.onFlexionTestChange($0) })
            } header: {
                RecordFormStyle.sectionHeader("Lameness")
            }

            Section {
                TextField("Diagnosis", text: Binding(
                    get: { form.diagnosis ?? "" },
                    set: { viewModel.onDiagnosisChange($0) }
                ), axis: .vertical)
                .lineLimit(3...6)
                .textCase(nil)

                TextField("Treatment", text: Binding(
                    get: { form.treatment ?? "" },
                    set: { viewModel.onTreatmentChange($0) }
                ), axis: .vertical)
                .lineLimit(3...6)
                .textCase(nil)
            } header: {
                RecordFormStyle.sectionHeader("Findings & Plan")
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
final class LamenessEditViewModel: RecordFormViewModel<LamenessEditStoreState> {
    private let store: LamenessEditStore

    init(patientId: Int64, lamenessId: Int64?) {
        let store = RecordStores.lamenessEditStore(patientId: patientId, lamenessId: lamenessId)
        self.store = store
        super.init(
            initial: store.state.current,
            subscribe: { store.state.subscribe(onEach: $0) },
            isSaving: { $0.form?.isSaving == true },
            hasError: { $0.form?.dateError != nil || $0.form?.gradeError != nil }
        )
    }

    var form: LamenessFormState? { state.form }

    func onDateChange(_ value: String) { store.onDateChange(date: value) }
    func onGradeAAEPChange(_ value: String) { store.onGradeAAEPChange(value: value) }
    func onLimbLocationChange(_ value: String) { store.onLimbLocationChange(value: value) }
    func onFlexionTestChange(_ value: String) { store.onFlexionTestChange(value: value) }
    func onDiagnosisChange(_ value: String) { store.onDiagnosisChange(value: value) }
    func onTreatmentChange(_ value: String) { store.onTreatmentChange(value: value) }
    func onVetNameChange(_ value: String) { store.onVetNameChange(value: value) }
    func onNotesChange(_ value: String) { store.onNotesChange(value: value) }
    func save() { store.save() }
    func dismissError() { store.dismissError() }
}
