import Foundation
import CloudKit

/// Swift bridge between Kotlin (via header-only cinterop) and CloudKit's
/// CKSyncEngine. Only NSString + block callbacks cross the boundary; every
/// CloudKit type stays in this file.
///
/// Design notes:
/// - Outbound rows are STAGED from Kotlin (`stageRecords`) into an in-memory
///   pool; `nextRecordZoneChangeBatch` builds its batch from that pool
///   synchronously because the delegate callback cannot await a round-trip
///   into Kotlin.
/// - Engine state (`stateSerialization`) is persisted to UserDefaults on every
///   state update; an undecodable blob is discarded at boot (the apply path is
///   idempotent upsert-by-recordName, so a full re-fetch is safe).
/// - The free developer profile has no push subscriptions, so change fetches
///   are driven by explicit `fetchChanges` calls from Kotlin (polling).
@objc(AnimallySyncShim)
final class AnimallySyncShim: NSObject {

    private static let containerId = "iCloud.com.github.rodrigotimoteo.animally"
    private static let zoneName = "animallyZone"
    private static let stateKey = "animally_ck_engine_state"

    private let container: CKContainer
    private var engine: CKSyncEngine?
    private var eventHandler: ((String) -> Void)?
    /// recordName -> CKRecord, awaiting the next batch build.
    private var stagedRecords: [String: CKRecord] = [:]
    private var started = false
    private let bootLock = NSLock()

    override private init() {
        self.container = CKContainer(identifier: Self.containerId)
        super.init()
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(accountDidChange),
            name: .CKAccountChanged,
            object: nil
        )
    }

    // MARK: - Kotlin-facing surface (mirrors AnimallySyncShim.h)

    @objc func accountStatus(_ completion: @escaping (String?, String?) -> Void) {
        container.accountStatus { status, error in
            if let error {
                completion(nil, error.localizedDescription)
                return
            }
            let name: String
            switch status {
            case .available: name = "available"
            case .noAccount: name = "noAccount"
            case .restricted: name = "restricted"
            case .couldNotDetermine: name = "couldNotDetermine"
            case .temporarilyUnavailable: name = "temporarilyUnavailable"
            @unknown default: name = "couldNotDetermine"
            }
            completion("{\"status\":\"\(name)\"}", nil)
        }
    }

    @objc func start(_ completion: @escaping (String?, String?) -> Void) {
        bootLock.lock()
        if started {
            bootLock.unlock()
            completion("ok", nil)
            return
        }
        started = true
        bootLock.unlock()

        var configuration = CKSyncEngine.Configuration(
            database: container.privateCloudDatabase,
            stateSerialization: Self.loadState(),
            delegate: self
        )
        configuration.automaticallySync = true
        engine = CKSyncEngine(configuration)
        // Zone creation rides the engine's own pending database changes.
        let zone = CKRecordZone(zoneName: Self.zoneName)
        engine?.state.add(pendingDatabaseChanges: [.saveZone(zone)])
        completion("ok", nil)
    }

    @objc func setEventHandler(_ handler: @escaping (String) -> Void) {
        eventHandler = handler
    }

    @objc func stageRecords(_ json: String, completion: @escaping (String?, String?) -> Void) {
        guard let data = json.data(using: .utf8),
              let envelopes = try? JSONSerialization.jsonObject(with: data) as? [[String: Any]] else {
            completion(nil, "malformed stage payload")
            return
        }
        var pendingSaves: [CKSyncEngine.PendingRecordZoneChange] = []
        for envelope in envelopes {
            guard let record = makeRecord(from: envelope) else { continue }
            stagedRecords[record.recordID.recordName] = record
            pendingSaves.append(.saveRecord(record.recordID))
        }
        engine?.state.add(pendingRecordZoneChanges: pendingSaves)
        completion("ok", nil)
    }

    @objc func fetchChanges(_ completion: @escaping (String?, String?) -> Void) {
        Task {
            do {
                try await engine?.fetchChanges(CKSyncEngine.FetchChangesOptions())
                completion("ok", nil)
            } catch {
                completion(nil, error.localizedDescription)
            }
        }
    }

    @objc func stop() {
        engine = nil
        stagedRecords.removeAll()
        UserDefaults.standard.removeObject(forKey: Self.stateKey)
        started = false
    }

    // MARK: - Account changes

    @objc private func accountDidChange() {
        container.accountStatus { [weak self] status, _ in
            self?.emit([
                "type": "accountChange",
                "available": status == .available
            ])
        }
    }

    // MARK: - Event emission

    private func emit(_ payload: [String: Any]) {
        guard let data = try? JSONSerialization.data(withJSONObject: payload),
              let json = String(data: data, encoding: .utf8) else { return }
        eventHandler?(json)
    }

    // MARK: - Envelope conversion

    /// Converts a fetched CKRecord back into the generic row envelope consumed
    /// by the Kotlin import pipeline.
    private func convertToEnvelope(_ record: CKRecord) -> [String: Any] {
        var body: [String: Any] = [:]
        var parents: [String: Any] = [:]
        for key in record.allKeys() {
            guard let value = record[key] else { continue }
            if key.hasSuffix("ParentId") {
                parents[String(key.dropLast("ParentId".count))] = value
            } else if key != "updatedAt" && key != "isActive" {
                body[key] = value
            }
        }
        return [
            "recordType": record.recordType,
            "recordName": record.recordID.recordName,
            "updatedAt": record["updatedAt"] ?? 0,
            "isActive": record["isActive"] ?? 1,
            "parents": parents,
            "body": body
        ]
    }

    /// Builds a CKRecord from a Kotlin-produced envelope. Body keys become
    /// top-level record fields; parent ids flatten as "<key>ParentId".
    private func makeRecord(from envelope: [String: Any]) -> CKRecord? {
        guard let recordType = envelope["recordType"] as? String,
              let recordName = envelope["recordName"] as? String else { return nil }
        let zoneId = CKRecordZone.ID(zoneName: Self.zoneName, ownerName: CKCurrentUserDefaultName)
        let recordId = CKRecord.ID(recordName: recordName, zoneID: zoneId)
        let record = CKRecord(recordType: recordType, recordID: recordId)

        if let updatedAt = envelope["updatedAt"] as? NSNumber { record["updatedAt"] = updatedAt }
        if let isActive = envelope["isActive"] as? NSNumber { record["isActive"] = isActive }
        if let parents = envelope["parents"] as? [String: String] {
            for (key, value) in parents { record["\(key)ParentId"] = value }
        }
        if let bodyJson = envelope["body"] as? String,
           let body = try? JSONSerialization.jsonObject(with: Data(bodyJson.utf8)) as? [String: Any] {
            for (key, value) in body where key != "id" {
                record[key] = normalize(value)
            }
        }
        return record
    }

    /// Maps JSON values onto CKRecord-compatible types (NSNumber/String).
    private func normalize(_ value: Any) -> (any __CKRecordObjCValue)? {
        switch value {
        case let number as NSNumber: return number
        case let string as String: return string as NSString
        case is NSNull: return "" as NSString // nullable columns round-trip as empty strings
        default: return String(describing: value) as NSString
        }
    }

    // MARK: - State persistence

    private static func loadState() -> CKSyncEngine.State.Serialization? {
        guard let data = UserDefaults.standard.data(forKey: stateKey) else { return nil }
        return try? JSONDecoder().decode(CKSyncEngine.State.Serialization.self, from: data)
    }

    private static func persistState(_ serialization: CKSyncEngine.State.Serialization?) {
        guard let serialization,
              let data = try? JSONEncoder().encode(serialization) else { return }
        UserDefaults.standard.set(data, forKey: stateKey)
    }
}

// MARK: - CKSyncEngineDelegate

extension AnimallySyncShim: CKSyncEngineDelegate {

    func handleEvent(_ event: CKSyncEngine.Event, syncEngine: CKSyncEngine) async {
        switch event {
        case .stateUpdate(let stateUpdate):
            Self.persistState(stateUpdate.stateSerialization)

        case .accountChange(let accountChange):
            container.accountStatus { [weak self] status, _ in
                self?.emit([
                    "type": "accountChange",
                    "available": status == .available
                ])
            }

        case .fetchedRecordZoneChanges(let changes):
            let envelopes = changes.modifications.map { convertToEnvelope($0.record) }
            emit(["type": "imported", "records": envelopes])

        case .sentRecordZoneChanges(let sent):
            let savedNames = sent.savedRecords.map(\.recordID.recordName)
            for name in savedNames { stagedRecords.removeValue(forKey: name) }
            if !savedNames.isEmpty {
                emit(["type": "exported", "names": savedNames])
            }
            for failure in sent.failedRecordSaves {
                // Re-stage failures: same recordName makes the retry an idempotent overwrite.
                emit([
                    "type": "exportFailed",
                    "names": [failure.record.recordID.recordName],
                    "error": failure.error.localizedDescription
                ])
            }

        default:
            break
        }
    }

    func nextRecordZoneChangeBatch(
        _ context: CKSyncEngine.SendChangesContext,
        syncEngine: CKSyncEngine
    ) async -> CKSyncEngine.RecordZoneChangeBatch? {
        // Synchronous drain from the staged pool — the reason outbound rows
        // are staged eagerly instead of fetched on demand.
        guard !stagedRecords.isEmpty else { return nil }
        let pending: [CKSyncEngine.PendingRecordZoneChange] =
            stagedRecords.values.map { .saveRecord($0.recordID) }
        return try await CKSyncEngine.RecordZoneChangeBatch(
            pendingChanges: pending,
            recordProvider: { [weak self] recordId in
                self?.stagedRecords[recordId.recordName]
            }
        )
    }
}
