import Foundation

/// Turns a raw dictated transcript into the session JSON consumed by
/// `DictationStore.validate` — i.e. a `DictatedSessionDto` payload whose
/// records carry exactly these keys:
/// `recordType`, `patientName`, `date`, `weightKg`, `ovaryStatus`,
/// `uterineStatus`, `follicleSizeMm`, `drugName`, `notes`.
///
/// Implementations: FoundationModels on-device (`FmDictationExtractor`) and
/// the canned simulator path (`MockDictationExtractor`). Swap via
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

extension DictationExtracting {
    func extract(transcript: String) async throws -> String {
        try await extract(transcript: transcript, onUpdate: nil)
    }
}

/// Chooses the dictation extractor for this device.
///
/// Swap point for the extraction backend: FoundationModels when available,
/// canned mock otherwise (drives validation/resolution paths on the
/// simulator without Apple Intelligence).
enum DictationExtractorFactory {
    @MainActor
    static func make() -> any DictationExtracting {
        if #available(iOS 26.0, *), FmDictationExtractor.isAvailable {
            return FmDictationExtractor()
        }
        return MockDictationExtractor()
    }
}
