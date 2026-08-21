import Foundation
import Shared

@MainActor
final class OwnerEditViewModel: ObservableObject {
    @Published var state: OwnerEditStoreState

    private let store: OwnerEditStore
    private var cancellable: NativeCancellable?

    /// Closure invoked after a successful save so the view can pop.
    var onSaved: (() -> Void)?

    init(ownerId: Int64?) {
        store = IosAppBridge.shared.ownerEditStore(ownerId: ownerId.map { KotlinLong(longLong: $0) })
        state = store.state.current
        cancellable = store.state.subscribe(onEach: { [weak self] newState in
            Task { @MainActor in
                let wasSaving = self?.state.form?.isSaving == true
                self?.state = newState
                // Detect save completion: was saving, now no longer saving, no error
                if wasSaving,
                   newState.form?.isSaving == false,
                   newState.form?.nameError == nil {
                    self?.onSaved?()
                }
            }
        })
    }

    var form: OwnerFormState? { state.form }

    func onNameChange(_ name: String) {
        store.onNameChange(name: name)
    }

    func onPhoneChange(_ phone: String) {
        store.onPhoneChange(phone: phone)
    }

    func onEmailChange(_ email: String) {
        store.onEmailChange(email: email)
    }

    func onAddressChange(_ address: String) {
        store.onAddressChange(address: address)
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
