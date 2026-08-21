import SwiftUI
import Shared

struct ConsultationEditView: View {
    @StateObject private var viewModel: ConsultationEditViewModel
    @Environment(\.dismiss) private var dismiss

    init(patientId: Int64, consultationId: Int64?) {
        _viewModel = StateObject(wrappedValue: ConsultationEditViewModel(patientId: patientId, consultationId: consultationId))
    }

    var body: some View {
        Group {
            if viewModel.state.form?.isLoading == true {
                RecordFormStyle.loadingView(subject: "consultation")
            } else if let form = viewModel.state.form {
                formView(form)
            } else {
                RecordFormStyle.notFoundView(subject: "Consultation")
            }
        }
        .navigationTitle(viewModel.form?.isEditing == true ? "Edit Consultation" : "New Consultation")
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
            if let errorMessage = viewModel.state.form?.dateError ?? viewModel.state.form?.nextVisitDateError {
                RecordFormStyle.errorBanner(message: errorMessage) {
                    viewModel.dismissError()
                }
            }
        }
        .onAppear {
            viewModel.onSaved = { dismiss() }
        }
    }

    private func formView(_ form: ConsultationFormState) -> some View {
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
                RecordFormStyle.sectionHeader("Visit")
            }

            soapSection(
                title: "Subjective",
                placeholder: "Owner's description of the problem",
                value: form.subjective
            ) { viewModel.onSubjectiveChange($0) }

            soapSection(
                title: "Objective",
                placeholder: "Exam findings, vitals, observations",
                value: form.objective
            ) { viewModel.onObjectiveChange($0) }

            soapSection(
                title: "Assessment",
                placeholder: "Diagnosis and differential considerations",
                value: form.assessment
            ) { viewModel.onAssessmentChange($0) }

            soapSection(
                title: "Plan",
                placeholder: "Treatment, medication, follow-up actions",
                value: form.plan
            ) { viewModel.onPlanChange($0) }

            Section {
                RecordFormStyle.dateField("YYYY-MM-DD", value: form.nextVisitDate ?? "") {
                    viewModel.onNextVisitDateChange($0)
                }

                if let nextVisitDateError = form.nextVisitDateError {
                    RecordFormStyle.errorText(nextVisitDateError)
                }
            } header: {
                RecordFormStyle.sectionHeader("Next Visit")
            }
        }
    }

    private func soapSection(
        title: String,
        placeholder: String,
        value: String,
        onChange: @escaping (String) -> Void
    ) -> some View {
        Section {
            TextField(placeholder, text: Binding(
                get: { value },
                set: { onChange($0) }
            ), axis: .vertical)
            .lineLimit(3...8)
            .textCase(nil)
        } header: {
            RecordFormStyle.sectionHeader(title)
        }
    }
}

@MainActor
final class ConsultationEditViewModel: RecordFormViewModel<ConsultationEditStoreState> {
    private let store: ConsultationEditStore

    init(patientId: Int64, consultationId: Int64?) {
        let store = IosEditStores.shared.consultationEditStore(
            patientId: patientId,
            consultationId: consultationId.map { KotlinLong(longLong: $0) }
        )
        self.store = store
        super.init(
            initial: store.state.current,
            subscribe: { store.state.subscribe(onEach: $0) },
            isSaving: { $0.form?.isSaving == true },
            hasError: { $0.form?.dateError != nil || $0.form?.nextVisitDateError != nil }
        )
    }

    var form: ConsultationFormState? { state.form }

    func onDateChange(_ value: String) { store.onDateChange(date: value) }
    func onSubjectiveChange(_ value: String) { store.onSubjectiveChange(subjective: value) }
    func onObjectiveChange(_ value: String) { store.onObjectiveChange(objective: value) }
    func onAssessmentChange(_ value: String) { store.onAssessmentChange(assessment: value) }
    func onPlanChange(_ value: String) { store.onPlanChange(plan: value) }
    func onVetNameChange(_ value: String) { store.onVetNameChange(vetName: value) }
    func onNextVisitDateChange(_ value: String) { store.onNextVisitDateChange(nextVisitDate: value) }
    func save() { store.save() }
    func dismissError() { store.dismissError() }
}
