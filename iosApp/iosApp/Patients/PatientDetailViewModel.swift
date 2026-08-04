import Foundation
import Shared

@MainActor
final class PatientDetailViewModel: ObservableObject {
    @Published var state: PatientDetailUiState

    private let store: PatientDetailStore
    private var cancellable: NativeCancellable?

    init(patientId: Int64) {
        store = IosAppBridge.shared.patientDetailStore(patientId: patientId)
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
