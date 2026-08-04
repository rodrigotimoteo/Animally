package com.github.rodrigotimoteo.animally.domain.sync.handlers

import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.domain.consultation.IConsultationRepository
import com.github.rodrigotimoteo.animally.domain.consultation.model.Consultation
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

/** Payload body of a [Consultation] record. `patientId` travels in parentServerIds. */
@Serializable
data class ConsultationPayload(
    val date: LocalDate,
    val subjective: String,
    val objective: String,
    val assessment: String,
    val plan: String,
    val vetName: String? = null,
    val nextVisitDate: LocalDate? = null,
    val createdAt: Instant? = null,
)

/** Push/pull serialization for [Consultation] rows. Parent FK: `patientId` → [PatientSyncHandler]. */
@Single
class ConsultationSyncHandler(
    @Provided private val consultationRepository: IConsultationRepository,
    @Provided patientRepository: IPatientRepository,
    @Provided database: AnimallyDatabase,
) : PatientLinkedSyncHandler<ConsultationPayload>(patientRepository, database) {
    override val entityType: SyncEntityType = SyncEntityType.CONSULTATION

    override suspend fun buildRecord(
        entityId: Long,
        parentServerIds: Map<String, String?>,
    ): SyncRecord {
        val row =
            consultationRepository.getById(entityId)
                ?: throw NoSuchElementException("Consultation $entityId not found")
        val payloadBody =
            SyncJson
                .encodeToJsonElement(
                    ConsultationPayload.serializer(),
                    ConsultationPayload(
                        date = row.date,
                        subjective = row.subjective,
                        objective = row.objective,
                        assessment = row.assessment,
                        plan = row.plan,
                        vetName = row.vetName,
                        nextVisitDate = row.nextVisitDate,
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

    override fun decodePayload(r: SyncRecord): ConsultationPayload = r.decode(ConsultationPayload.serializer())

    override suspend fun serverIdOf(entityId: Long): String? =
        database.consultationQueries
            .selectById(entityId)
            .executeAsOneOrNull()
            ?.serverId

    override suspend fun localIdFor(serverId: String): Long? =
        database.consultationQueries
            .selectByServerId(serverId)
            .executeAsOneOrNull()
            ?.id

    override fun insertRemote(
        record: SyncRecord,
        payload: ConsultationPayload,
        serverId: String,
    ): Long {
        val patientId = resolveParentPatientId(record) ?: return ENTITY_NOT_APPLIED
        val newId =
            consultationRepository.insert(
                Consultation(
                    id = 0L,
                    patientId = patientId,
                    date = payload.date,
                    subjective = payload.subjective,
                    objective = payload.objective,
                    assessment = payload.assessment,
                    plan = payload.plan,
                    vetName = payload.vetName,
                    nextVisitDate = payload.nextVisitDate,
                    isActive = record.isActive,
                    createdAt = payload.createdAt ?: record.updatedAt,
                    updatedAt = record.updatedAt,
                ),
            )
        database.consultationQueries.setServerId(serverId, record.updatedAt, newId)
        return newId
    }

    override fun applyRemote(
        existingId: Long,
        record: SyncRecord,
        payload: ConsultationPayload,
    ): Long {
        val local = consultationRepository.getById(existingId) ?: return ENTITY_NOT_APPLIED
        if (lwwDecision(record, local.updatedAt) == Lww.KEEP) return existingId
        consultationRepository.update(
            Consultation(
                id = existingId,
                patientId = resolveParentPatientId(record) ?: local.patientId,
                date = payload.date,
                subjective = payload.subjective,
                objective = payload.objective,
                assessment = payload.assessment,
                plan = payload.plan,
                vetName = payload.vetName,
                nextVisitDate = payload.nextVisitDate,
                isActive = record.isActive,
                createdAt = local.createdAt,
                updatedAt = record.updatedAt,
            ),
        )
        return existingId
    }
}
