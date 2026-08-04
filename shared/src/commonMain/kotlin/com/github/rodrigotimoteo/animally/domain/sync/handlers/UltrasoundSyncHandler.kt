package com.github.rodrigotimoteo.animally.domain.sync.handlers

import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.domain.patient.IPatientRepository
import com.github.rodrigotimoteo.animally.domain.sync.ENTITY_NOT_APPLIED
import com.github.rodrigotimoteo.animally.domain.sync.SyncEntityType
import com.github.rodrigotimoteo.animally.domain.sync.SyncRecord
import com.github.rodrigotimoteo.animally.domain.ultrasound.IUltrasoundRepository
import com.github.rodrigotimoteo.animally.domain.ultrasound.model.Ultrasound
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.jsonObject
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single
import kotlin.time.Instant

/** Payload body of an [Ultrasound] record. `patientId` travels in parentServerIds. */
@Serializable
data class UltrasoundPayload(
    val date: LocalDate,
    val ovaryStatus: String? = null,
    val uterineStatus: String? = null,
    val follicleSizeMm: Double? = null,
    val findings: String? = null,
    val imageUris: String? = null,
    val vetName: String? = null,
    val notes: String? = null,
    val createdAt: Instant? = null,
)

/** Push/pull serialization for [Ultrasound] rows. Parent FK: `patientId` → [PatientSyncHandler]. */
@Single
class UltrasoundSyncHandler(
    @Provided private val ultrasoundRepository: IUltrasoundRepository,
    @Provided patientRepository: IPatientRepository,
    @Provided database: AnimallyDatabase,
) : PatientLinkedSyncHandler<UltrasoundPayload>(patientRepository, database) {
    override val entityType: SyncEntityType = SyncEntityType.ULTRASOUND

    override suspend fun buildRecord(
        entityId: Long,
        parentServerIds: Map<String, String?>,
    ): SyncRecord {
        val row =
            ultrasoundRepository.getById(entityId)
                ?: throw NoSuchElementException("Ultrasound $entityId not found")
        val payloadBody =
            SyncJson
                .encodeToJsonElement(
                    UltrasoundPayload.serializer(),
                    UltrasoundPayload(
                        date = row.date,
                        ovaryStatus = row.ovaryStatus,
                        uterineStatus = row.uterineStatus,
                        follicleSizeMm = row.follicleSizeMm,
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

    override fun decodePayload(r: SyncRecord): UltrasoundPayload = r.decode(UltrasoundPayload.serializer())

    override suspend fun serverIdOf(entityId: Long): String? =
        database.ultrasoundQueries
            .selectById(entityId)
            .executeAsOneOrNull()
            ?.serverId

    override suspend fun localIdFor(serverId: String): Long? =
        database.ultrasoundQueries
            .selectByServerId(serverId)
            .executeAsOneOrNull()
            ?.id

    override fun insertRemote(
        record: SyncRecord,
        payload: UltrasoundPayload,
        serverId: String,
    ): Long {
        val patientId = resolveParentPatientId(record) ?: return ENTITY_NOT_APPLIED
        val newId =
            ultrasoundRepository.insert(
                Ultrasound(
                    id = 0L,
                    patientId = patientId,
                    date = payload.date,
                    ovaryStatus = payload.ovaryStatus,
                    uterineStatus = payload.uterineStatus,
                    follicleSizeMm = payload.follicleSizeMm,
                    findings = payload.findings,
                    imageUris = payload.imageUris,
                    vetName = payload.vetName,
                    notes = payload.notes,
                    isActive = record.isActive,
                    createdAt = payload.createdAt ?: record.updatedAt,
                    updatedAt = record.updatedAt,
                ),
            )
        database.ultrasoundQueries.setServerId(serverId, record.updatedAt, newId)
        return newId
    }

    override fun applyRemote(
        existingId: Long,
        record: SyncRecord,
        payload: UltrasoundPayload,
    ): Long {
        val local = ultrasoundRepository.getById(existingId) ?: return ENTITY_NOT_APPLIED
        if (lwwDecision(record, local.updatedAt) == Lww.KEEP) return existingId
        ultrasoundRepository.update(
            Ultrasound(
                id = existingId,
                patientId = resolveParentPatientId(record) ?: local.patientId,
                date = payload.date,
                ovaryStatus = payload.ovaryStatus,
                uterineStatus = payload.uterineStatus,
                follicleSizeMm = payload.follicleSizeMm,
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
