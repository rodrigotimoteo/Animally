package com.github.rodrigotimoteo.animally.data.sync

import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.domain.sync.SyncMetadataRepository
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single
import kotlin.time.Instant

/**
 * SQLDelight-backed [SyncMetadataRepository] over the single-row sync metadata table.
 */
@Single(binds = [SyncMetadataRepository::class])
class SyncMetadataRepositoryImpl(
    @Provided private val database: AnimallyDatabase,
) : SyncMetadataRepository {
    private val syncMetadataQueries: SyncMetadataQueries = database.syncMetadataQueries

    override suspend fun getOrCreateLastSyncAt(deviceId: String): Instant {
        syncMetadataQueries.getMetadata().executeAsOneOrNull()?.let { return it.lastSyncAt }
        syncMetadataQueries.insertMetadata(Instant.fromEpochMilliseconds(0), deviceId)
        return Instant.fromEpochMilliseconds(0)
    }

    override suspend fun updateLastSyncAt(instant: Instant) {
        val existing = syncMetadataQueries.getMetadata().executeAsOneOrNull()
        if (existing != null) {
            syncMetadataQueries.updateLastSyncAt(instant, existing.id)
            return
        }
        syncMetadataQueries.insertMetadata(instant, getDeviceId())
    }

    override suspend fun getDeviceId(): String = syncMetadataQueries.getMetadata().executeAsOneOrNull()?.deviceId ?: ""

    override suspend fun saveDeviceId(deviceId: String) {
        val lastSyncAt =
            syncMetadataQueries.getMetadata().executeAsOneOrNull()?.lastSyncAt
                ?: Instant.fromEpochMilliseconds(0)
        syncMetadataQueries.deleteAll()
        syncMetadataQueries.insertMetadata(lastSyncAt, deviceId)
    }
}
