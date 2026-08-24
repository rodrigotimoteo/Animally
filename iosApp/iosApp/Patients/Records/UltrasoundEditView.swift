import SwiftUI
import Shared

struct UltrasoundEditView: View {
    @StateObject private var viewModel: UltrasoundEditViewModel
    @Environment(\.dismiss) private var dismiss

    init(patientId: Int64, ultrasoundId: Int64?, prefill: RecordPrefill? = nil) {
        _viewModel = StateObject(wrappedValue: UltrasoundEditViewModel(patientId: patientId, ultrasoundId: ultrasoundId, prefill: prefill))
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
            viewModel.applyPrefillIfNeeded()
        }
        .onChange(of: viewModel.hasForm) { _ in
            viewModel.applyPrefillIfNeeded()
        }
    }

    /// One editable follicle row: size, optional description, remove action.
    /// Guided entry for one ovary: status field, numbered follicle cards with
    /// labeled inputs, an empty-state prompt, and a full-width add affordance.
    private func ovarySection(
        side: String,
        title: String,
        status: String?,
        onStatusChange: @escaping (String) -> Void,
        follicles: [FollicleRow],
    ) -> some View {
        Section {
            RecordFormStyle.textField("\(title) status", value: status) { onStatusChange($0) }

            if follicles.isEmpty {
                Text("No follicles recorded. Tap Add follicle to record the first one.")
                    .font(.footnote)
                    .foregroundStyle(Theme.textSecondary)
            } else {
                ForEach(Array(follicles.enumerated()), id: \.offset) { index, follicle in
                    follicleCard(side: side, number: index + 1, index: index, follicle: follicle)
                }
            }

            Button {
                viewModel.onAddFollicle(side)
            } label: {
                HStack {
                    Image(systemName: "plus.circle.fill")
                    Text("Add follicle")
                        .font(.subheadline.weight(.medium))
                }
                .frame(maxWidth: .infinity)
                .foregroundStyle(Theme.forestGreen)
            }
            .buttonStyle(.plain)
        } header: {
            RecordFormStyle.sectionHeader(title)
        } footer: {
            Text("Record each visible follicle separately with its size in millimeters.")
                .font(.caption2)
                .foregroundStyle(Theme.textSecondary)
        }
    }

    /// One follicle entry card: numbered header with remove action, labeled
    /// size and description inputs.
    private func follicleCard(
        side: String,
        number: Int,
        index: Int,
        follicle: FollicleRow,
    ) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                Label("Follicle \(number)", systemImage: "circle.dotted")
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(Theme.forestGreen)
                Spacer()
                Button {
                    viewModel.onRemoveFollicle(side, index: index)
                } label: {
                    Image(systemName: "minus.circle.fill")
                        .foregroundStyle(.red)
                }
                .buttonStyle(.plain)
                .accessibilityLabel("Remove follicle \(number)")
            }

            VStack(alignment: .leading, spacing: 4) {
                Text("Size (mm)")
                    .font(.caption)
                    .foregroundStyle(Theme.textSecondary)
                TextField("0", text: Binding(
                    get: { follicle.sizeMm },
                    set: { viewModel.onFollicleSizeChange(side, index: index, $0) }
                ))
                .keyboardType(.decimalPad)
                .textCase(nil)
            }

            VStack(alignment: .leading, spacing: 4) {
                Text("Description")
                    .font(.caption)
                    .foregroundStyle(Theme.textSecondary)
                TextField("e.g. mature, soft edema", text: Binding(
                    get: { follicle.note ?? "" },
                    set: { viewModel.onFollicleDescriptionChange(side, index: index, $0) }
                ))
                .font(.subheadline)
                .textCase(nil)
            }
        }
        .padding(.vertical, 2)
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

            ovarySection(
                side: "LEFT",
                title: "Left Ovary",
                status: form.leftOvaryStatus,
                onStatusChange: { viewModel.onLeftOvaryStatusChange($0) },
                follicles: form.leftFollicles,
            )

            ovarySection(
                side: "RIGHT",
                title: "Right Ovary",
                status: form.rightOvaryStatus,
                onStatusChange: { viewModel.onRightOvaryStatusChange($0) },
                follicles: form.rightFollicles,
            )

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
    private let prefill: RecordPrefill?
    private var prefillApplied = false

    init(patientId: Int64, ultrasoundId: Int64?, prefill: RecordPrefill? = nil) {
        let store = RecordStores.ultrasoundEditStore(patientId: patientId, ultrasoundId: ultrasoundId)
        self.store = store
        self.prefill = prefill
        super.init(
            initial: store.state.current,
            subscribe: { store.state.subscribe(onEach: $0) },
            isSaving: { $0.form?.isSaving == true },
            hasError: { $0.form?.dateError != nil || $0.form?.follicleSizeMmError != nil }
        )
    }

    var form: UltrasoundFormState? { state.form }

    var hasForm: Bool { state.form != nil }

    /// Applies dictated values once the Kotlin form has loaded. Runs at most
    /// once; user edits afterwards always win. The dictated ovary status lands
    /// on the left ovary and the follicle size becomes its first follicle.
    func applyPrefillIfNeeded() {
        guard let prefill, !prefillApplied, state.form != nil else { return }
        prefillApplied = true
        if let date = prefill.date { onDateChange(date) }
        if let ovaryStatus = prefill.ovaryStatus { onLeftOvaryStatusChange(ovaryStatus) }
        if let uterineStatus = prefill.uterineStatus { onUterineStatusChange(uterineStatus) }
        if let follicleSizeMm = prefill.follicleSizeMm,
           (state.form?.leftFollicles.isEmpty ?? true) {
            onAddFollicle("LEFT")
            onFollicleSizeChange("LEFT", index: 0, follicleSizeMm)
        }
        if let notes = prefill.notes { onNotesChange(notes) }
    }

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
