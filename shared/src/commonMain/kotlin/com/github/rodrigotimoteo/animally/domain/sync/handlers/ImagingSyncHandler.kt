package com.github.rodrigotimoteo.animally.domain.sync.handlers

import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.domain.imaging.IImagingRepository
import com.github.rodrigotimoteo.animally.domain.imaging.model.Imaging
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

/** Payload body of an [Imaging] record. `patientId` travels in parentServerIds. */
@Serializable
data class ImagingPayload(
    val type: String,
    val date: LocalDate,
    val findings: String? = null,
    val imageUris: String? = null,
    val vetName: String? = null,
    val notes: String? = null,
    val createdAt: Instant? = null,
)

/** Push/pull serialization for [Imaging] rows. Parent FK: `patientId` → [PatientSyncHandler]. */
@Single
class ImagingSyncHandler(
    @Provided private val imagingRepository: IImagingRepository,
    @Provided patientRepository: IPatientRepository,
    @Provided database: AnimallyDatabase,
) : PatientLinkedSyncHandler<ImagingPayload>(patientRepository, database) {
    override val entityType: SyncEntityType = SyncEntityType.IMAGING

    override suspend fun buildRecord(
        entityId: Long,
        parentServerIds: Map<String, String?>,
    ): SyncRecord {
        val row = imagingRepository.getById(entityId) ?: throw NoSuchElementException("Imaging $entityId not found")
        val payloadBody =
            SyncJson
                .encodeToJsonElement(
                    ImagingPayload.serializer(),
                    ImagingPayload(
                        type = row.type,
                        date = row.date,
                        findings = row.findings,
                        imageUris = row.imageUris,
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

    override fun decodePayload(r: SyncRecord): ImagingPayload = r.decode(ImagingPayload.serializer())

    override suspend fun serverIdOf(entityId: Long): String? =
        database.imagingQueries
            .selectById(entityId)
            .executeAsOneOrNull()
            ?.serverId

    override suspend fun localIdFor(serverId: String): Long? =
        database.imagingQueries
            .selectByServerId(serverId)
            .executeAsOneOrNull()
            ?.id

    override fun insertRemote(
        record: SyncRecord,
        payload: ImagingPayload,
        serverId: String,
    ): Long {
        val patientId = resolveParentPatientId(record) ?: return ENTITY_NOT_APPLIED
        val newId =
            imagingRepository.insert(
                Imaging(
                    id = 0L,
                    patientId = patientId,
                    type = payload.type,
                    date = payload.date,
                    findings = payload.findings,
                    imageUris = payload.imageUris,
                    vetName = payload.vetName,
                    notes = payload.notes,
                    isActive = record.isActive,
                    createdAt = payload.createdAt ?: record.updatedAt,
                    updatedAt = record.updatedAt,
                ),
            )
        database.imagingQueries.setServerId(serverId, record.updatedAt, newId)
        return newId
    }

    override fun applyRemote(
        existingId: Long,
        record: SyncRecord,
        payload: ImagingPayload,
    ): Long {
        val local = imagingRepository.getById(existingId) ?: return ENTITY_NOT_APPLIED
        if (lwwDecision(record, local.updatedAt) == Lww.KEEP) return existingId
        imagingRepository.update(
            Imaging(
                id = existingId,
                patientId = resolveParentPatientId(record) ?: local.patientId,
                type = payload.type,
                date = payload.date,
                findings = payload.findings,
                imageUris = payload.imageUris,
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
