package com.github.rodrigotimoteo.animally.domain.sync.handlers

import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.domain.lameness.ILamenessRepository
import com.github.rodrigotimoteo.animally.domain.lameness.model.Lameness
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

/** Payload body of a [Lameness] record. `patientId` travels in parentServerIds. */
@Serializable
data class LamenessPayload(
    val date: LocalDate,
    val gradeAAEP: Int,
    val limbLocation: String? = null,
    val flexionTest: String? = null,
    val diagnosis: String? = null,
    val treatment: String? = null,
    val vetName: String? = null,
    val notes: String? = null,
    val createdAt: Instant? = null,
)

/** Push/pull serialization for [Lameness] rows. Parent FK: `patientId` → [PatientSyncHandler]. */
@Single
class LamenessSyncHandler(
    @Provided private val lamenessRepository: ILamenessRepository,
    @Provided patientRepository: IPatientRepository,
    @Provided database: AnimallyDatabase,
) : PatientLinkedSyncHandler<LamenessPayload>(patientRepository, database) {
    override val entityType: SyncEntityType = SyncEntityType.LAMENESS

    override suspend fun buildRecord(
        entityId: Long,
        parentServerIds: Map<String, String?>,
    ): SyncRecord {
        val row = lamenessRepository.getById(entityId) ?: throw NoSuchElementException("Lameness $entityId not found")
        val payloadBody =
            SyncJson
                .encodeToJsonElement(
                    LamenessPayload.serializer(),
                    LamenessPayload(
                        date = row.date,
                        gradeAAEP = row.gradeAAEP,
                        limbLocation = row.limbLocation,
                        flexionTest = row.flexionTest,
                        diagnosis = row.diagnosis,
                        treatment = row.treatment,
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

    override fun decodePayload(r: SyncRecord): LamenessPayload = r.decode(LamenessPayload.serializer())

    override suspend fun serverIdOf(entityId: Long): String? =
        database.lamenessQueries
            .selectById(entityId)
            .executeAsOneOrNull()
            ?.serverId

    override suspend fun localIdFor(serverId: String): Long? =
        database.lamenessQueries
            .selectByServerId(serverId)
            .executeAsOneOrNull()
            ?.id

    override fun insertRemote(
        record: SyncRecord,
        payload: LamenessPayload,
        serverId: String,
    ): Long {
        val patientId = resolveParentPatientId(record) ?: return ENTITY_NOT_APPLIED
        val newId =
            lamenessRepository.insert(
                Lameness(
                    id = 0L,
                    patientId = patientId,
                    date = payload.date,
                    gradeAAEP = payload.gradeAAEP,
                    limbLocation = payload.limbLocation,
                    flexionTest = payload.flexionTest,
                    diagnosis = payload.diagnosis,
                    treatment = payload.treatment,
                    vetName = payload.vetName,
                    notes = payload.notes,
                    isActive = record.isActive,
                    createdAt = payload.createdAt ?: record.updatedAt,
                    updatedAt = record.updatedAt,
                ),
            )
        database.lamenessQueries.setServerId(serverId, record.updatedAt, newId)
        return newId
    }

    override fun applyRemote(
        existingId: Long,
        record: SyncRecord,
        payload: LamenessPayload,
    ): Long {
        val local = lamenessRepository.getById(existingId) ?: return ENTITY_NOT_APPLIED
        if (lwwDecision(record, local.updatedAt) == Lww.KEEP) return existingId
        lamenessRepository.update(
            Lameness(
                id = existingId,
                patientId = resolveParentPatientId(record) ?: local.patientId,
                date = payload.date,
                gradeAAEP = payload.gradeAAEP,
                limbLocation = payload.limbLocation,
                flexionTest = payload.flexionTest,
                diagnosis = payload.diagnosis,
                treatment = payload.treatment,
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
