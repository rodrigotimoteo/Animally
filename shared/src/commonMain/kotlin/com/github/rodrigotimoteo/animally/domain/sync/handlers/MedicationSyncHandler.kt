package com.github.rodrigotimoteo.animally.domain.sync.handlers

import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.domain.medication.IMedicationRepository
import com.github.rodrigotimoteo.animally.domain.medication.model.Medication
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

/** Payload body of a [Medication] record. `patientId` travels in parentServerIds. */
@Serializable
data class MedicationPayload(
    val name: String,
    val dosage: String,
    val route: String? = null,
    val frequency: String? = null,
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val prescribedBy: String? = null,
    val notes: String? = null,
    val createdAt: Instant? = null,
)

/** Push/pull serialization for [Medication] rows. Parent FK: `patientId` → [PatientSyncHandler]. */
@Single
class MedicationSyncHandler(
    @Provided private val medicationRepository: IMedicationRepository,
    @Provided patientRepository: IPatientRepository,
    @Provided database: AnimallyDatabase,
) : PatientLinkedSyncHandler<MedicationPayload>(patientRepository, database) {
    override val entityType: SyncEntityType = SyncEntityType.MEDICATION

    override suspend fun buildRecord(
        entityId: Long,
        parentServerIds: Map<String, String?>,
    ): SyncRecord {
        val row =
            medicationRepository.getById(entityId)
                ?: throw NoSuchElementException("Medication $entityId not found")
        val payloadBody =
            SyncJson
                .encodeToJsonElement(
                    MedicationPayload.serializer(),
                    MedicationPayload(
                        name = row.name,
                        dosage = row.dosage,
                        route = row.route,
                        frequency = row.frequency,
                        startDate = row.startDate,
                        endDate = row.endDate,
                        prescribedBy = row.prescribedBy,
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

    override fun decodePayload(r: SyncRecord): MedicationPayload = r.decode(MedicationPayload.serializer())

    override suspend fun serverIdOf(entityId: Long): String? =
        database.medicationQueries
            .selectById(entityId)
            .executeAsOneOrNull()
            ?.serverId

    override suspend fun localIdFor(serverId: String): Long? =
        database.medicationQueries
            .selectByServerId(serverId)
            .executeAsOneOrNull()
            ?.id

    override fun insertRemote(
        record: SyncRecord,
        payload: MedicationPayload,
        serverId: String,
    ): Long {
        val patientId = resolveParentPatientId(record) ?: return ENTITY_NOT_APPLIED
        val newId =
            medicationRepository.insert(
                Medication(
                    id = 0L,
                    patientId = patientId,
                    name = payload.name,
                    dosage = payload.dosage,
                    route = payload.route,
                    frequency = payload.frequency,
                    startDate = payload.startDate,
                    endDate = payload.endDate,
                    prescribedBy = payload.prescribedBy,
                    notes = payload.notes,
                    isActive = record.isActive,
                    createdAt = payload.createdAt ?: record.updatedAt,
                    updatedAt = record.updatedAt,
                ),
            )
        database.medicationQueries.setServerId(serverId, record.updatedAt, newId)
        return newId
    }

    override fun applyRemote(
        existingId: Long,
        record: SyncRecord,
        payload: MedicationPayload,
    ): Long {
        val local = medicationRepository.getById(existingId) ?: return ENTITY_NOT_APPLIED
        if (lwwDecision(record, local.updatedAt) == Lww.KEEP) return existingId
        medicationRepository.update(
            Medication(
                id = existingId,
                patientId = resolveParentPatientId(record) ?: local.patientId,
                name = payload.name,
                dosage = payload.dosage,
                route = payload.route,
                frequency = payload.frequency,
                startDate = payload.startDate,
                endDate = payload.endDate,
                prescribedBy = payload.prescribedBy,
                notes = payload.notes,
                isActive = record.isActive,
                createdAt = local.createdAt,
                updatedAt = record.updatedAt,
            ),
        )
        return existingId
    }
}
