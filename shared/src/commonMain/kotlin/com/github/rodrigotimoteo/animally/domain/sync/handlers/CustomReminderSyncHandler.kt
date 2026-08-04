package com.github.rodrigotimoteo.animally.domain.sync.handlers

import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.domain.customreminder.ICustomReminderRepository
import com.github.rodrigotimoteo.animally.domain.customreminder.model.CustomReminder
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

/**
 * Payload body of a [CustomReminder] record.
 *
 * `patientId` travels in parentServerIds like every patient-linked handler.
 * [linkedRecordId] is a device-local reference to an in-app record (vaccination,
 * deworming, ...) and is carried verbatim — it is not resolved through the
 * server and may dangle after a remote apply.
 */
@Serializable
data class CustomReminderPayload(
    val title: String,
    val dueDate: LocalDate,
    val linkedRecordType: String? = null,
    val linkedRecordId: Long? = null,
    val notes: String? = null,
    val createdAt: Instant? = null,
)

/** Push/pull serialization for [CustomReminder] rows. Parent FK: `patientId` → [PatientSyncHandler]. */
@Single
class CustomReminderSyncHandler(
    @Provided private val customReminderRepository: ICustomReminderRepository,
    @Provided patientRepository: IPatientRepository,
    @Provided database: AnimallyDatabase,
) : PatientLinkedSyncHandler<CustomReminderPayload>(patientRepository, database) {
    override val entityType: SyncEntityType = SyncEntityType.CUSTOM_REMINDER

    override suspend fun buildRecord(
        entityId: Long,
        parentServerIds: Map<String, String?>,
    ): SyncRecord {
        val row =
            customReminderRepository.getById(entityId)
                ?: throw NoSuchElementException("CustomReminder $entityId not found")
        val payloadBody =
            SyncJson
                .encodeToJsonElement(
                    CustomReminderPayload.serializer(),
                    CustomReminderPayload(
                        title = row.title,
                        dueDate = row.dueDate,
                        linkedRecordType = row.linkedRecordType,
                        linkedRecordId = row.linkedRecordId,
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

    override fun decodePayload(r: SyncRecord): CustomReminderPayload = r.decode(CustomReminderPayload.serializer())

    override suspend fun serverIdOf(entityId: Long): String? =
        database.customReminderQueries
            .selectById(entityId)
            .executeAsOneOrNull()
            ?.serverId

    override suspend fun localIdFor(serverId: String): Long? =
        database.customReminderQueries
            .selectByServerId(serverId)
            .executeAsOneOrNull()
            ?.id

    override fun insertRemote(
        record: SyncRecord,
        payload: CustomReminderPayload,
        serverId: String,
    ): Long {
        val patientId = resolveParentPatientId(record) ?: return ENTITY_NOT_APPLIED
        val newId =
            customReminderRepository.insert(
                CustomReminder(
                    id = 0L,
                    patientId = patientId,
                    title = payload.title,
                    dueDate = payload.dueDate,
                    linkedRecordType = payload.linkedRecordType,
                    linkedRecordId = payload.linkedRecordId,
                    notes = payload.notes,
                    isActive = record.isActive,
                    createdAt = payload.createdAt ?: record.updatedAt,
                    updatedAt = record.updatedAt,
                ),
            )
        database.customReminderQueries.setServerId(serverId, record.updatedAt, newId)
        return newId
    }

    override fun applyRemote(
        existingId: Long,
        record: SyncRecord,
        payload: CustomReminderPayload,
    ): Long {
        val local = customReminderRepository.getById(existingId) ?: return ENTITY_NOT_APPLIED
        if (lwwDecision(record, local.updatedAt) == Lww.KEEP) return existingId
        customReminderRepository.update(
            CustomReminder(
                id = existingId,
                patientId = resolveParentPatientId(record) ?: local.patientId,
                title = payload.title,
                dueDate = payload.dueDate,
                linkedRecordType = payload.linkedRecordType,
                linkedRecordId = payload.linkedRecordId,
                notes = payload.notes,
                isActive = record.isActive,
                createdAt = local.createdAt,
                updatedAt = record.updatedAt,
            ),
        )
        return existingId
    }
}
