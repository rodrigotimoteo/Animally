package com.github.rodrigotimoteo.animally.domain.sync.handlers

import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.domain.patient.IPatientRepository
import com.github.rodrigotimoteo.animally.domain.sync.ENTITY_NOT_APPLIED
import com.github.rodrigotimoteo.animally.domain.sync.SyncEntityType
import com.github.rodrigotimoteo.animally.domain.sync.SyncRecord
import com.github.rodrigotimoteo.animally.domain.vaccination.IVaccinationRepository
import com.github.rodrigotimoteo.animally.domain.vaccination.model.Vaccination
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.jsonObject
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single
import kotlin.time.Instant

/** Payload body of a [Vaccination] record. `patientId` travels in parentServerIds. */
@Serializable
data class VaccinationPayload(
    val vaccineName: String,
    val dateAdministered: LocalDate,
    val nextDueDate: LocalDate? = null,
    val vetName: String? = null,
    val batchNumber: String? = null,
    val site: String? = null,
    val notes: String? = null,
    val createdAt: Instant? = null,
)

/** Push/pull serialization for [Vaccination] rows. Parent FK: `patientId` → [PatientSyncHandler]. */
@Single
class VaccinationSyncHandler(
    @Provided private val vaccinationRepository: IVaccinationRepository,
    @Provided patientRepository: IPatientRepository,
    @Provided database: AnimallyDatabase,
) : PatientLinkedSyncHandler<VaccinationPayload>(patientRepository, database) {
    override val entityType: SyncEntityType = SyncEntityType.VACCINATION

    override suspend fun buildRecord(
        entityId: Long,
        parentServerIds: Map<String, String?>,
    ): SyncRecord {
        val row =
            vaccinationRepository.getById(entityId)
                ?: throw NoSuchElementException("Vaccination $entityId not found")
        val payloadBody =
            SyncJson
                .encodeToJsonElement(
                    VaccinationPayload.serializer(),
                    VaccinationPayload(
                        vaccineName = row.vaccineName,
                        dateAdministered = row.dateAdministered,
                        nextDueDate = row.nextDueDate,
                        vetName = row.vetName,
                        batchNumber = row.batchNumber,
                        site = row.site,
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

    override fun decodePayload(r: SyncRecord): VaccinationPayload = r.decode(VaccinationPayload.serializer())

    override suspend fun serverIdOf(entityId: Long): String? =
        database.vaccinationQueries
            .selectById(entityId)
            .executeAsOneOrNull()
            ?.serverId

    override suspend fun localIdFor(serverId: String): Long? =
        database.vaccinationQueries
            .selectByServerId(serverId)
            .executeAsOneOrNull()
            ?.id

    override fun insertRemote(
        record: SyncRecord,
        payload: VaccinationPayload,
        serverId: String,
    ): Long {
        val patientId = resolveParentPatientId(record) ?: return ENTITY_NOT_APPLIED
        val newId =
            vaccinationRepository.insert(
                Vaccination(
                    id = 0L,
                    patientId = patientId,
                    vaccineName = payload.vaccineName,
                    dateAdministered = payload.dateAdministered,
                    nextDueDate = payload.nextDueDate,
                    vetName = payload.vetName,
                    batchNumber = payload.batchNumber,
                    site = payload.site,
                    notes = payload.notes,
                    isActive = record.isActive,
                    createdAt = payload.createdAt ?: record.updatedAt,
                    updatedAt = record.updatedAt,
                ),
            )
        database.vaccinationQueries.setServerId(serverId, record.updatedAt, newId)
        return newId
    }

    override fun applyRemote(
        existingId: Long,
        record: SyncRecord,
        payload: VaccinationPayload,
    ): Long {
        val local = vaccinationRepository.getById(existingId) ?: return ENTITY_NOT_APPLIED
        if (lwwDecision(record, local.updatedAt) == Lww.KEEP) return existingId
        vaccinationRepository.update(
            Vaccination(
                id = existingId,
                patientId = resolveParentPatientId(record) ?: local.patientId,
                vaccineName = payload.vaccineName,
                dateAdministered = payload.dateAdministered,
                nextDueDate = payload.nextDueDate,
                vetName = payload.vetName,
                batchNumber = payload.batchNumber,
                site = payload.site,
                notes = payload.notes,
                isActive = record.isActive,
                createdAt = local.createdAt,
                updatedAt = record.updatedAt,
            ),
        )
        return existingId
    }
}
