import Foundation
import FoundationModels

/// Thin Objective-C-exposed shim around Apple Foundation Models.
///
/// FoundationModels is a Swift-only framework (macros, generics) and cannot be consumed
/// directly through Kotlin/Native cinterop, so this class is the single boundary: it exposes
/// a String-in / String-out `@objc` interface that the Kotlin side calls via cinterop. All
/// `LanguageModelSession` usage stays here. The deployment target is 18.2, so every FM entry
/// point is guarded with `@available(iOS 26.0, *)`.
// Explicit ObjC name must match the cinterop header (@interface FmLlmShim).
// Without it, Swift emits the mangled name _TtC8Animally9FmLlmShim and linking fails.
@objc(FmLlmShim)
class FmLlmShim: NSObject {

    @objc
    func availability() -> String {
        guard #available(iOS 26.0, *) else {
            return "unavailable:modelNotReady"
        }
        switch SystemLanguageModel.default.availability {
        case .available:
            return "available"
        case .unavailable(let reason):
            switch reason {
            case .deviceNotEligible:
                return "unavailable:deviceNotEligible"
            case .appleIntelligenceNotEnabled:
                return "unavailable:appleIntelligenceNotEnabled"
            case .modelNotReady:
                return "unavailable:modelNotReady"
            @unknown default:
                return "unavailable:unknown"
            }
        @unknown default:
            return "unavailable:unknown"
        }
    }

    @objc
    func generate(_ prompt: String, completion: @escaping (String?, String?) -> Void) {
        generateWithInstructions(prompt, instructions: "", completion: completion)
    }

    /// Generates a response for `prompt` grounded in `instructions` (system
    /// prompt). Creates a fresh LanguageModelSession per call carrying the
    /// instructions (LanguageModelSession is single-request-at-a-time, #144).
    @objc
    func generateWithInstructions(
        _ prompt: String,
        instructions: String,
        completion: @escaping (String?, String?) -> Void
    ) {
        guard #available(iOS 26.0, *) else {
            completion(nil, "FoundationModels requires iOS 26.0")
            return
        }
        Task {
            do {
                let session =
                    if instructions.isEmpty {
                        LanguageModelSession()
                    } else {
                        LanguageModelSession(instructions: instructions)
                    }
                let response = try await session.respond(to: Prompt(prompt))
                completion(response.content, nil)
            } catch {
                completion(nil, error.localizedDescription)
            }
        }
    }

    @objc
    func generateJson(_ prompt: String, schema: String, completion: @escaping (String?, String?) -> Void) {
        guard #available(iOS 26.0, *) else {
            completion(nil, "FoundationModels requires iOS 26.0")
            return
        }
        Task {
            do {
                let session = LanguageModelSession()
                let response = try await session.respond(to: Prompt(prompt))
                completion(response.content, nil)
            } catch {
                completion(nil, error.localizedDescription)
            }
        }
    }

    // MARK: - Streaming

    /// Gate for the currently active stream. Only one stream may run at a time per
    /// shim instance (FoundationModels sessions are single-request-at-a-time). The
    /// boolean is claimed synchronously before the Task launches so completion racing
    /// the property write below cannot wedge the shim.
    private var streamActive = false
    private var streamTask: Task<Void, Never>?
    private let streamLock = NSLock()

    /// Streams a response for `prompt` grounded in `instructions`. Creates a fresh
    /// LanguageModelSession per call carrying the instructions. Each snapshot's
    /// `content` is the cumulative text so far; it is forwarded via `onChunk` as it
    /// arrives. `onComplete` fires exactly once at the end with the final text or an
    /// error. The stream must be iterated inside its creating task (`sending`
    /// constraint on ResponseStream), which is why the loop lives here.
    @objc
    func streamResponseWithInstructions(
        _ prompt: String,
        instructions: String,
        onChunk: @escaping (String) -> Void,
        onComplete: @escaping (String?, String?) -> Void
    ) {
        guard #available(iOS 26.0, *) else {
            onComplete(nil, "FoundationModels requires iOS 26.0")
            return
        }
        streamLock.lock()
        if streamActive {
            streamLock.unlock()
            onComplete(nil, "Another stream is already active")
            return
        }
        streamActive = true
        streamLock.unlock()

        // Captured strongly: the shim is owned by the app-lifetime LlmEngine, and the
        // cycle (task -> shim -> streamTask) breaks when the defer clears streamTask.
        let task = Task {
            defer {
                self.streamLock.lock()
                self.streamActive = false
                // Release the finished task too: keeps the shim -> task cycle
                // from outliving the stream and drops the completion handler
                // context promptly.
                self.streamTask = nil
                self.streamLock.unlock()
            }
            do {
                let session =
                    if instructions.isEmpty {
                        LanguageModelSession()
                    } else {
                        LanguageModelSession(instructions: instructions)
                    }
                var lastContent = ""
                let stream = session.streamResponse(to: Prompt(prompt))
                for try await snapshot in stream {
                    try Task.checkCancellation()
                    lastContent = snapshot.content
                    onChunk(snapshot.content)
                }
                onComplete(lastContent, nil)
            } catch is CancellationError {
                onComplete(nil, "cancelled")
            } catch {
                onComplete(nil, error.localizedDescription)
            }
        }
        streamLock.lock()
        streamTask = task
        streamLock.unlock()
    }

    /// Cancels the active streaming task. No-op when idle.
    @objc
    func cancelStream() {
        streamLock.lock()
        streamTask?.cancel()
        streamLock.unlock()
    }
}
