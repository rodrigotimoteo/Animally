package com.github.rodrigotimoteo.animally.domain.sync.handlers

import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.domain.owner.IOwnerRepository
import com.github.rodrigotimoteo.animally.domain.owner.model.Owner
import com.github.rodrigotimoteo.animally.domain.sync.ENTITY_NOT_APPLIED
import com.github.rodrigotimoteo.animally.domain.sync.SyncEntityType
import com.github.rodrigotimoteo.animally.domain.sync.SyncRecord
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.jsonObject
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single
import kotlin.time.Instant

/**
 * Payload body of an [Owner] record. Excludes `id`/`serverId` (carried by the
 * [SyncRecord] envelope) and lifecycle timestamps (envelope carries
 * `updatedAt`/`isActive`; only [createdAt] travels here).
 */
@Serializable
data class OwnerPayload(
    val name: String,
    val email: String? = null,
    val phone: String? = null,
    val address: String? = null,
    val createdAt: Instant? = null,
)

/** Push/pull serialization for [Owner] rows. No parent FK — top of the sync order. */
@Single
class OwnerSyncHandler(
    @Provided private val ownerRepository: IOwnerRepository,
    @Provided database: AnimallyDatabase,
) : EntitySyncHandler(database) {
    override val entityType: SyncEntityType = SyncEntityType.OWNER

    override suspend fun buildRecord(
        entityId: Long,
        parentServerIds: Map<String, String?>,
    ): SyncRecord {
        val row = ownerRepository.getOwnerById(entityId) ?: throw NoSuchElementException("Owner $entityId not found")
        val payloadBody =
            SyncJson
                .encodeToJsonElement(
                    OwnerPayload.serializer(),
                    OwnerPayload(
                        name = row.name,
                        email = row.email,
                        phone = row.phone,
                        address = row.address,
                        createdAt = row.createdAt,
                    ),
                ).jsonObject
        return SyncRecord(
            type = entityType.wireName,
            serverId = serverIdOf(entityId),
            clientId = entityId,
            updatedAt = row.updatedAt,
            isActive = row.isActive,
            payload = payloadBody,
        )
    }

    override suspend fun applyRecord(record: SyncRecord): Long {
        val serverId = record.serverId ?: return ENTITY_NOT_APPLIED
        val existingId = localIdFor(serverId)
        val payload = SyncJson.decodeFromJsonElement(OwnerPayload.serializer(), record.payload)
        return if (existingId == null) {
            insertRemote(record, payload, serverId)
        } else {
            applyRemote(existingId, record, payload)
        }
    }

    override suspend fun serverIdOf(entityId: Long): String? =
        database.ownerQueries
            .selectById(entityId)
            .executeAsOneOrNull()
            ?.serverId

    override suspend fun localIdFor(serverId: String): Long? =
        database.ownerQueries
            .selectByServerId(serverId)
            .executeAsOneOrNull()
            ?.id

    private fun insertRemote(
        record: SyncRecord,
        payload: OwnerPayload,
        serverId: String,
    ): Long {
        val newId =
            ownerRepository.insertOwner(
                Owner(
                    id = 0L,
                    name = payload.name,
                    email = payload.email,
                    phone = payload.phone,
                    address = payload.address,
                    isActive = record.isActive,
                    createdAt = payload.createdAt ?: record.updatedAt,
                    updatedAt = record.updatedAt,
                ),
            )
        database.ownerQueries.setServerId(serverId, record.updatedAt, newId)
        return newId
    }

    private fun applyRemote(
        existingId: Long,
        record: SyncRecord,
        payload: OwnerPayload,
    ): Long {
        val local = ownerRepository.getOwnerById(existingId) ?: return ENTITY_NOT_APPLIED
        if (lwwDecision(record, local.updatedAt) == Lww.KEEP) return existingId
        ownerRepository.updateOwner(
            Owner(
                id = existingId,
                name = payload.name,
                email = payload.email,
                phone = payload.phone,
                address = payload.address,
                isActive = record.isActive,
                createdAt = local.createdAt,
                updatedAt = record.updatedAt,
            ),
        )
        return existingId
    }
}
