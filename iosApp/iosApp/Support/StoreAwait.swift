import Foundation
import Shared

enum StoreAwaitError: LocalizedError {
    case timeout
    var errorDescription: String? { "Loading timed out. Please retry." }
}

enum StoreAwait {
    /// Awaits the store's `isLoading` becoming false via KeyPath — avoids generic closure capture.
    /// Returns true if idle before timeout, false if timed out. Caller must surface timeout via InlineErrorBanner.
    @discardableResult
    static func awaitIdle<T>(_ flow: NativeFlow<T>, keyPath: KeyPath<T, Bool>) async -> Bool {
        await awaitIdle(flow, isLoading: { $0[keyPath: keyPath] })
    }

    /// Awaits the store's `isLoading` becoming false. Returns false on timeout.
    @discardableResult
    static func awaitIdle<T>(_ flow: NativeFlow<T>, isLoading: @escaping (T) -> Bool) async -> Bool {
        let ok = await withCheckedContinuation { (cont: CheckedContinuation<Bool, Never>) in
            var sub: NativeCancellable?
            var resumed = false
            sub = flow.subscribe { state in
                guard !resumed, !isLoading(state) else { return }
                resumed = true
                sub?.cancel()
                cont.resume(returning: true)
            }
            Task {
                try? await Task.sleep(nanoseconds: 5_000_000_000)
                guard !resumed else { return }
                resumed = true
                sub?.cancel()
                cont.resume(returning: false)
            }
        }
        await Task.yield()
        return ok
    }

    /// Throwing variant — throws StoreAwaitError.timeout on timeout.
    static func awaitIdleOrThrow<T>(_ flow: NativeFlow<T>, isLoading: @escaping (T) -> Bool) async throws {
        let ok = await awaitIdle(flow, isLoading: isLoading)
        if !ok { throw StoreAwaitError.timeout }
    }
}
