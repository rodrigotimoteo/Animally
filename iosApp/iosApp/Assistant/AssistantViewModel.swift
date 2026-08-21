import Foundation
import Shared

@MainActor
final class AssistantViewModel: ObservableObject {
    @Published var state: AssistantUiState

    private let store: AssistantStore
    private var cancellable: NativeCancellable?

    init() {
        store = IosSettingsStores.shared.assistantStore()
        state = store.state.current
        cancellable = store.state.subscribe(onEach: { [weak self] newState in
            Task { @MainActor in
                self?.state = newState
            }
        })
    }

    func ask(question: String) {
        store.ask(question: question)
    }

    func refreshAvailability() {
        store.refreshAvailability()
    }

    func dismissError() {
        store.dismissError()
    }

    deinit {
        cancellable?.cancel()
    }
}
