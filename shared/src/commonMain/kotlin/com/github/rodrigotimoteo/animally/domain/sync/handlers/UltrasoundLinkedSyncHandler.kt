package com.github.rodrigotimoteo.animally.domain.sync.handlers

import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.domain.sync.ENTITY_NOT_APPLIED
import com.github.rodrigotimoteo.animally.domain.sync.SyncRecord

/**
 * Base for records whose required parent is an ultrasound rather than a
 * patient. Keeping this relationship separate prevents Follicle records from
 * being incorrectly treated as direct patient children during sync ordering.
 */
abstract class UltrasoundLinkedSyncHandler<P>(
    database: AnimallyDatabase,
) : EntitySyncHandler(database) {
    /** Decodes the entity body from [record.payload]. */
    protected abstract fun decodePayload(record: SyncRecord): P

    /** Inserts a remote record and stamps its server id. */
    protected abstract fun insertRemote(
        record: SyncRecord,
        payload: P,
        serverId: String,
    ): Long

    /** Applies a remote record over an existing local row. */
    protected abstract fun applyRemote(
        existingId: Long,
        record: SyncRecord,
        payload: P,
    ): Long

    /** Resolves the required ultrasound parent, or null when it is not synced. */
    protected fun resolveParentUltrasoundId(record: SyncRecord): Long? {
        val serverId = record.parentServerIds["ultrasoundId"] ?: return null
        return database.ultrasoundQueries
            .selectByServerId(serverId)
            .executeAsOneOrNull()
            ?.id
    }

    /** Builds the required parent mapping for a local record. */
    protected fun defaultUltrasoundParent(ultrasoundId: Long): Map<String, String?> =
        mapOf(
            "ultrasoundId" to
                database.ultrasoundQueries
                    .selectRowById(ultrasoundId)
                    .executeAsOneOrNull()
                    ?.serverId,
        )

    override suspend fun applyRecord(record: SyncRecord): Long {
        val serverId = record.serverId ?: return ENTITY_NOT_APPLIED
        val existingId = localIdFor(serverId)
        val payload = decodePayload(record)
        return if (existingId == null) {
            insertRemote(record, payload, serverId)
        } else {
            applyRemote(existingId, record, payload)
        }
    }
}
