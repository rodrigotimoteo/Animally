import Foundation
import Shared

@MainActor
final class PatientListViewModel: ObservableObject {
    @Published var state: PatientListUiState

    private let store: PatientListStore
    private var cancellable: NativeCancellable?

    init() {
        store = IosAppBridge.shared.patientListStore()
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

    func delete(patientId: Int64) {
        store.deletePatient(patientId: patientId)
    }

    func dismissError() {
        store.dismissError()
    }

    deinit {
        cancellable?.cancel()
    }
}
