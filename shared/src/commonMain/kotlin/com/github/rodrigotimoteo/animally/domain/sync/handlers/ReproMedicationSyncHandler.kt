package com.github.rodrigotimoteo.animally.domain.sync.handlers

import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.domain.patient.IPatientRepository
import com.github.rodrigotimoteo.animally.domain.repromedication.IReproMedicationRepository
import com.github.rodrigotimoteo.animally.domain.repromedication.model.ReproMedication
import com.github.rodrigotimoteo.animally.domain.sync.ENTITY_NOT_APPLIED
import com.github.rodrigotimoteo.animally.domain.sync.SyncEntityType
import com.github.rodrigotimoteo.animally.domain.sync.SyncRecord
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.jsonObject
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single
import kotlin.time.Instant

/** Payload body of a [ReproMedication] record. `patientId` travels in parentServerIds. */
@Serializable
data class ReproMedicationPayload(
    val medication: String,
    val dateAdministered: LocalDate,
    val dosage: String? = null,
    val purpose: String? = null,
    val vetName: String? = null,
    val notes: String? = null,
    val createdAt: Instant? = null,
)

/** Push/pull serialization for [ReproMedication] rows. Parent FK: `patientId` → [PatientSyncHandler]. */
@Single
class ReproMedicationSyncHandler(
    @Provided private val reproMedicationRepository: IReproMedicationRepository,
    @Provided patientRepository: IPatientRepository,
    @Provided database: AnimallyDatabase,
) : PatientLinkedSyncHandler<ReproMedicationPayload>(patientRepository, database) {
    override val entityType: SyncEntityType = SyncEntityType.REPRO_MEDICATION

    override suspend fun buildRecord(
        entityId: Long,
        parentServerIds: Map<String, String?>,
    ): SyncRecord {
        val row =
            reproMedicationRepository.getById(entityId)
                ?: throw NoSuchElementException("ReproMedication $entityId not found")
        val payloadBody =
            SyncJson
                .encodeToJsonElement(
                    ReproMedicationPayload.serializer(),
                    ReproMedicationPayload(
                        medication = row.medication,
                        dateAdministered = row.dateAdministered,
                        dosage = row.dosage,
                        purpose = row.purpose,
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

    override fun decodePayload(r: SyncRecord): ReproMedicationPayload = r.decode(ReproMedicationPayload.serializer())

    override suspend fun serverIdOf(entityId: Long): String? =
        database.reproMedicationQueries
            .selectById(entityId)
            .executeAsOneOrNull()
            ?.serverId

    override suspend fun localIdFor(serverId: String): Long? =
        database.reproMedicationQueries
            .selectByServerId(serverId)
            .executeAsOneOrNull()
            ?.id

    override fun insertRemote(
        record: SyncRecord,
        payload: ReproMedicationPayload,
        serverId: String,
    ): Long {
        val patientId = resolveParentPatientId(record) ?: return ENTITY_NOT_APPLIED
        val newId =
            reproMedicationRepository.insert(
                ReproMedication(
                    id = 0L,
                    patientId = patientId,
                    medication = payload.medication,
                    dateAdministered = payload.dateAdministered,
                    dosage = payload.dosage,
                    purpose = payload.purpose,
                    vetName = payload.vetName,
                    notes = payload.notes,
                    isActive = record.isActive,
                    createdAt = payload.createdAt ?: record.updatedAt,
                    updatedAt = record.updatedAt,
                ),
            )
        database.reproMedicationQueries.setServerId(serverId, record.updatedAt, newId)
        return newId
    }

    override fun applyRemote(
        existingId: Long,
        record: SyncRecord,
        payload: ReproMedicationPayload,
    ): Long {
        val local = reproMedicationRepository.getById(existingId) ?: return ENTITY_NOT_APPLIED
        if (lwwDecision(record, local.updatedAt) == Lww.KEEP) return existingId
        reproMedicationRepository.update(
            ReproMedication(
                id = existingId,
                patientId = resolveParentPatientId(record) ?: local.patientId,
                medication = payload.medication,
                dateAdministered = payload.dateAdministered,
                dosage = payload.dosage,
                purpose = payload.purpose,
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
