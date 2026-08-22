package com.github.rodrigotimoteo.animally.sync.cloudkit

import com.github.rodrigotimoteo.animally.data.sync.SyncStateQueries

/** SyncState keys owned by the CloudKit sync lane. */
public object CloudKitSyncKeys {
    /** Feature flag: `"true"` enables the CloudKit engine. Default OFF. */
    public const val ENABLED: String = "cloud_enabled"

    /** Export cursor: epoch millis of the newest confirmed-exported row. */
    public const val EXPORT_CURSOR: String = "cloud_export_cursor"

    /** Marker cleared on account loss so the next start() does a full re-fetch. */
    public const val ENGINE_STATE: String = "cloud_engine_state"
}

/**
 * Small typed facade over the [SyncStateQueries] key/value table for the
 * CloudKit sync feature flag and persisted engine state.
 */
public class CloudKitSyncSettings(
    private val queries: SyncStateQueries,
) {
    /** Reads the `cloud_enabled` flag; absent or non-`"true"` means disabled. */
    public fun isEnabled(): Boolean = read(CloudKitSyncKeys.ENABLED) == "true"

    /** Writes the `cloud_enabled` flag. */
    public fun setEnabled(value: Boolean) = queries.upsert(CloudKitSyncKeys.ENABLED, value.toString())

    /** Returns the persisted export cursor in epoch millis, or 0 when unset. */
    public fun exportCursorMs(): Long = read(CloudKitSyncKeys.EXPORT_CURSOR)?.toLongOrNull() ?: 0L

    /** Persists the export cursor in epoch millis. */
    public fun setExportCursorMs(epochMs: Long) = queries.upsert(CloudKitSyncKeys.EXPORT_CURSOR, epochMs.toString())

    /**
     * Clears all persisted engine state (cursor + marker). Local data is kept;
     * the next [start][CloudKitSyncEngineImpl.start] re-fetches everything.
     */
    public fun clearEngineState() {
        queries.deleteByKey(CloudKitSyncKeys.EXPORT_CURSOR)
        queries.deleteByKey(CloudKitSyncKeys.ENGINE_STATE)
    }

    private fun read(key: String): String? = queries.selectValueByKey(key).executeAsOneOrNull()
}
