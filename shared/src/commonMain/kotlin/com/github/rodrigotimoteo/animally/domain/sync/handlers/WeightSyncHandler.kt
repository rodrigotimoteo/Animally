package com.github.rodrigotimoteo.animally.domain.sync.handlers

import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.domain.patient.IPatientRepository
import com.github.rodrigotimoteo.animally.domain.sync.ENTITY_NOT_APPLIED
import com.github.rodrigotimoteo.animally.domain.sync.SyncEntityType
import com.github.rodrigotimoteo.animally.domain.sync.SyncRecord
import com.github.rodrigotimoteo.animally.domain.weight.IWeightRepository
import com.github.rodrigotimoteo.animally.domain.weight.model.Weight
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.jsonObject
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single
import kotlin.time.Instant

/** Payload body of a [Weight] record. `patientId` travels in parentServerIds. */
@Serializable
data class WeightPayload(
    val weightKg: Double,
    val date: LocalDate,
    val notes: String? = null,
    val createdAt: Instant? = null,
)

/** Push/pull serialization for [Weight] rows. Parent FK: `patientId` → [PatientSyncHandler]. */
@Single
class WeightSyncHandler(
    @Provided private val weightRepository: IWeightRepository,
    @Provided patientRepository: IPatientRepository,
    @Provided database: AnimallyDatabase,
) : PatientLinkedSyncHandler<WeightPayload>(patientRepository, database) {
    override val entityType: SyncEntityType = SyncEntityType.WEIGHT

    override suspend fun buildRecord(
        entityId: Long,
        parentServerIds: Map<String, String?>,
    ): SyncRecord {
        val row = weightRepository.getById(entityId) ?: throw NoSuchElementException("Weight $entityId not found")
        val payloadBody =
            SyncJson
                .encodeToJsonElement(
                    WeightPayload.serializer(),
                    WeightPayload(
                        weightKg = row.weightKg,
                        date = row.date,
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

    override fun decodePayload(r: SyncRecord): WeightPayload = r.decode(WeightPayload.serializer())

    override suspend fun serverIdOf(entityId: Long): String? =
        database.weightQueries
            .selectById(entityId)
            .executeAsOneOrNull()
            ?.serverId

    override suspend fun localIdFor(serverId: String): Long? =
        database.weightQueries
            .selectByServerId(serverId)
            .executeAsOneOrNull()
            ?.id

    override fun insertRemote(
        record: SyncRecord,
        payload: WeightPayload,
        serverId: String,
    ): Long {
        val patientId = resolveParentPatientId(record) ?: return ENTITY_NOT_APPLIED
        val newId =
            weightRepository.insert(
                Weight(
                    id = 0L,
                    patientId = patientId,
                    weightKg = payload.weightKg,
                    date = payload.date,
                    notes = payload.notes,
                    isActive = record.isActive,
                    createdAt = payload.createdAt ?: record.updatedAt,
                    updatedAt = record.updatedAt,
                ),
            )
        database.weightQueries.setServerId(serverId, record.updatedAt, newId)
        return newId
    }

    override fun applyRemote(
        existingId: Long,
        record: SyncRecord,
        payload: WeightPayload,
    ): Long {
        val local = weightRepository.getById(existingId) ?: return ENTITY_NOT_APPLIED
        if (lwwDecision(record, local.updatedAt) == Lww.KEEP) return existingId
        weightRepository.update(
            Weight(
                id = existingId,
                patientId = resolveParentPatientId(record) ?: local.patientId,
                weightKg = payload.weightKg,
                date = payload.date,
                notes = payload.notes,
                isActive = record.isActive,
                createdAt = local.createdAt,
                updatedAt = record.updatedAt,
            ),
        )
        return existingId
    }
}
