import Foundation

/// Canned extractor used on devices without FoundationModels (e.g. the
/// simulator without Apple Intelligence).
///
/// Returns three records that deliberately exercise every review path:
/// 1. A clean weight entry (validation `Ok`).
/// 2. A deworming entry with an unparseable date (silently becomes today)
///    and an over-long drug name (truncated + flagged).
/// 3. An ultrasound for a patient name that matches nothing (quarantined
///    by patient resolution) with an implausible follicle size (flagged).
struct MockDictationExtractor: DictationExtracting {
    /// Simulated extraction latency so the transcribing state is visible.
    private let latency: TimeInterval

    init(latency: TimeInterval = 1.2) {
        self.latency = latency
    }

    func extract(
        transcript: String,
        onUpdate: ((String) -> Void)?
    ) async throws -> String {
        try await Task.sleep(nanoseconds: UInt64(latency * 1_000_000_000))
        return Self.cannedSessionJSON
    }
}

extension MockDictationExtractor {
    static let cannedSessionJSON = """
    {
      "records": [
        {
          "recordType": "weight",
          "patientName": "Cometa",
          "date": "2026-08-20",
          "weightKg": 512.0,
          "notes": "peso antes da dose"
        },
        {
          "recordType": "deworming",
          "patientName": "Cometa",
          "date": "1999-13-45",
          "drugName": "Ivermectina Comprimido Palatável para Equinos de Grande Porte Formulação Estendida",
          "notes": "dose única administrada pela manhã"
        },
        {
          "recordType": "ultrasound",
          "patientName": "Fantasma Inexistente",
          "date": "2026-08-24",
          "ovaryStatus": "ovário direito com folículo dominante",
          "uterineStatus": "edema uterino grau 2",
          "follicleSizeMm": 120.0
        }
      ]
    }
    """
}
