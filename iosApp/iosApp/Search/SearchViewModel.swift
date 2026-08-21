import Foundation
import Shared

@MainActor
final class SearchViewModel: ObservableObject {
    @Published var state: SearchUiState

    private let store: SearchStore
    private var cancellable: NativeCancellable?

    init() {
        store = IosSettingsStores.shared.searchStore()
        state = store.state.current
        cancellable = store.state.subscribe(onEach: { [weak self] newState in
            Task { @MainActor in
                self?.state = newState
            }
        })
    }

    func setQuery(query: String) {
        store.setQuery(query: query)
    }

    func toggleRecordType(recordType: String) {
        store.toggleRecordType(recordType: recordType)
    }

    func dismissError() {
        store.dismissError()
    }

    deinit {
        cancellable?.cancel()
    }
}
