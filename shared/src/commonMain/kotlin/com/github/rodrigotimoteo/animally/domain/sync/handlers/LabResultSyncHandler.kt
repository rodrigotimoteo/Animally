package com.github.rodrigotimoteo.animally.domain.sync.handlers

import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.domain.labresult.ILabResultRepository
import com.github.rodrigotimoteo.animally.domain.labresult.model.LabResult
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

/** Payload body of a [LabResult] record. `patientId` travels in parentServerIds. */
@Serializable
data class LabResultPayload(
    val testType: String,
    val date: LocalDate,
    val results: String? = null,
    val normalRange: String? = null,
    val vetName: String? = null,
    val notes: String? = null,
    val createdAt: Instant? = null,
)

/** Push/pull serialization for [LabResult] rows. Parent FK: `patientId` → [PatientSyncHandler]. */
@Single
class LabResultSyncHandler(
    @Provided private val labResultRepository: ILabResultRepository,
    @Provided patientRepository: IPatientRepository,
    @Provided database: AnimallyDatabase,
) : PatientLinkedSyncHandler<LabResultPayload>(patientRepository, database) {
    override val entityType: SyncEntityType = SyncEntityType.LAB_RESULT

    override suspend fun buildRecord(
        entityId: Long,
        parentServerIds: Map<String, String?>,
    ): SyncRecord {
        val row = labResultRepository.getById(entityId) ?: throw NoSuchElementException("LabResult $entityId not found")
        val payloadBody =
            SyncJson
                .encodeToJsonElement(
                    LabResultPayload.serializer(),
                    LabResultPayload(
                        testType = row.testType,
                        date = row.date,
                        results = row.results,
                        normalRange = row.normalRange,
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

    override fun decodePayload(r: SyncRecord): LabResultPayload = r.decode(LabResultPayload.serializer())

    override suspend fun serverIdOf(entityId: Long): String? =
        database.labResultQueries
            .selectById(entityId)
            .executeAsOneOrNull()
            ?.serverId

    override suspend fun localIdFor(serverId: String): Long? =
        database.labResultQueries
            .selectByServerId(serverId)
            .executeAsOneOrNull()
            ?.id

    override fun insertRemote(
        record: SyncRecord,
        payload: LabResultPayload,
        serverId: String,
    ): Long {
        val patientId = resolveParentPatientId(record) ?: return ENTITY_NOT_APPLIED
        val newId =
            labResultRepository.insert(
                LabResult(
                    id = 0L,
                    patientId = patientId,
                    testType = payload.testType,
                    date = payload.date,
                    results = payload.results,
                    normalRange = payload.normalRange,
                    vetName = payload.vetName,
                    notes = payload.notes,
                    isActive = record.isActive,
                    createdAt = payload.createdAt ?: record.updatedAt,
                    updatedAt = record.updatedAt,
                ),
            )
        database.labResultQueries.setServerId(serverId, record.updatedAt, newId)
        return newId
    }

    override fun applyRemote(
        existingId: Long,
        record: SyncRecord,
        payload: LabResultPayload,
    ): Long {
        val local = labResultRepository.getById(existingId) ?: return ENTITY_NOT_APPLIED
        if (lwwDecision(record, local.updatedAt) == Lww.KEEP) return existingId
        labResultRepository.update(
            LabResult(
                id = existingId,
                patientId = resolveParentPatientId(record) ?: local.patientId,
                testType = payload.testType,
                date = payload.date,
                results = payload.results,
                normalRange = payload.normalRange,
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
