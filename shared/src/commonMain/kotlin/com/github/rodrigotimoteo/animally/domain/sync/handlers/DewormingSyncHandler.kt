package com.github.rodrigotimoteo.animally.domain.sync.handlers

import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.domain.deworming.IDewormingRepository
import com.github.rodrigotimoteo.animally.domain.deworming.model.Deworming
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

/** Payload body of a [Deworming] record. `patientId` travels in parentServerIds. */
@Serializable
data class DewormingPayload(
    val product: String,
    val dateAdministered: LocalDate,
    val nextDueDate: LocalDate? = null,
    val dose: String? = null,
    val vetName: String? = null,
    val notes: String? = null,
    val createdAt: Instant? = null,
)

/** Push/pull serialization for [Deworming] rows. Parent FK: `patientId` → [PatientSyncHandler]. */
@Single
class DewormingSyncHandler(
    @Provided private val dewormingRepository: IDewormingRepository,
    @Provided patientRepository: IPatientRepository,
    @Provided database: AnimallyDatabase,
) : PatientLinkedSyncHandler<DewormingPayload>(patientRepository, database) {
    override val entityType: SyncEntityType = SyncEntityType.DEWORMING

    override suspend fun buildRecord(
        entityId: Long,
        parentServerIds: Map<String, String?>,
    ): SyncRecord {
        val row = dewormingRepository.getById(entityId) ?: throw NoSuchElementException("Deworming $entityId not found")
        val payloadBody =
            SyncJson
                .encodeToJsonElement(
                    DewormingPayload.serializer(),
                    DewormingPayload(
                        product = row.product,
                        dateAdministered = row.dateAdministered,
                        nextDueDate = row.nextDueDate,
                        dose = row.dose,
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

    override fun decodePayload(r: SyncRecord): DewormingPayload = r.decode(DewormingPayload.serializer())

    override suspend fun serverIdOf(entityId: Long): String? =
        database.dewormingQueries
            .selectById(entityId)
            .executeAsOneOrNull()
            ?.serverId

    override suspend fun localIdFor(serverId: String): Long? =
        database.dewormingQueries
            .selectByServerId(serverId)
            .executeAsOneOrNull()
            ?.id

    override fun insertRemote(
        record: SyncRecord,
        payload: DewormingPayload,
        serverId: String,
    ): Long {
        val patientId = resolveParentPatientId(record) ?: return ENTITY_NOT_APPLIED
        val newId =
            dewormingRepository.insert(
                Deworming(
                    id = 0L,
                    patientId = patientId,
                    product = payload.product,
                    dateAdministered = payload.dateAdministered,
                    nextDueDate = payload.nextDueDate,
                    dose = payload.dose,
                    vetName = payload.vetName,
                    notes = payload.notes,
                    isActive = record.isActive,
                    createdAt = payload.createdAt ?: record.updatedAt,
                    updatedAt = record.updatedAt,
                ),
            )
        database.dewormingQueries.setServerId(serverId, record.updatedAt, newId)
        return newId
    }

    override fun applyRemote(
        existingId: Long,
        record: SyncRecord,
        payload: DewormingPayload,
    ): Long {
        val local = dewormingRepository.getById(existingId) ?: return ENTITY_NOT_APPLIED
        if (lwwDecision(record, local.updatedAt) == Lww.KEEP) return existingId
        dewormingRepository.update(
            Deworming(
                id = existingId,
                patientId = resolveParentPatientId(record) ?: local.patientId,
                product = payload.product,
                dateAdministered = payload.dateAdministered,
                nextDueDate = payload.nextDueDate,
                dose = payload.dose,
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
