import SwiftUI
import Shared

struct UltrasoundEditView: View {
    @StateObject private var viewModel: UltrasoundEditViewModel
    @Environment(\.dismiss) private var dismiss

    init(patientId: Int64, ultrasoundId: Int64?) {
        _viewModel = StateObject(wrappedValue: UltrasoundEditViewModel(patientId: patientId, ultrasoundId: ultrasoundId))
    }

    var body: some View {
        Group {
            if viewModel.state.form?.isLoading == true {
                RecordFormStyle.loadingView(subject: "ultrasound")
            } else if let form = viewModel.state.form {
                formView(form)
            } else {
                RecordFormStyle.notFoundView(subject: "Ultrasound")
            }
        }
        .navigationTitle(viewModel.form?.isEditing == true ? "Edit Ultrasound" : "New Ultrasound")
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
            if let errorMessage = viewModel.state.form?.dateError ?? viewModel.state.form?.follicleSizeMmError {
                RecordFormStyle.errorBanner(message: errorMessage) {
                    viewModel.dismissError()
                }
            }
        }
        .onAppear {
            viewModel.onSaved = { dismiss() }
        }
    }

    /// One editable follicle row: size, optional description, remove action.
    private func follicleRow(
        side: String,
        index: Int,
        follicle: FollicleRow,
    ) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack {
                TextField("Size (mm)", text: Binding(
                    get: { follicle.sizeMm },
                    set: { viewModel.onFollicleSizeChange(side, index: index, $0) }
                ))
                .keyboardType(.decimalPad)
                .textCase(nil)

                Button {
                    viewModel.onRemoveFollicle(side, index: index)
                } label: {
                    Image(systemName: "minus.circle.fill")
                        .foregroundStyle(.red)
                }
                .buttonStyle(.plain)
                .accessibilityLabel("Remove follicle")
            }

            TextField("Description (optional)", text: Binding(
                get: { follicle.description ?? "" },
                set: { viewModel.onFollicleDescriptionChange(side, index: index, $0) }
            ))
            .font(.subheadline)
            .textCase(nil)
        }
    }

    private func formView(_ form: UltrasoundFormState) -> some View {
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
                RecordFormStyle.textField("Left ovary status", value: form.leftOvaryStatus) {
                    viewModel.onLeftOvaryStatusChange($0)
                }

                ForEach(Array(form.leftFollicles.enumerated()), id: \.offset) { index, follicle in
                    follicleRow(side: "LEFT", index: index, follicle: follicle)
                }

                Button {
                    viewModel.onAddFollicle("LEFT")
                } label: {
                    Label("Add follicle", systemImage: "plus.circle.fill")
                        .font(.subheadline.weight(.medium))
                        .foregroundStyle(Theme.forestGreen)
                }
                .buttonStyle(.plain)
            } header: {
                RecordFormStyle.sectionHeader("Left Ovary")
            }

            Section {
                RecordFormStyle.textField("Right ovary status", value: form.rightOvaryStatus) {
                    viewModel.onRightOvaryStatusChange($0)
                }

                ForEach(Array(form.rightFollicles.enumerated()), id: \.offset) { index, follicle in
                    follicleRow(side: "RIGHT", index: index, follicle: follicle)
                }

                Button {
                    viewModel.onAddFollicle("RIGHT")
                } label: {
                    Label("Add follicle", systemImage: "plus.circle.fill")
                        .font(.subheadline.weight(.medium))
                        .foregroundStyle(Theme.forestGreen)
                }
                .buttonStyle(.plain)
            } header: {
                RecordFormStyle.sectionHeader("Right Ovary")
            }

            Section {
                RecordFormStyle.textField("Uterine status", value: form.uterineStatus) {
                    viewModel.onUterineStatusChange($0)
                }

                Toggle("Fluid present", isOn: Binding(
                    get: { form.uterineLiquid == true },
                    set: { viewModel.onUterineLiquidChange($0) }
                ))

                if form.uterineLiquid == true {
                    RecordFormStyle.textField("Fluid description", value: form.uterineLiquidDescription) {
                        viewModel.onUterineLiquidDescriptionChange($0)
                    }
                }

                RecordFormStyle.textField("Uterine edema", value: form.uterineEdema) {
                    viewModel.onUterineEdemaChange($0)
                }

                RecordFormStyle.textField("Uterus description", value: form.uterusDescription) {
                    viewModel.onUterusDescriptionChange($0)
                }

                if let follicleSizeMmError = form.follicleSizeMmError {
                    RecordFormStyle.errorText(follicleSizeMmError)
                }
            } header: {
                RecordFormStyle.sectionHeader("Uterus")
            }

            Section {
                TextField("Findings", text: Binding(
                    get: { form.findings ?? "" },
                    set: { viewModel.onFindingsChange($0) }
                ), axis: .vertical)
                .lineLimit(3...6)
                .textCase(nil)

                ImageAttachmentField(title: "Ultrasound Image", imagePath: form.imageUris) {
                    viewModel.onImageUrisChange($0)
                }
            } header: {
                RecordFormStyle.sectionHeader("Findings & Images")
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
final class UltrasoundEditViewModel: RecordFormViewModel<UltrasoundEditStoreState> {
    private let store: UltrasoundEditStore

    init(patientId: Int64, ultrasoundId: Int64?) {
        let store = RecordStores.ultrasoundEditStore(patientId: patientId, ultrasoundId: ultrasoundId)
        self.store = store
        super.init(
            initial: store.state.current,
            subscribe: { store.state.subscribe(onEach: $0) },
            isSaving: { $0.form?.isSaving == true },
            hasError: { $0.form?.dateError != nil || $0.form?.follicleSizeMmError != nil }
        )
    }

    var form: UltrasoundFormState? { state.form }

    func onDateChange(_ value: String) { store.onDateChange(date: value) }
    func onOvaryStatusChange(_ value: String) { store.onOvaryStatusChange(value: value) }
    func onUterineStatusChange(_ value: String) { store.onUterineStatusChange(value: value) }
    func onFollicleSizeMmChange(_ value: String) { store.onFollicleSizeMmChange(value: value) }
    func onLeftOvaryStatusChange(_ value: String) { store.onLeftOvaryStatusChange(value: value) }
    func onRightOvaryStatusChange(_ value: String) { store.onRightOvaryStatusChange(value: value) }
    func onLeftFollicleSizeMmChange(_ value: String) { store.onLeftFollicleSizeMmChange(value: value) }
    func onRightFollicleSizeMmChange(_ value: String) { store.onRightFollicleSizeMmChange(value: value) }
    func onUterineEdemaChange(_ value: String) { store.onUterineEdemaChange(value: value) }
    func onUterineLiquidChange(_ value: Bool) { store.onUterineLiquidChange(value: KotlinBoolean(bool: value)) }
    func onUterineLiquidDescriptionChange(_ value: String) { store.onUterineLiquidDescriptionChange(value: value) }
    func onUterusDescriptionChange(_ value: String) { store.onUterusDescriptionChange(value: value) }
    func onAddFollicle(_ side: String) { store.onAddFollicle(side: side) }
    func onRemoveFollicle(_ side: String, index: Int) { store.onRemoveFollicle(side: side, index: Int64(index)) }
    func onFollicleSizeChange(_ side: String, index: Int, _ value: String) {
        store.onFollicleSizeChange(side: side, index: Int64(index), value: value)
    }
    func onFollicleDescriptionChange(_ side: String, index: Int, _ value: String) {
        store.onFollicleDescriptionChange(side: side, index: Int64(index), value: value)
    }
    func onFindingsChange(_ value: String) { store.onFindingsChange(value: value) }
    func onImageUrisChange(_ value: String) { store.onImageUrisChange(value: value) }
    func onVetNameChange(_ value: String) { store.onVetNameChange(value: value) }
    func onNotesChange(_ value: String) { store.onNotesChange(value: value) }
    func save() { store.save() }
    func dismissError() { store.dismissError() }
}
