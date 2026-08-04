package com.github.rodrigotimoteo.animally.domain.sync.handlers

import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.domain.patient.IPatientRepository
import com.github.rodrigotimoteo.animally.domain.substance.IControlledSubstanceRepository
import com.github.rodrigotimoteo.animally.domain.substance.model.ControlledSubstance
import com.github.rodrigotimoteo.animally.domain.sync.ENTITY_NOT_APPLIED
import com.github.rodrigotimoteo.animally.domain.sync.SyncEntityType
import com.github.rodrigotimoteo.animally.domain.sync.SyncRecord
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.jsonObject
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single
import kotlin.time.Instant

/** Payload body of a [ControlledSubstance] record. `patientId` travels in parentServerIds. */
@Serializable
data class SubstancePayload(
    val drugName: String,
    val dose: String,
    val unit: String? = null,
    val route: String? = null,
    val administeredBy: String? = null,
    val witness: String? = null,
    val date: LocalDate,
    val reason: String? = null,
    val notes: String? = null,
    val createdAt: Instant? = null,
)

/** Push/pull serialization for [ControlledSubstance] rows. Parent FK: `patientId` → [PatientSyncHandler]. */
@Single
class SubstanceSyncHandler(
    @Provided private val substanceRepository: IControlledSubstanceRepository,
    @Provided patientRepository: IPatientRepository,
    @Provided database: AnimallyDatabase,
) : PatientLinkedSyncHandler<SubstancePayload>(patientRepository, database) {
    override val entityType: SyncEntityType = SyncEntityType.SUBSTANCE

    override suspend fun buildRecord(
        entityId: Long,
        parentServerIds: Map<String, String?>,
    ): SyncRecord {
        val row =
            substanceRepository.getById(entityId)
                ?: throw NoSuchElementException("ControlledSubstance $entityId not found")
        val payloadBody =
            SyncJson
                .encodeToJsonElement(
                    SubstancePayload.serializer(),
                    SubstancePayload(
                        drugName = row.drugName,
                        dose = row.dose,
                        unit = row.unit,
                        route = row.route,
                        administeredBy = row.administeredBy,
                        witness = row.witness,
                        date = row.date,
                        reason = row.reason,
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

    override fun decodePayload(r: SyncRecord): SubstancePayload = r.decode(SubstancePayload.serializer())

    override suspend fun serverIdOf(entityId: Long): String? =
        database.substanceQueries
            .selectById(entityId)
            .executeAsOneOrNull()
            ?.serverId

    override suspend fun localIdFor(serverId: String): Long? =
        database.substanceQueries
            .selectByServerId(serverId)
            .executeAsOneOrNull()
            ?.id

    override fun insertRemote(
        record: SyncRecord,
        payload: SubstancePayload,
        serverId: String,
    ): Long {
        val patientId = resolveParentPatientId(record) ?: return ENTITY_NOT_APPLIED
        val newId =
            substanceRepository.insert(
                ControlledSubstance(
                    id = 0L,
                    patientId = patientId,
                    drugName = payload.drugName,
                    dose = payload.dose,
                    unit = payload.unit,
                    route = payload.route,
                    administeredBy = payload.administeredBy,
                    witness = payload.witness,
                    date = payload.date,
                    reason = payload.reason,
                    notes = payload.notes,
                    isActive = record.isActive,
                    createdAt = payload.createdAt ?: record.updatedAt,
                    updatedAt = record.updatedAt,
                ),
            )
        database.substanceQueries.setServerId(serverId, record.updatedAt, newId)
        return newId
    }

    override fun applyRemote(
        existingId: Long,
        record: SyncRecord,
        payload: SubstancePayload,
    ): Long {
        val local = substanceRepository.getById(existingId) ?: return ENTITY_NOT_APPLIED
        if (lwwDecision(record, local.updatedAt) == Lww.KEEP) return existingId
        substanceRepository.update(
            ControlledSubstance(
                id = existingId,
                patientId = resolveParentPatientId(record) ?: local.patientId,
                drugName = payload.drugName,
                dose = payload.dose,
                unit = payload.unit,
                route = payload.route,
                administeredBy = payload.administeredBy,
                witness = payload.witness,
                date = payload.date,
                reason = payload.reason,
                notes = payload.notes,
                isActive = record.isActive,
                createdAt = local.createdAt,
                updatedAt = record.updatedAt,
            ),
        )
        return existingId
    }
}
