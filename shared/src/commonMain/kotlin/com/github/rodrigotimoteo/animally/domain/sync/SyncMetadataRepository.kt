package com.github.rodrigotimoteo.animally.domain.sync

import kotlin.time.Instant

/**
 * Repository contract for the single-row sync metadata table used by sync lane 2.
 */
interface SyncMetadataRepository {
    /**
     * Returns the stored last-synced timestamp for [deviceId], inserting an
     * epoch-zero row when none exists yet.
     */
    suspend fun getOrCreateLastSyncAt(deviceId: String): Instant

    /**
     * Persists [instant] as the new last-synced timestamp, inserting a fresh row
     * first when none exists yet.
     */
    suspend fun updateLastSyncAt(instant: Instant)

    /**
     * Returns the stored device id, or `""` when no row exists.
     */
    suspend fun getDeviceId(): String

    /**
     * Stores [deviceId], preserving the current last-synced timestamp.
     */
    suspend fun saveDeviceId(deviceId: String)
}
