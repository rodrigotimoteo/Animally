package com.github.rodrigotimoteo.animally.domain.sync.handlers

import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.domain.gestation.IGestationRepository
import com.github.rodrigotimoteo.animally.domain.gestation.model.Gestation
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

/** Payload body of a [Gestation] record. `patientId` travels in parentServerIds. */
@Serializable
data class GestationPayload(
    val breedingDate: LocalDate,
    val expectedDueDate: LocalDate,
    val gestationDays: Int,
    val status: String? = null,
    val fetalCount: Int? = null,
    val lastCheckDate: LocalDate? = null,
    val notes: String? = null,
    val createdAt: Instant? = null,
)

/** Push/pull serialization for [Gestation] rows. Parent FK: `patientId` → [PatientSyncHandler]. */
@Single
class GestationSyncHandler(
    @Provided private val gestationRepository: IGestationRepository,
    @Provided patientRepository: IPatientRepository,
    @Provided database: AnimallyDatabase,
) : PatientLinkedSyncHandler<GestationPayload>(patientRepository, database) {
    override val entityType: SyncEntityType = SyncEntityType.GESTATION

    override suspend fun buildRecord(
        entityId: Long,
        parentServerIds: Map<String, String?>,
    ): SyncRecord {
        val row = gestationRepository.getById(entityId) ?: throw NoSuchElementException("Gestation $entityId not found")
        val payloadBody =
            SyncJson
                .encodeToJsonElement(
                    GestationPayload.serializer(),
                    GestationPayload(
                        breedingDate = row.breedingDate,
                        expectedDueDate = row.expectedDueDate,
                        gestationDays = row.gestationDays,
                        status = row.status,
                        fetalCount = row.fetalCount,
                        lastCheckDate = row.lastCheckDate,
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

    override fun decodePayload(r: SyncRecord): GestationPayload = r.decode(GestationPayload.serializer())

    override suspend fun serverIdOf(entityId: Long): String? =
        database.gestationQueries
            .selectById(entityId)
            .executeAsOneOrNull()
            ?.serverId

    override suspend fun localIdFor(serverId: String): Long? =
        database.gestationQueries
            .selectByServerId(serverId)
            .executeAsOneOrNull()
            ?.id

    override fun insertRemote(
        record: SyncRecord,
        payload: GestationPayload,
        serverId: String,
    ): Long {
        val patientId = resolveParentPatientId(record) ?: return ENTITY_NOT_APPLIED
        val newId =
            gestationRepository.insert(
                Gestation(
                    id = 0L,
                    patientId = patientId,
                    breedingDate = payload.breedingDate,
                    expectedDueDate = payload.expectedDueDate,
                    gestationDays = payload.gestationDays,
                    status = payload.status.orEmpty(),
                    fetalCount = payload.fetalCount,
                    lastCheckDate = payload.lastCheckDate,
                    notes = payload.notes,
                    isActive = record.isActive,
                    createdAt = payload.createdAt ?: record.updatedAt,
                    updatedAt = record.updatedAt,
                ),
            )
        database.gestationQueries.setServerId(serverId, record.updatedAt, newId)
        return newId
    }

    override fun applyRemote(
        existingId: Long,
        record: SyncRecord,
        payload: GestationPayload,
    ): Long {
        val local = gestationRepository.getById(existingId) ?: return ENTITY_NOT_APPLIED
        if (lwwDecision(record, local.updatedAt) == Lww.KEEP) return existingId
        gestationRepository.update(
            Gestation(
                id = existingId,
                patientId = resolveParentPatientId(record) ?: local.patientId,
                breedingDate = payload.breedingDate,
                expectedDueDate = payload.expectedDueDate,
                gestationDays = payload.gestationDays,
                status = payload.status.orEmpty(),
                fetalCount = payload.fetalCount,
                lastCheckDate = payload.lastCheckDate,
                notes = payload.notes,
                isActive = record.isActive,
                createdAt = local.createdAt,
                updatedAt = record.updatedAt,
            ),
        )
        return existingId
    }
}
