import Foundation
import Shared

/// Orchestrates linking an existing patient to an owner: lists candidates and
/// performs the link by loading the patient's edit form, setting the owner,
/// and saving through the existing patient-edit bridge.
@MainActor
final class LinkPatientViewModel: ObservableObject {
    struct Section: Identifiable {
        let title: String
        let patients: [Patient_]
        var id: String { title }
    }

    @Published private(set) var sections: [Section] = []
    @Published private(set) var linkingPatientId: Int64?
    @Published var errorMessage: String?

    /// Invoked once a link succeeds so the view can dismiss the sheet.
    var onLinked: (() -> Void)?

    private let ownerId: Int64

    private var linkStore: PatientEditStore?
    private var cancellable: NativeCancellable?
    private var linkStarted = false
    private var wasSaving = false

    init(ownerId: Int64) {
        self.ownerId = ownerId
        reloadPatients()
    }

    deinit {
        cancellable?.cancel()
    }

    /// Splits the patient list into unassigned patients and those belonging
    /// to other owners; both are linkable, this owner's own patients are not shown.
    func reloadPatients() {
        let store = IosSettingsStores.shared.settingsStore()
        let all = (store.patients as? [Patient_]) ?? []
        let unassigned = all.filter { $0.ownerId == nil }
        let otherOwners = all.filter { id in
            guard let ownerId = id.ownerId else { return false }
            return ownerId.int64Value != self.ownerId
        }
        sections = [
            Section(title: "Unassigned", patients: unassigned),
            Section(title: "Other owners", patients: otherOwners),
        ].filter { !$0.patients.isEmpty }
    }

    /// Links the tapped patient to this owner via the patient-edit bridge.
    func link(_ patient: Patient_) {
        guard linkingPatientId == nil else { return }
        linkingPatientId = patient.id
        errorMessage = nil
        linkStarted = false
        wasSaving = false

        let store = IosAppBridge.shared.patientEditStore(patientId: KotlinLong(longLong: patient.id), initialOwnerId: nil)
        linkStore = store
        cancellable = store.state.subscribe(onEach: { [weak self] newState in
            Task { @MainActor in
                self?.handleState(newState, store: store)
            }
        })
    }

    func dismissError() {
        errorMessage = nil
    }

    private func handleState(_ newState: PatientEditStoreState, store: PatientEditStore) {
        guard let form = newState.form else { return }

        if !linkStarted {
            // Form finished loading — set the owner and save.
            guard !form.isLoading else { return }
            linkStarted = true
            store.onOwnerChange(ownerId: KotlinLong(longLong: ownerId))
            store.save()
        } else {
            // Detect save completion the same way RecordForm flows do.
            let previouslySaving = wasSaving
            wasSaving = form.isSaving
            if previouslySaving, !form.isSaving {
                finish(error: form.nameError)
            }
        }
    }

    private func finish(error: String?) {
        cancellable?.cancel()
        cancellable = nil
        linkStore = nil

        if let error {
            linkingPatientId = nil
            errorMessage = error
        } else {
            onLinked?()
        }
    }
}
