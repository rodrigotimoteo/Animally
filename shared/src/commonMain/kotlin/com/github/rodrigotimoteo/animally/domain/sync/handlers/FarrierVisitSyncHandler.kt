package com.github.rodrigotimoteo.animally.domain.sync.handlers

import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.domain.farrier.IFarrierVisitRepository
import com.github.rodrigotimoteo.animally.domain.farrier.model.FarrierVisit
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

/** Payload body of a [FarrierVisit] record. `patientId` travels in parentServerIds. */
@Serializable
data class FarrierVisitPayload(
    val date: LocalDate,
    val trimOrShoe: String? = null,
    val shoeType: String? = null,
    val findings: String? = null,
    val nextDueDate: LocalDate? = null,
    val farrier: String? = null,
    val notes: String? = null,
    val createdAt: Instant? = null,
)

/** Push/pull serialization for [FarrierVisit] rows. Parent FK: `patientId` → [PatientSyncHandler]. */
@Single
class FarrierVisitSyncHandler(
    @Provided private val farrierVisitRepository: IFarrierVisitRepository,
    @Provided patientRepository: IPatientRepository,
    @Provided database: AnimallyDatabase,
) : PatientLinkedSyncHandler<FarrierVisitPayload>(patientRepository, database) {
    override val entityType: SyncEntityType = SyncEntityType.FARRIER_VISIT

    override suspend fun buildRecord(
        entityId: Long,
        parentServerIds: Map<String, String?>,
    ): SyncRecord {
        val row =
            farrierVisitRepository.getById(entityId)
                ?: throw NoSuchElementException("FarrierVisit $entityId not found")
        val payloadBody =
            SyncJson
                .encodeToJsonElement(
                    FarrierVisitPayload.serializer(),
                    FarrierVisitPayload(
                        date = row.date,
                        trimOrShoe = row.trimOrShoe,
                        shoeType = row.shoeType,
                        findings = row.findings,
                        nextDueDate = row.nextDueDate,
                        farrier = row.farrier,
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

    override fun decodePayload(r: SyncRecord): FarrierVisitPayload = r.decode(FarrierVisitPayload.serializer())

    override suspend fun serverIdOf(entityId: Long): String? =
        database.farrierVisitQueries
            .selectById(entityId)
            .executeAsOneOrNull()
            ?.serverId

    override suspend fun localIdFor(serverId: String): Long? =
        database.farrierVisitQueries
            .selectByServerId(serverId)
            .executeAsOneOrNull()
            ?.id

    override fun insertRemote(
        record: SyncRecord,
        payload: FarrierVisitPayload,
        serverId: String,
    ): Long {
        val patientId = resolveParentPatientId(record) ?: return ENTITY_NOT_APPLIED
        val newId =
            farrierVisitRepository.insert(
                FarrierVisit(
                    id = 0L,
                    patientId = patientId,
                    date = payload.date,
                    trimOrShoe = payload.trimOrShoe,
                    shoeType = payload.shoeType,
                    findings = payload.findings,
                    nextDueDate = payload.nextDueDate,
                    farrier = payload.farrier,
                    notes = payload.notes,
                    isActive = record.isActive,
                    createdAt = payload.createdAt ?: record.updatedAt,
                    updatedAt = record.updatedAt,
                ),
            )
        database.farrierVisitQueries.setServerId(serverId, record.updatedAt, newId)
        return newId
    }

    override fun applyRemote(
        existingId: Long,
        record: SyncRecord,
        payload: FarrierVisitPayload,
    ): Long {
        val local = farrierVisitRepository.getById(existingId) ?: return ENTITY_NOT_APPLIED
        if (lwwDecision(record, local.updatedAt) == Lww.KEEP) return existingId
        farrierVisitRepository.update(
            FarrierVisit(
                id = existingId,
                patientId = resolveParentPatientId(record) ?: local.patientId,
                date = payload.date,
                trimOrShoe = payload.trimOrShoe,
                shoeType = payload.shoeType,
                findings = payload.findings,
                nextDueDate = payload.nextDueDate,
                farrier = payload.farrier,
                notes = payload.notes,
                isActive = record.isActive,
                createdAt = local.createdAt,
                updatedAt = record.updatedAt,
            ),
        )
        return existingId
    }
}
