import Foundation

/// Turns a raw dictated transcript into the session JSON consumed by
/// `DictationStore.validate` — i.e. a `DictatedSessionDto` payload whose
/// records carry exactly these keys:
/// `recordType`, `patientName`, `date`, `weightKg`, `ovaryStatus`,
/// `uterineStatus`, `follicleSizeMm`, `drugName`, `notes`.
///
/// Implementations: FoundationModels on-device (`FmDictationExtractor`), a
/// deterministic UI-test extractor (`MockDictationExtractor`), and an explicit
/// unavailable state for devices without structured extraction. Swap via
/// [DictationExtractorFactory.make].
protocol DictationExtracting {
    /// Extracts structured records from [transcript].
    ///
    /// - Parameter transcript: The finalized speech transcript.
    /// - Parameter onUpdate: Optional callback fired with intermediate JSON
    ///   snapshots while extraction streams (best effort).
    /// - Returns: The complete session JSON string.
    func extract(
        transcript: String,
        onUpdate: ((String) -> Void)?
    ) async throws -> String
}

/// Language selected for one dictation session. Speech recognition and
/// structured extraction receive the same choice so an English transcript is
/// not sent through Portuguese-only instructions (and vice versa).
enum DictationLanguage: String, CaseIterable, Identifiable {
    case english
    case portuguese

    var id: String { rawValue }

    var localeIdentifier: String {
        switch self {
        case .english: return "en-US"
        case .portuguese: return "pt-PT"
        }
    }

    var displayName: String {
        switch self {
        case .english: return "English"
        case .portuguese: return "Português (PT)"
        }
    }

    var locale: Locale { Locale(identifier: localeIdentifier) }

    static var deviceDefault: DictationLanguage {
        let preferred = Locale.preferredLanguages.first?.lowercased() ?? ""
        return preferred.hasPrefix("pt") ? .portuguese : .english
    }
}

/// Explicit opt-in used only by the simulator UI test. Runtime availability
/// must never silently turn the canned fixture into a production extractor.
enum DictationTestConfiguration {
    static var isEnabled: Bool {
        ProcessInfo.processInfo.arguments.contains("-animally-ui-test-dictation")
    }
}

extension DictationExtracting {
    func extract(transcript: String) async throws -> String {
        try await extract(transcript: transcript, onUpdate: nil)
    }
}

/// Chooses the dictation extractor for this device.
///
/// Swap point for the extraction backend: FoundationModels when available,
/// an explicit deterministic mock only under the UI-test launch argument, or
/// an unavailable result when structured extraction cannot run on-device.
enum DictationExtractorFactory {
    @MainActor
    static func make(language: DictationLanguage = .deviceDefault) -> any DictationExtracting {
        if DictationTestConfiguration.isEnabled {
            return MockDictationExtractor(latency: 0.1)
        }
        if #available(iOS 26.0, *), FmDictationExtractor.isAvailable {
            return FmDictationExtractor(language: language)
        }
        return UnavailableDictationExtractor()
    }
}

/// Production fallback when structured extraction is not available on-device.
/// The user can still capture/edit the speech transcript, but no fabricated
/// records are presented for review.
struct UnavailableDictationExtractor: DictationExtracting {
    func extract(
        transcript: String,
        onUpdate: ((String) -> Void)?
    ) async throws -> String {
        throw DictationExtractorError.unavailable
    }
}

enum DictationExtractorError: LocalizedError {
    case unavailable

    var errorDescription: String? {
        switch self {
        case .unavailable:
            return "Structured dictation is unavailable on this device. You can still copy the transcript and add the records manually."
        }
    }
}
