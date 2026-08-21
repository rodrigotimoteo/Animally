import Foundation
import Shared

@MainActor
final class PatientEditViewModel: ObservableObject {
    @Published var state: PatientEditStoreState

    private let store: PatientEditStore
    private var cancellable: NativeCancellable?

    /// Closure invoked after a successful save so the view can pop.
    var onSaved: (() -> Void)?

    init(patientId: Int64?, preselectedOwnerId: Int64? = nil) {
        store = IosAppBridge.shared.patientEditStore(
            patientId: patientId.map { KotlinLong(longLong: $0) },
            initialOwnerId: preselectedOwnerId.map { KotlinLong(longLong: $0) }
        )
        state = store.state.current
        cancellable = store.state.subscribe(onEach: { [weak self] newState in
            Task { @MainActor in
                let wasSaving = self?.state.form?.isSaving == true
                self?.state = newState
                // Detect save completion: was saving, now no longer saving, no error
                if wasSaving,
                   newState.form?.isSaving == false,
                   newState.form?.nameError == nil,
                   newState.form?.uelnError == nil {
                    self?.onSaved?()
                }
            }
        })
    }

    var form: PatientFormState? { state.form }

    func onNameChange(_ name: String) {
        store.onNameChange(name: name)
    }

    func onSpeciesChange(_ species: String) {
        store.onSpeciesChange(species: species)
    }

    func onBreedChange(_ breed: String) {
        store.onBreedChange(breed: breed)
    }

    func onDateOfBirthChange(_ dateOfBirth: String) {
        store.onDateOfBirthChange(dateOfBirth: dateOfBirth)
    }

    func onGenderChange(_ gender: String?) {
        store.onGenderChange(gender: gender ?? "")
    }

    func onMicrochipIdChange(_ microchipId: String) {
        store.onMicrochipIdChange(microchipId: microchipId)
    }

    func onUelnChange(_ ueln: String) {
        store.onUelnChange(ueln: ueln)
    }

    func onRegistrationNumberChange(_ registrationNumber: String) {
        store.onRegistrationNumberChange(registrationNumber: registrationNumber)
    }

    func onStableLocationChange(_ stableLocation: String) {
        store.onStableLocationChange(stableLocation: stableLocation)
    }

    func onNotesChange(_ notes: String) {
        store.onNotesChange(notes: notes)
    }

    func onCogginsTestDateChange(_ value: String) {
        store.onCogginsChange(field: .testDate, value: value)
    }

    func onCogginsResultChange(_ value: String) {
        store.onCogginsChange(field: .result, value: value)
    }

    func onCogginsExpiryDateChange(_ value: String) {
        store.onCogginsChange(field: .expiryDate, value: value)
    }

    func onOwnerChange(_ ownerId: Int64?) {
        store.onOwnerChange(ownerId: ownerId.map { KotlinLong(longLong: $0) })
    }

    func save() {
        store.save()
    }

    func dismissError() {
        store.dismissError()
    }

    deinit {
        cancellable?.cancel()
    }
}
