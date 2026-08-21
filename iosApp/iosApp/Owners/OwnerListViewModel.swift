import Foundation
import Shared

@MainActor
final class OwnerListViewModel: ObservableObject {
    @Published var state: OwnerListUiState

    private let store: OwnerListStore
    private var cancellable: NativeCancellable?

    init() {
        store = IosAppBridge.shared.ownerListStore()
        state = store.state.current
        cancellable = store.state.subscribe(onEach: { [weak self] newState in
            Task { @MainActor in
                self?.state = newState
            }
        })
    }

    func load() {
        store.load()
    }

    func delete(ownerId: Int64) {
        store.deleteOwner(ownerId: ownerId)
    }

    func dismissError() {
        store.dismissError()
    }

    deinit {
        cancellable?.cancel()
    }
}
