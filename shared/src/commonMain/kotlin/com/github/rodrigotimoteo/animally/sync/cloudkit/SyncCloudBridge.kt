package com.github.rodrigotimoteo.animally.sync.cloudkit

/**
 * Platform bridge to the native CloudKit sync shim.
 *
 * All methods talk JSON: [accountStatus] returns `{"status":"available|..."}`,
 * [stageRecords] takes an array of record envelopes, and events arrive on
 * [setEventHandler]'s callback as JSON strings (see [parseSyncBridgeEvent]).
 */
expect class SyncCloudBridge {
    /** Returns the current iCloud account status JSON. */
    suspend fun accountStatus(): String

    /** Boots the native engine. Idempotent. */
    suspend fun start()

    /** Installs the JSON event callback. Replace-safe. */
    fun setEventHandler(onEvent: (String) -> Unit)

    /** Stages an envelope array for export; confirmation arrives as an event. */
    suspend fun stageRecords(json: String)

    /** Triggers a remote change fetch; results arrive as imported events. */
    suspend fun fetchChanges()

    /** Tears down the native engine. */
    fun stop()
}

/**
 * Creates the platform [SyncCloudBridge]. iOS returns the real shim-backed
 * bridge; every other platform throws on use.
 */
expect fun createSyncCloudBridge(): SyncCloudBridge
