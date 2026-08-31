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
    private let syncState = SyncState()

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
        Task { [container] in
            do {
                let status = try await container.accountStatus()
                completion("{\"status\":\"\(Self.accountStatusName(status))\"}", nil)
            } catch {
                completion(nil, error.localizedDescription)
            }
        }
    }

    @objc func start(_ completion: @escaping (String?, String?) -> Void) {
        guard let engine =
            syncState.startEngine(
                container: container,
                stateSerialization: Self.loadState(),
                delegate: self,
            )
        else {
            completion("ok", nil)
            return
        }
        // Zone creation rides the engine's own pending database changes.
        let zone = CKRecordZone(zoneName: Self.zoneName)
        engine.state.add(pendingDatabaseChanges: [.saveZone(zone)])
        completion("ok", nil)
    }

    @objc func setEventHandler(_ handler: @escaping (String) -> Void) {
        syncState.setEventHandler(handler)
    }

    @objc func stageRecords(_ json: String, completion: @escaping (String?, String?) -> Void) {
        guard let data = json.data(using: .utf8),
              let envelopes = try? JSONSerialization.jsonObject(with: data) as? [[String: Any]] else {
            completion(nil, "malformed stage payload")
            return
        }
        let (engine, pendingSaves) = syncState.stage(envelopes) { [weak self] envelope in
            self?.makeRecord(from: envelope)
        }
        engine?.state.add(pendingRecordZoneChanges: pendingSaves)
        completion("ok", nil)
    }

    @objc func fetchChanges(_ completion: @escaping (String?, String?) -> Void) {
        Task {
            do {
                try await syncState.engine?.fetchChanges(CKSyncEngine.FetchChangesOptions())
                completion("ok", nil)
            } catch {
                completion(nil, error.localizedDescription)
            }
        }
    }

    @objc func stop() {
        syncState.stop()
        UserDefaults.standard.removeObject(forKey: Self.stateKey)
    }

    // MARK: - Account changes

    @objc private func accountDidChange() {
        emitAccountStatus()
    }

    private func emitAccountStatus() {
        Task { [weak self, container] in
            guard let self else { return }
            do {
                let status = try await container.accountStatus()
                self.emit([
                    "type": "accountChange",
                    "available": status == .available,
                ])
            } catch {
                self.emit([
                    "type": "accountChange",
                    "available": false,
                    "error": error.localizedDescription,
                ])
            }
        }
    }

    // MARK: - Event emission

    private func emit(_ payload: [String: Any]) {
        guard let data = try? JSONSerialization.data(withJSONObject: payload),
              let json = String(data: data, encoding: .utf8) else { return }
        syncState.eventHandler?(json)
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

    private static func accountStatusName(_ status: CKAccountStatus) -> String {
        switch status {
        case .available: return "available"
        case .noAccount: return "noAccount"
        case .restricted: return "restricted"
        case .couldNotDetermine: return "couldNotDetermine"
        case .temporarilyUnavailable: return "temporarilyUnavailable"
        @unknown default: return "couldNotDetermine"
        }
    }

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

/// Synchronizes all mutable CloudKit bridge state shared by Objective-C entry
/// points and CKSyncEngine's Sendable delegate callbacks.
private final class SyncState: @unchecked Sendable {
    private let lock = NSLock()
    private var engineValue: CKSyncEngine?
    private var eventHandlerValue: ((String) -> Void)?
    /// recordName -> CKRecord, awaiting the next batch build.
    private var stagedRecords: [String: CKRecord] = [:]
    private var started = false

    var engine: CKSyncEngine? {
        withLock { engineValue }
    }

    var eventHandler: ((String) -> Void)? {
        withLock { eventHandlerValue }
    }

    func setEventHandler(_ handler: @escaping (String) -> Void) {
        withLock { eventHandlerValue = handler }
    }

    func startEngine(
        container: CKContainer,
        stateSerialization: CKSyncEngine.State.Serialization?,
        delegate: AnimallySyncShim,
    ) -> CKSyncEngine? {
        withLock {
            guard !started else { return nil }
            started = true
            var configuration = CKSyncEngine.Configuration(
                database: container.privateCloudDatabase,
                stateSerialization: stateSerialization,
                delegate: delegate,
            )
            configuration.automaticallySync = true
            let engine = CKSyncEngine(configuration)
            engineValue = engine
            return engine
        }
    }

    func stage(
        _ envelopes: [[String: Any]],
        makeRecord: ([String: Any]) -> CKRecord?,
    ) -> (engine: CKSyncEngine?, pending: [CKSyncEngine.PendingRecordZoneChange]) {
        withLock {
            var pending: [CKSyncEngine.PendingRecordZoneChange] = []
            for envelope in envelopes {
                guard let record = makeRecord(envelope) else { continue }
                stagedRecords[record.recordID.recordName] = record
                pending.append(.saveRecord(record.recordID))
            }
            return (engineValue, pending)
        }
    }

    func stop() {
        withLock {
            engineValue = nil
            stagedRecords.removeAll()
            eventHandlerValue = nil
            started = false
        }
    }

    func removeStagedRecords(named names: [String]) {
        withLock {
            for name in names {
                stagedRecords.removeValue(forKey: name)
            }
        }
    }

    func pendingStagedChanges() -> [CKSyncEngine.PendingRecordZoneChange] {
        withLock { stagedRecords.values.map { .saveRecord($0.recordID) } }
    }

    func stagedRecord(named name: String) -> CKRecord? {
        withLock { stagedRecords[name] }
    }

    private func withLock<T>(_ body: () -> T) -> T {
        lock.lock()
        defer { lock.unlock() }
        return body()
    }
}

// MARK: - CKSyncEngineDelegate

extension AnimallySyncShim: CKSyncEngineDelegate {

    func handleEvent(_ event: CKSyncEngine.Event, syncEngine: CKSyncEngine) async {
        switch event {
        case .stateUpdate(let stateUpdate):
            Self.persistState(stateUpdate.stateSerialization)

        case .accountChange:
            emitAccountStatus()

        case .fetchedRecordZoneChanges(let changes):
            let envelopes = changes.modifications.map { convertToEnvelope($0.record) }
            emit(["type": "imported", "records": envelopes])

        case .sentRecordZoneChanges(let sent):
            let savedNames = sent.savedRecords.map(\.recordID.recordName)
            syncState.removeStagedRecords(named: savedNames)
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
        let pending = syncState.pendingStagedChanges()
        guard !pending.isEmpty else { return nil }
        return await CKSyncEngine.RecordZoneChangeBatch(
            pendingChanges: pending,
            recordProvider: { [weak self] recordId in
                self?.syncState.stagedRecord(named: recordId.recordName)
            }
        )
    }
}
