package com.github.rodrigotimoteo.animally.domain.sync.handlers

import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.domain.embryotransfer.IEmbryoTransferRepository
import com.github.rodrigotimoteo.animally.domain.embryotransfer.model.EmbryoTransfer
import com.github.rodrigotimoteo.animally.domain.patient.IPatientRepository
import com.github.rodrigotimoteo.animally.domain.sync.ENTITY_NOT_APPLIED
import com.github.rodrigotimoteo.animally.domain.sync.SyncEntityType
import com.github.rodrigotimoteo.animally.domain.sync.SyncRecord
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.jsonObject
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single
import kotlin.time.Instant

/** Payload body of an [EmbryoTransfer] record. `patientId` travels in parentServerIds. */
@Serializable
data class EmbryoTransferPayload(
    val date: LocalDate,
    val embryoCount: Int = 0,
    val recipientMares: String? = null,
    val vetName: String? = null,
    val notes: String? = null,
    val createdAt: Instant? = null,
)

/** Push/pull serialization for [EmbryoTransfer] rows. Parent FK: `patientId` → [PatientSyncHandler]. */
@Single
class EmbryoTransferSyncHandler(
    @Provided private val repository: IEmbryoTransferRepository,
    @Provided patientRepository: IPatientRepository,
    @Provided database: AnimallyDatabase,
) : PatientLinkedSyncHandler<EmbryoTransferPayload>(patientRepository, database) {
    override val entityType: SyncEntityType = SyncEntityType.EMBRYO_TRANSFER

    override suspend fun buildRecord(
        entityId: Long,
        parentServerIds: Map<String, String?>,
    ): SyncRecord {
        val row =
            repository.getById(entityId) ?: throw NoSuchElementException("EmbryoTransfer $entityId not found")
        val payloadBody =
            SyncJson
                .encodeToJsonElement(
                    EmbryoTransferPayload.serializer(),
                    EmbryoTransferPayload(
                        date = row.date,
                        embryoCount = row.embryoCount,
                        recipientMares = row.recipientMares,
                        vetName = row.vetName,
                        notes = row.notes,
                        createdAt = row.createdAt,
                    ),
                ).jsonObject
        return SyncRecord(
            type = entityType.wireName,
            serverId = serverIdOf(entityId),
            clientId = entityId,
            updatedAt = row.updatedAt,
            isActive = row.isActive,
            parentServerIds = parentServerIds.ifEmpty { defaultPatientParent(row.patientId) },
            payload = payloadBody,
        )
    }

    override fun decodePayload(r: SyncRecord): EmbryoTransferPayload = r.decode(EmbryoTransferPayload.serializer())

    override suspend fun serverIdOf(entityId: Long): String? =
        database.embryoTransferQueries
            .selectById(entityId)
            .executeAsOneOrNull()
            ?.serverId

    override suspend fun localIdFor(serverId: String): Long? =
        database.embryoTransferQueries
            .selectByServerId(serverId)
            .executeAsOneOrNull()
            ?.id

    override fun insertRemote(
        record: SyncRecord,
        payload: EmbryoTransferPayload,
        serverId: String,
    ): Long {
        val patientId = resolveParentPatientId(record) ?: return ENTITY_NOT_APPLIED
        val newId =
            repository.insert(
                EmbryoTransfer(
                    id = 0L,
                    patientId = patientId,
                    date = payload.date,
                    embryoCount = payload.embryoCount,
                    recipientMares = payload.recipientMares,
                    vetName = payload.vetName,
                    notes = payload.notes,
                    isActive = record.isActive,
                    createdAt = payload.createdAt ?: record.updatedAt,
                    updatedAt = record.updatedAt,
                ),
            )
        database.embryoTransferQueries.setServerId(serverId, record.updatedAt, newId)
        return newId
    }

    override fun applyRemote(
        existingId: Long,
        record: SyncRecord,
        payload: EmbryoTransferPayload,
    ): Long {
        val local = repository.getById(existingId) ?: return ENTITY_NOT_APPLIED
        if (lwwDecision(record, local.updatedAt) == Lww.KEEP) return existingId
        repository.update(
            EmbryoTransfer(
                id = existingId,
                patientId = resolveParentPatientId(record) ?: local.patientId,
                date = payload.date,
                embryoCount = payload.embryoCount,
                recipientMares = payload.recipientMares,
                vetName = payload.vetName,
                notes = payload.notes,
                isActive = record.isActive,
                createdAt = local.createdAt,
                updatedAt = record.updatedAt,
            ),
        )
        return existingId
    }
}
