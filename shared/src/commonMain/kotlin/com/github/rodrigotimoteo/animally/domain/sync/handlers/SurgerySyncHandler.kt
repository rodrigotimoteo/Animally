package com.github.rodrigotimoteo.animally.domain.sync.handlers

import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.domain.patient.IPatientRepository
import com.github.rodrigotimoteo.animally.domain.surgery.ISurgeryRepository
import com.github.rodrigotimoteo.animally.domain.surgery.model.Surgery
import com.github.rodrigotimoteo.animally.domain.sync.ENTITY_NOT_APPLIED
import com.github.rodrigotimoteo.animally.domain.sync.SyncEntityType
import com.github.rodrigotimoteo.animally.domain.sync.SyncRecord
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.jsonObject
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single
import kotlin.time.Instant

/** Payload body of a [Surgery] record. `patientId` travels in parentServerIds. */
@Serializable
data class SurgeryPayload(
    val date: LocalDate,
    val type: String? = null,
    val description: String? = null,
    val outcome: String? = null,
    val surgeon: String? = null,
    val anesthesia: String? = null,
    val analgesia: String? = null,
    val complications: String? = null,
    val recoveryNotes: String? = null,
    val createdAt: Instant? = null,
)

/** Push/pull serialization for [Surgery] rows. Parent FK: `patientId` → [PatientSyncHandler]. */
@Single
class SurgerySyncHandler(
    @Provided private val surgeryRepository: ISurgeryRepository,
    @Provided patientRepository: IPatientRepository,
    @Provided database: AnimallyDatabase,
) : PatientLinkedSyncHandler<SurgeryPayload>(patientRepository, database) {
    override val entityType: SyncEntityType = SyncEntityType.SURGERY

    override suspend fun buildRecord(
        entityId: Long,
        parentServerIds: Map<String, String?>,
    ): SyncRecord {
        val row = surgeryRepository.getById(entityId) ?: throw NoSuchElementException("Surgery $entityId not found")
        val payloadBody =
            SyncJson
                .encodeToJsonElement(
                    SurgeryPayload.serializer(),
                    SurgeryPayload(
                        date = row.date,
                        type = row.type,
                        description = row.description,
                        outcome = row.outcome,
                        surgeon = row.surgeon,
                        anesthesia = row.anesthesia,
                        analgesia = row.analgesia,
                        complications = row.complications,
                        recoveryNotes = row.recoveryNotes,
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

    override fun decodePayload(r: SyncRecord): SurgeryPayload = r.decode(SurgeryPayload.serializer())

    override suspend fun serverIdOf(entityId: Long): String? =
        database.surgeryQueries
            .selectById(entityId)
            .executeAsOneOrNull()
            ?.serverId

    override suspend fun localIdFor(serverId: String): Long? =
        database.surgeryQueries
            .selectByServerId(serverId)
            .executeAsOneOrNull()
            ?.id

    override fun insertRemote(
        record: SyncRecord,
        payload: SurgeryPayload,
        serverId: String,
    ): Long {
        val patientId = resolveParentPatientId(record) ?: return ENTITY_NOT_APPLIED
        val newId =
            surgeryRepository.insert(
                Surgery(
                    id = 0L,
                    patientId = patientId,
                    date = payload.date,
                    type = payload.type,
                    description = payload.description,
                    outcome = payload.outcome,
                    surgeon = payload.surgeon,
                    anesthesia = payload.anesthesia,
                    analgesia = payload.analgesia,
                    complications = payload.complications,
                    recoveryNotes = payload.recoveryNotes,
                    isActive = record.isActive,
                    createdAt = payload.createdAt ?: record.updatedAt,
                    updatedAt = record.updatedAt,
                ),
            )
        database.surgeryQueries.setServerId(serverId, record.updatedAt, newId)
        return newId
    }

    override fun applyRemote(
        existingId: Long,
        record: SyncRecord,
        payload: SurgeryPayload,
    ): Long {
        val local = surgeryRepository.getById(existingId) ?: return ENTITY_NOT_APPLIED
        if (lwwDecision(record, local.updatedAt) == Lww.KEEP) return existingId
        surgeryRepository.update(
            Surgery(
                id = existingId,
                patientId = resolveParentPatientId(record) ?: local.patientId,
                date = payload.date,
                type = payload.type,
                description = payload.description,
                outcome = payload.outcome,
                surgeon = payload.surgeon,
                anesthesia = payload.anesthesia,
                analgesia = payload.analgesia,
                complications = payload.complications,
                recoveryNotes = payload.recoveryNotes,
                isActive = record.isActive,
                createdAt = local.createdAt,
                updatedAt = record.updatedAt,
            ),
        )
        return existingId
    }
}
