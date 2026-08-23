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
}
