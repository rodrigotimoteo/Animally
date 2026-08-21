import Foundation
import Shared

@MainActor
final class OwnerDetailViewModel: ObservableObject {
    @Published var state: OwnerDetailUiState

    private let store: OwnerDetailStore
    private var cancellable: NativeCancellable?

    init(ownerId: Int64) {
        store = IosAppBridge.shared.ownerDetailStore(ownerId: ownerId)
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

    func dismissError() {
        store.dismissError()
    }

    deinit {
        cancellable?.cancel()
    }
}
