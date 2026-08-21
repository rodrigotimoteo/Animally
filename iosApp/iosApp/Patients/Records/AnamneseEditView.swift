import SwiftUI
import Shared

struct AnamneseEditView: View {
    @StateObject private var viewModel: AnamneseEditViewModel
    @Environment(\.dismiss) private var dismiss

    init(patientId: Int64, anamneseId: Int64?) {
        _viewModel = StateObject(wrappedValue: AnamneseEditViewModel(patientId: patientId, anamneseId: anamneseId))
    }

    var body: some View {
        Group {
            if viewModel.state.form?.isLoading == true {
                RecordFormStyle.loadingView(subject: "anamnese")
            } else if let form = viewModel.state.form {
                formView(form)
            } else {
                RecordFormStyle.notFoundView(subject: "Anamnese")
            }
        }
        .navigationTitle(viewModel.form?.isEditing == true ? "Edit Anamnese" : "New Anamnese")
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
        .onAppear {
            viewModel.onSaved = { dismiss() }
        }
    }

    private func formView(_ form: AnamneseFormState) -> some View {
        List {
            Section {
                TextField("General history", text: Binding(
                    get: { form.generalHistory },
                    set: { viewModel.onGeneralHistoryChange($0) }
                ), axis: .vertical)
                .lineLimit(3...8)
                .textCase(nil)
            } header: {
                RecordFormStyle.sectionHeader("General History")
            }

            Section {
                TextField("Chronic conditions", text: Binding(
                    get: { form.chronicConditions },
                    set: { viewModel.onChronicConditionsChange($0) }
                ), axis: .vertical)
                .lineLimit(3...8)
                .textCase(nil)
            } header: {
                RecordFormStyle.sectionHeader("Chronic Conditions")
            }

            Section {
                TextField("Allergies", text: Binding(
                    get: { form.allergies },
                    set: { viewModel.onAllergiesChange($0) }
                ), axis: .vertical)
                .lineLimit(3...6)
                .textCase(nil)
            } header: {
                RecordFormStyle.sectionHeader("Allergies")
            }
        }
    }
}

@MainActor
final class AnamneseEditViewModel: RecordFormViewModel<AnamneseEditStoreState> {
    private let store: AnamneseEditStore

    init(patientId: Int64, anamneseId: Int64?) {
        let store = RecordStores.anamneseEditStore(patientId: patientId, anamneseId: anamneseId)
        self.store = store
        super.init(
            initial: store.state.current,
            subscribe: { store.state.subscribe(onEach: $0) },
            isSaving: { $0.form?.isSaving == true },
            hasError: { _ in false }
        )
    }

    var form: AnamneseFormState? { state.form }

    func onGeneralHistoryChange(_ value: String) { store.onGeneralHistoryChange(value: value) }
    func onChronicConditionsChange(_ value: String) { store.onChronicConditionsChange(value: value) }
    func onAllergiesChange(_ value: String) { store.onAllergiesChange(value: value) }
    func save() { store.save() }
}
