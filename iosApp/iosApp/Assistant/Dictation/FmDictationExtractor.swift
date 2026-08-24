import Foundation
import FoundationModels

/// Wire-shaped payload mirroring `DictatedSessionDto` exactly. Field names
/// are the JSON contract; they must not change without updating
/// `shared/src/commonMain/.../domain/dictation/dto/DictationDtos.kt`.
@available(iOS 26.0, *)
@Generable
struct DictatedSessionPayload {
    /// One entry per record the transcript expressed.
    @Guide(description: "All records captured in the dictation. Empty array when no record was expressed.")
    var records: [SuggestedRecordPayload] = []
}

@available(iOS 26.0, *)
@Generable
struct SuggestedRecordPayload {
    @Guide(description: "Record kind: \"ultrasound\", \"weight\" or \"deworming\" (lowercase).")
    var recordType: String = ""

    @Guide(description: "Horse name exactly as spoken in the transcript; null when not mentioned.")
    var patientName: String? = nil

    @Guide(description: "ISO-8601 local date yyyy-MM-dd resolved against the provided current date; null when unknown.")
    var date: String? = nil

    @Guide(description: "Measured weight in kilograms (weight records only).")
    var weightKg: Double? = nil

    @Guide(description: "Ovary status description (ultrasound records only).")
    var ovaryStatus: String? = nil

    @Guide(description: "Uterine status description (ultrasound records only).")
    var uterineStatus: String? = nil

    @Guide(description: "Dominant follicle size in millimeters (ultrasound records only).")
    var follicleSizeMm: Double? = nil

    @Guide(description: "Anthelmintic product name (deworming records only).")
    var drugName: String? = nil

    @Guide(description: "Free-form clinical notes from the transcript.")
    var notes: String? = nil
}

/// FoundationModels extraction running directly in the app target.
///
/// Deliberately NOT routed through `FmLlmShim`: `@Generable` types cannot
/// cross the Kotlin cinterop boundary, so the structured generation session
/// lives here and emits the DTO JSON itself.
@available(iOS 26.0, *)
struct FmDictationExtractor: DictationExtracting {
    /// Mirrors the availability check used by the assistant chat lane.
    static var isAvailable: Bool {
        guard #available(iOS 26.0, *) else { return false }
        if case SystemLanguageModel.Availability.available = SystemLanguageModel.default.availability {
            return true
        }
        return false
    }

    private static let instructions = """
    You transcribe Portuguese (Portugal) veterinary dictations about horses \
    into structured records. Today's date is \(Self.todayISO). For each record \
    the speaker mentions, emit one entry with recordType "ultrasound", "weight" \
    or "deworming". Resolve relative dates ("ontem", "hoje") to ISO yyyy-MM-dd. \
    Keep patient names exactly as spoken even when unknown. Fill only fields \
    the transcript expresses; leave everything else null. Never invent values.
    """

    func extract(
        transcript: String,
        onUpdate: ((String) -> Void)?
    ) async throws -> String {
        let session = LanguageModelSession(instructions: Self.instructions)
        let prompt = Prompt(transcript)

        var lastEncoded = ""
        let stream = session.streamResponse(
            to: prompt,
            generating: DictatedSessionPayload.self
        )
        for try await snapshot in stream {
            let encoded = try Self.encode(snapshot.content)
            lastEncoded = encoded
            onUpdate?(encoded)
        }
        return lastEncoded
    }

    // MARK: - JSON encoding

    /// Encodes a streaming snapshot into the DTO JSON contract. Fields the
    /// model has not produced yet are emitted as null.
    static func encode(_ partial: DictatedSessionPayload.PartiallyGenerated) throws -> String {
        try Self.encodeWire(
            records: (partial.records ?? []).map { record in
                WireRecord(
                    recordType: record.recordType ?? "",
                    patientName: record.patientName ?? nil,
                    date: record.date ?? nil,
                    weightKg: record.weightKg ?? nil,
                    ovaryStatus: record.ovaryStatus ?? nil,
                    uterineStatus: record.uterineStatus ?? nil,
                    follicleSizeMm: record.follicleSizeMm ?? nil,
                    drugName: record.drugName ?? nil,
                    notes: record.notes ?? nil
                )
            }
        )
    }

    /// Encodes a completed generation into the exact DTO JSON contract.
    static func encode(_ payload: DictatedSessionPayload) throws -> String {
        try Self.encodeWire(
            records: payload.records.map { record in
                WireRecord(
                    recordType: record.recordType,
                    patientName: record.patientName,
                    date: record.date,
                    weightKg: record.weightKg,
                    ovaryStatus: record.ovaryStatus,
                    uterineStatus: record.uterineStatus,
                    follicleSizeMm: record.follicleSizeMm,
                    drugName: record.drugName,
                    notes: record.notes
                )
            }
        )
    }

    private struct WireRecord: Encodable {
        let recordType: String
        let patientName: String?
        let date: String?
        let weightKg: Double?
        let ovaryStatus: String?
        let uterineStatus: String?
        let follicleSizeMm: Double?
        let drugName: String?
        let notes: String?
    }

    private struct WireSession: Encodable {
        let records: [WireRecord]
    }

    private static func encodeWire(records: [WireRecord]) throws -> String {
        let encoder = JSONEncoder()
        encoder.outputFormatting = [.sortedKeys]
        let data = try encoder.encode(WireSession(records: records))
        return String(decoding: data, as: UTF8.self)
    }

    private static var todayISO: String {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.dateFormat = "yyyy-MM-dd"
        return formatter.string(from: Date())
    }
}
