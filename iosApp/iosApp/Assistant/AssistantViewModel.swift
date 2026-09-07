import Foundation
import Shared

@MainActor
final class AssistantViewModel: ObservableObject {
    @Published var state: AssistantStoreState

    private let store: AssistantStore
    private var cancellable: NativeCancellable?
    private var pendingState: AssistantStoreState?
    private var stateDeliveryTask: Task<Void, Never>?

    init() {
        store = IosSettingsStores.shared.assistantStore()
        state = store.state.current
        cancellable = store.state.subscribe(onEach: { [weak self] newState in
            Task { @MainActor in
                self?.enqueueState(newState)
            }
        })
    }

    /// Coalesces rapid Kotlin StateFlow snapshots before publishing to SwiftUI.
    /// A cloud stream can emit many cumulative text states per second; assigning
    /// every one directly to an @Published property queues redundant view-tree
    /// work on the main actor and can starve accessibility snapshots. The latest
    /// state is sufficient because each snapshot contains the complete transcript.
    private func enqueueState(_ newState: AssistantStoreState) {
        pendingState = newState
        guard stateDeliveryTask == nil else { return }

        stateDeliveryTask = Task { @MainActor [weak self] in
            // Let other StateFlow callbacks that arrived in this run-loop turn
            // replace pendingState before SwiftUI receives the publication.
            await Task.yield()
            guard let self else { return }
            state = pendingState ?? state
            pendingState = nil
            stateDeliveryTask = nil
        }
    }

    func ask(question: String) {
        store.ask(question: question)
    }

    func cancelGeneration() {
        store.cancelGeneration()
    }

    func refreshAvailability() {
        store.refreshAvailability()
    }

    func refreshHistory() {
        store.refreshHistory()
    }

    func startNewChat() {
        store.startNewChat()
    }

    func dismissError() {
        store.dismissError()
    }

    deinit {
        cancellable?.cancel()
        stateDeliveryTask?.cancel()
        store.clear()
    }
}
