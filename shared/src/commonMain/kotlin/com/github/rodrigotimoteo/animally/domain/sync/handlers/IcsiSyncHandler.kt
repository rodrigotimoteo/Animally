package com.github.rodrigotimoteo.animally.domain.sync.handlers

import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.domain.icsi.IIcsiRepository
import com.github.rodrigotimoteo.animally.domain.icsi.model.Icsi
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

/** Payload body of an [Icsi] record. `patientId` travels in parentServerIds. */
@Serializable
data class IcsiPayload(
    val date: LocalDate,
    val folliclesRecovered: Int = 0,
    val vetName: String? = null,
    val notes: String? = null,
    val createdAt: Instant? = null,
)

/** Push/pull serialization for [Icsi] rows. Parent FK: `patientId` → [PatientSyncHandler]. */
@Single
class IcsiSyncHandler(
    @Provided private val repository: IIcsiRepository,
    @Provided patientRepository: IPatientRepository,
    @Provided database: AnimallyDatabase,
) : PatientLinkedSyncHandler<IcsiPayload>(patientRepository, database) {
    override val entityType: SyncEntityType = SyncEntityType.ICSI

    override suspend fun buildRecord(
        entityId: Long,
        parentServerIds: Map<String, String?>,
    ): SyncRecord {
        val row = repository.getById(entityId) ?: throw NoSuchElementException("Icsi $entityId not found")
        val payloadBody =
            SyncJson
                .encodeToJsonElement(
                    IcsiPayload.serializer(),
                    IcsiPayload(
                        date = row.date,
                        folliclesRecovered = row.folliclesRecovered,
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

    override fun decodePayload(r: SyncRecord): IcsiPayload = r.decode(IcsiPayload.serializer())

    override suspend fun serverIdOf(entityId: Long): String? =
        database.icsiQueries
            .selectById(entityId)
            .executeAsOneOrNull()
            ?.serverId

    override suspend fun localIdFor(serverId: String): Long? =
        database.icsiQueries
            .selectByServerId(serverId)
            .executeAsOneOrNull()
            ?.id

    override fun insertRemote(
        record: SyncRecord,
        payload: IcsiPayload,
        serverId: String,
    ): Long {
        val patientId = resolveParentPatientId(record) ?: return ENTITY_NOT_APPLIED
        val newId =
            repository.insert(
                Icsi(
                    id = 0L,
                    patientId = patientId,
                    date = payload.date,
                    folliclesRecovered = payload.folliclesRecovered,
                    vetName = payload.vetName,
                    notes = payload.notes,
                    isActive = record.isActive,
                    createdAt = payload.createdAt ?: record.updatedAt,
                    updatedAt = record.updatedAt,
                ),
            )
        database.icsiQueries.setServerId(serverId, record.updatedAt, newId)
        return newId
    }

    override fun applyRemote(
        existingId: Long,
        record: SyncRecord,
        payload: IcsiPayload,
    ): Long {
        val local = repository.getById(existingId) ?: return ENTITY_NOT_APPLIED
        if (lwwDecision(record, local.updatedAt) == Lww.KEEP) return existingId
        repository.update(
            Icsi(
                id = existingId,
                patientId = resolveParentPatientId(record) ?: local.patientId,
                date = payload.date,
                folliclesRecovered = payload.folliclesRecovered,
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
