package com.github.rodrigotimoteo.animally.domain.sync

import kotlin.time.Instant

/**
 * A row that changed since a given instant, tagged with its entity type for sync lane 2.
 */
data class ChangedRecord(
    val entityType: String,
    val id: Long,
    val updatedAt: Instant,
    val serverId: String?,
    val isActive: Boolean,
)

/**
 * Detects rows updated since a given instant across all synced entity tables.
 */
interface SyncChangeTracker {
    /**
     * Returns every row whose `updatedAt` is newer than [instant], across all entity tables.
     */
    suspend fun recordsChangedSince(instant: Instant): List<ChangedRecord>
}
