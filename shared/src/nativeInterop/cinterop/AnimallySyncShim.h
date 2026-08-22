#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

/// Swift-facing bridge to CloudKit sync (CKSyncEngine).
///
/// Only `NSString` + block callbacks cross the cinterop boundary; every CloudKit type stays
/// inside the Swift implementation (see `AnimallySyncShim.swift`). The Kotlin side calls this
/// through a header-only cinterop binding. The explicit ObjC class name in Swift
/// (`@objc(AnimallySyncShim)`) must match this `@interface` or linking fails.
@interface AnimallySyncShim : NSObject

- (instancetype)init NS_DESIGNATED_INITIALIZER;

/// Checks the iCloud account status for the app's container.
/// `completion` is invoked exactly once with (json, error): on success `json` is a JSON object
/// of the form {"status":"available|noAccount|restricted|couldNotDetermine|temporarilyUnavailable"}
/// and `error` is nil; on failure `json` is nil and `error` describes the failure.
- (void)accountStatus:(void (^)(NSString *_Nullable json, NSString *_Nullable error))completion;

/// Starts the sync engine: creates the custom zone "animallyZone" if needed and initializes
/// CKSyncEngine with persisted state (UserDefaults key "animally_ck_engine_state"; a state
/// blob that fails to decode is discarded and recreated). Idempotent — subsequent calls are
/// no-ops. On success `completion` receives (nil, nil); on failure (nil, error).
- (void)start:(void (^)(NSString *_Nullable result, NSString *_Nullable error))completion;

/// Registers the single event handler. All engine events are funneled through it as one JSON
/// string per event:
///   {"type":"accountChange","available":true|false}
///   {"type":"imported","records":[<envelope>,...]}
///   {"type":"exported","names":["uuid",...]}
///   {"type":"exportFailed","names":["uuid",...],"error":"..."}
/// Envelope shape: {"recordType":...,"recordName":...,"updatedAt":ms,"isActive":0|1,
///                  "parents":{"patientId":"uuid"},"body":"<json>"}
- (void)setEventHandler:(void (^)(NSString *eventJson))handler;

/// Stages outbound records for export. `json` is a JSON array of envelopes (shape above).
/// Each envelope is converted to a CKRecord immediately, added to the engine's pending record
/// zone changes, and kept in an in-memory staged pool that the next
/// `nextRecordZoneBatch` drain consumes synchronously.
/// `completion` receives (nil, nil) on success or (nil, error) on malformed input.
- (void)stageRecords:(NSString *)json
          completion:(void (^)(NSString *_Nullable result, NSString *_Nullable error))completion;

/// Manually triggers a change fetch from the server.
/// `completion` receives ("ok", nil) on success or (nil, error) on failure.
- (void)fetchChanges:(void (^)(NSString *_Nullable result, NSString *_Nullable error))completion;

/// Stops the engine and tears down observers. Safe to call repeatedly.
- (void)stop;

@end

NS_ASSUME_NONNULL_END
