package com.github.rodrigotimoteo.animally.data.sync.handlers

import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.data.follicle.mapper.toDomain
import com.github.rodrigotimoteo.animally.domain.follicle.IFollicleRepository
import com.github.rodrigotimoteo.animally.domain.follicle.model.Follicle
import com.github.rodrigotimoteo.animally.domain.sync.ENTITY_NOT_APPLIED
import com.github.rodrigotimoteo.animally.domain.sync.SyncEntityType
import com.github.rodrigotimoteo.animally.domain.sync.SyncRecord
import com.github.rodrigotimoteo.animally.domain.sync.handlers.SyncJson
import com.github.rodrigotimoteo.animally.domain.sync.handlers.UltrasoundLinkedSyncHandler
import com.github.rodrigotimoteo.animally.domain.sync.handlers.decode
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.jsonObject
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single
import kotlin.time.Instant

/** Payload body of a [Follicle] record. `ultrasoundId` travels in the parent map. */
@Serializable
data class FolliclePayload(
    val side: String,
    val sizeMm: Double,
    val description: String? = null,
    val createdAt: Instant? = null,
)

/** Push/pull serialization for [Follicle] rows. Parent FK: `ultrasoundId`. */
@Single
class FollicleSyncHandler(
    @Provided private val repository: IFollicleRepository,
    @Provided database: AnimallyDatabase,
) : UltrasoundLinkedSyncHandler<FolliclePayload>(database) {
    override val entityType: SyncEntityType = SyncEntityType.FOLLICLE

    override suspend fun buildRecord(
        entityId: Long,
        parentServerIds: Map<String, String?>,
    ): SyncRecord {
        // Read the raw row so a soft-deleted follicle can still produce a
        // tombstone; the repository's public getById intentionally returns
        // active rows only.
        val row =
            database.follicleQueries
                .selectRowById(entityId)
                .executeAsOneOrNull()
                ?.toDomain()
                ?: throw NoSuchElementException("Follicle $entityId not found")
        val payloadBody =
            SyncJson
                .encodeToJsonElement(
                    FolliclePayload.serializer(),
                    FolliclePayload(
                        side = row.side,
                        sizeMm = row.sizeMm,
                        description = row.description,
                        createdAt = row.createdAt,
                    ),
                ).jsonObject
        return SyncRecord(
            type = entityType.wireName,
            serverId = row.serverId,
            clientId = entityId,
            updatedAt = row.updatedAt,
            isActive = row.isActive,
            parentServerIds = parentServerIds.ifEmpty { defaultUltrasoundParent(row.ultrasoundId) },
            payload = payloadBody,
        )
    }

    override fun decodePayload(record: SyncRecord): FolliclePayload = record.decode(FolliclePayload.serializer())

    override suspend fun serverIdOf(entityId: Long): String? =
        database.follicleQueries
            .selectRowById(entityId)
            .executeAsOneOrNull()
            ?.serverId

    override suspend fun localIdFor(serverId: String): Long? =
        database.follicleQueries
            .selectByServerId(serverId)
            .executeAsOneOrNull()
            ?.id

    override fun insertRemote(
        record: SyncRecord,
        payload: FolliclePayload,
        serverId: String,
    ): Long {
        val ultrasoundId = resolveParentUltrasoundId(record) ?: return ENTITY_NOT_APPLIED
        val newId =
            repository.insert(
                Follicle(
                    id = 0L,
                    ultrasoundId = ultrasoundId,
                    side = payload.side,
                    sizeMm = payload.sizeMm,
                    description = payload.description,
                    isActive = record.isActive,
                    createdAt = payload.createdAt ?: record.updatedAt,
                    updatedAt = record.updatedAt,
                ),
            )
        database.follicleQueries.setServerId(serverId, record.updatedAt, newId)
        return newId
    }

    override fun applyRemote(
        existingId: Long,
        record: SyncRecord,
        payload: FolliclePayload,
    ): Long {
        val local =
            database.follicleQueries
                .selectRowById(existingId)
                .executeAsOneOrNull()
                ?.toDomain()
                ?: return ENTITY_NOT_APPLIED
        if (lwwDecision(record, local.updatedAt) == Lww.KEEP) return existingId
        repository.update(
            Follicle(
                id = existingId,
                ultrasoundId = resolveParentUltrasoundId(record) ?: local.ultrasoundId,
                side = payload.side,
                sizeMm = payload.sizeMm,
                description = payload.description,
                isActive = record.isActive,
                createdAt = local.createdAt,
                updatedAt = record.updatedAt,
            ),
        )
        return existingId
    }
}
