import Foundation
import Shared

@MainActor
final class TimelineViewModel: ObservableObject {
    @Published var state: TimelineUiState

    private let store: TimelineStore
    private var cancellable: NativeCancellable?

    init(patientId: Int64? = nil) {
        store = IosSettingsStores.shared.timelineStore(patientId: patientId.map { KotlinLong(longLong: $0) })
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
