package com.github.rodrigotimoteo.animally.domain.sync.handlers

import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.domain.dentistry.IDentistryRepository
import com.github.rodrigotimoteo.animally.domain.dentistry.model.Dentistry
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

/** Payload body of a [Dentistry] record. `patientId` travels in parentServerIds. */
@Serializable
data class DentistryPayload(
    val date: LocalDate,
    val findings: String? = null,
    val treatment: String? = null,
    val nextDueDate: LocalDate? = null,
    val vetName: String? = null,
    val notes: String? = null,
    val createdAt: Instant? = null,
)

/** Push/pull serialization for [Dentistry] rows. Parent FK: `patientId` → [PatientSyncHandler]. */
@Single
class DentistrySyncHandler(
    @Provided private val dentistryRepository: IDentistryRepository,
    @Provided patientRepository: IPatientRepository,
    @Provided database: AnimallyDatabase,
) : PatientLinkedSyncHandler<DentistryPayload>(patientRepository, database) {
    override val entityType: SyncEntityType = SyncEntityType.DENTISTRY

    override suspend fun buildRecord(
        entityId: Long,
        parentServerIds: Map<String, String?>,
    ): SyncRecord {
        val row = dentistryRepository.getById(entityId) ?: throw NoSuchElementException("Dentistry $entityId not found")
        val payloadBody =
            SyncJson
                .encodeToJsonElement(
                    DentistryPayload.serializer(),
                    DentistryPayload(
                        date = row.date,
                        findings = row.findings,
                        treatment = row.treatment,
                        nextDueDate = row.nextDueDate,
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

    override fun decodePayload(r: SyncRecord): DentistryPayload = r.decode(DentistryPayload.serializer())

    override suspend fun serverIdOf(entityId: Long): String? =
        database.dentistryQueries
            .selectById(entityId)
            .executeAsOneOrNull()
            ?.serverId

    override suspend fun localIdFor(serverId: String): Long? =
        database.dentistryQueries
            .selectByServerId(serverId)
            .executeAsOneOrNull()
            ?.id

    override fun insertRemote(
        record: SyncRecord,
        payload: DentistryPayload,
        serverId: String,
    ): Long {
        val patientId = resolveParentPatientId(record) ?: return ENTITY_NOT_APPLIED
        val newId =
            dentistryRepository.insert(
                Dentistry(
                    id = 0L,
                    patientId = patientId,
                    date = payload.date,
                    findings = payload.findings,
                    treatment = payload.treatment,
                    nextDueDate = payload.nextDueDate,
                    vetName = payload.vetName,
                    notes = payload.notes,
                    isActive = record.isActive,
                    createdAt = payload.createdAt ?: record.updatedAt,
                    updatedAt = record.updatedAt,
                ),
            )
        database.dentistryQueries.setServerId(serverId, record.updatedAt, newId)
        return newId
    }

    override fun applyRemote(
        existingId: Long,
        record: SyncRecord,
        payload: DentistryPayload,
    ): Long {
        val local = dentistryRepository.getById(existingId) ?: return ENTITY_NOT_APPLIED
        if (lwwDecision(record, local.updatedAt) == Lww.KEEP) return existingId
        dentistryRepository.update(
            Dentistry(
                id = existingId,
                patientId = resolveParentPatientId(record) ?: local.patientId,
                date = payload.date,
                findings = payload.findings,
                treatment = payload.treatment,
                nextDueDate = payload.nextDueDate,
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
