package com.github.rodrigotimoteo.animally.domain.sync.handlers

import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.domain.anamnese.IAnamneseRepository
import com.github.rodrigotimoteo.animally.domain.anamnese.model.Anamnese
import com.github.rodrigotimoteo.animally.domain.patient.IPatientRepository
import com.github.rodrigotimoteo.animally.domain.sync.ENTITY_NOT_APPLIED
import com.github.rodrigotimoteo.animally.domain.sync.SyncEntityType
import com.github.rodrigotimoteo.animally.domain.sync.SyncRecord
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.jsonObject
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single
import kotlin.time.Instant

/**
 * Payload body of an [Anamnese] record.
 *
 * Anamnese is 1:1 with a patient (`patientId` is UNIQUE) — the payload carries
 * no entity body FK; `patientId` travels in parentServerIds like every other
 * patient-linked handler.
 */
@Serializable
data class AnamnesePayload(
    val generalHistory: String? = null,
    val chronicConditions: String? = null,
    val allergies: String? = null,
    val createdAt: Instant? = null,
)

/** Push/pull serialization for [Anamnese] rows. Parent FK: `patientId` → [PatientSyncHandler]. */
@Single
class AnamneseSyncHandler(
    @Provided private val anamneseRepository: IAnamneseRepository,
    @Provided patientRepository: IPatientRepository,
    @Provided database: AnimallyDatabase,
) : PatientLinkedSyncHandler<AnamnesePayload>(patientRepository, database) {
    override val entityType: SyncEntityType = SyncEntityType.ANAMNESE

    override suspend fun buildRecord(
        entityId: Long,
        parentServerIds: Map<String, String?>,
    ): SyncRecord {
        // Anamnese rows have no id-based repo read; locate the row via its patient.
        val patientId =
            anamneseIdToPatientId(entityId)
                ?: throw NoSuchElementException("Anamnese $entityId not found")
        val row =
            anamneseRepository.getByPatient(patientId)
                ?: throw NoSuchElementException("Anamnese $entityId not found")
        val payloadBody =
            SyncJson
                .encodeToJsonElement(
                    AnamnesePayload.serializer(),
                    AnamnesePayload(
                        generalHistory = row.generalHistory,
                        chronicConditions = row.chronicConditions,
                        allergies = row.allergies,
                        createdAt = row.createdAt,
                    ),
                ).jsonObject
        return SyncRecord(
            type = entityType.wireName,
            serverId = serverIdOf(entityId),
            clientId = row.id,
            updatedAt = row.updatedAt,
            isActive = true,
            parentServerIds = parentServerIds.ifEmpty { defaultPatientParent(patientId) },
            payload = payloadBody,
        )
    }

    override fun decodePayload(r: SyncRecord): AnamnesePayload = r.decode(AnamnesePayload.serializer())

    override suspend fun serverIdOf(entityId: Long): String? {
        val patientId = anamneseIdToPatientId(entityId) ?: return null
        return anamneseRepository.getByPatient(patientId)?.serverId
    }

    override suspend fun localIdFor(serverId: String): Long? =
        database.anamneseQueries
            .selectByServerId(serverId)
            .executeAsOneOrNull()
            ?.id

    override fun insertRemote(
        record: SyncRecord,
        payload: AnamnesePayload,
        serverId: String,
    ): Long {
        val patientId = resolveParentPatientId(record) ?: return ENTITY_NOT_APPLIED
        // 1:1 with patient — a locally created, unsynced row may already exist.
        val existing = anamneseRepository.getByPatient(patientId)
        if (existing != null) {
            val id = applyRemote(existing.id, record, payload)
            if (existing.serverId == null) {
                database.anamneseQueries.setServerId(serverId, record.updatedAt, id)
            }
            return id
        }
        val newId =
            anamneseRepository.save(
                Anamnese(
                    id = 0L,
                    patientId = patientId,
                    generalHistory = payload.generalHistory.orEmpty(),
                    chronicConditions = payload.chronicConditions.orEmpty(),
                    allergies = payload.allergies.orEmpty(),
                    createdAt = payload.createdAt ?: record.updatedAt,
                    updatedAt = record.updatedAt,
                ),
            )
        database.anamneseQueries.setServerId(serverId, record.updatedAt, newId)
        return newId
    }

    override fun applyRemote(
        existingId: Long,
        record: SyncRecord,
        payload: AnamnesePayload,
    ): Long {
        val existing = database.anamneseQueries.selectById(existingId).executeAsOneOrNull() ?: return ENTITY_NOT_APPLIED
        if (lwwDecision(record, existing.updatedAt) == Lww.KEEP) return existingId
        val patientId = resolveParentPatientId(record)
        anamneseRepository.save(
            Anamnese(
                id = existingId,
                patientId = patientId ?: existing.patientId,
                generalHistory = payload.generalHistory.orEmpty(),
                chronicConditions = payload.chronicConditions.orEmpty(),
                allergies = payload.allergies.orEmpty(),
                createdAt = existing.createdAt,
                updatedAt = record.updatedAt,
            ),
        )
        return existingId
    }

    /** Anamnese has no `getById` repo read; map an id back through the single-row-per-patient table. */
    private fun anamneseIdToPatientId(entityId: Long): Long? =
        database.anamneseQueries
            .selectAllRows()
            .executeAsList()
            .firstOrNull { it.id == entityId }
            ?.patientId
}
