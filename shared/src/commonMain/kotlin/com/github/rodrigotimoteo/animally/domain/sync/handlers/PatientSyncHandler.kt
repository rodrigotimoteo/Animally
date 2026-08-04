package com.github.rodrigotimoteo.animally.domain.sync.handlers

import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.domain.patient.IPatientRepository
import com.github.rodrigotimoteo.animally.domain.patient.model.Patient
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
 * Payload body of a [Patient] record. `ownerId` is a parent FK and travels in
 * [SyncRecord.parentServerIds], not here.
 */
@Serializable
data class PatientPayload(
    val name: String,
    val species: String = "Equine",
    val breed: String? = null,
    val dateOfBirth: LocalDate? = null,
    val gender: String? = null,
    val microchipId: String? = null,
    val ueln: String? = null,
    val registrationNumber: String? = null,
    val stableLocation: String? = null,
    val photoUri: String? = null,
    val notes: String? = null,
    val cogginsTestDate: LocalDate? = null,
    val cogginsResult: String? = null,
    val cogginsExpiryDate: LocalDate? = null,
    val createdAt: Instant? = null,
)

/** Push/pull serialization for [Patient] rows. Parent FK: `ownerId` → [OwnerSyncHandler]. */
@Single
class PatientSyncHandler(
    @Provided private val patientRepository: IPatientRepository,
    @Provided database: AnimallyDatabase,
) : EntitySyncHandler(database) {
    override val entityType: SyncEntityType = SyncEntityType.PATIENT

    override suspend fun buildRecord(
        entityId: Long,
        parentServerIds: Map<String, String?>,
    ): SyncRecord {
        val row =
            patientRepository.getPatientById(entityId)
                ?: throw NoSuchElementException("Patient $entityId not found")
        val ownerServerId =
            row.ownerId?.let {
                database.ownerQueries
                    .selectById(it)
                    .executeAsOneOrNull()
                    ?.serverId
            }
        val payloadBody =
            SyncJson
                .encodeToJsonElement(
                    PatientPayload.serializer(),
                    PatientPayload(
                        name = row.name,
                        species = row.species,
                        breed = row.breed,
                        dateOfBirth = row.dateOfBirth,
                        gender = row.gender,
                        microchipId = row.microchipId,
                        ueln = row.ueln,
                        registrationNumber = row.registrationNumber,
                        stableLocation = row.stableLocation,
                        photoUri = row.photoUri,
                        notes = row.notes,
                        cogginsTestDate = row.cogginsTestDate,
                        cogginsResult = row.cogginsResult,
                        cogginsExpiryDate = row.cogginsExpiryDate,
                        createdAt = row.createdAt,
                    ),
                ).jsonObject
        return SyncRecord(
            type = entityType.wireName,
            serverId = serverIdOf(entityId),
            clientId = entityId,
            updatedAt = row.updatedAt,
            isActive = row.isActive,
            parentServerIds = parentServerIds.ifEmpty { mapOf("ownerId" to ownerServerId) },
            payload = payloadBody,
        )
    }

    override suspend fun applyRecord(record: SyncRecord): Long {
        val serverId = record.serverId ?: return ENTITY_NOT_APPLIED
        val existingId = localIdFor(serverId)
        val payload = SyncJson.decodeFromJsonElement(PatientPayload.serializer(), record.payload)
        return if (existingId == null) {
            insertRemote(record, payload, serverId)
        } else {
            applyRemote(existingId, record, payload)
        }
    }

    override suspend fun serverIdOf(entityId: Long): String? = patientRepository.getPatientById(entityId)?.serverId

    override suspend fun localIdFor(serverId: String): Long? =
        database.patientQueries
            .selectByServerId(serverId)
            .executeAsOneOrNull()
            ?.id

    private fun insertRemote(
        record: SyncRecord,
        payload: PatientPayload,
        serverId: String,
    ): Long {
        val newId =
            patientRepository.insertPatient(
                Patient(
                    id = 0L,
                    name = payload.name,
                    species = payload.species,
                    breed = payload.breed,
                    dateOfBirth = payload.dateOfBirth,
                    gender = payload.gender,
                    microchipId = payload.microchipId,
                    ueln = payload.ueln,
                    registrationNumber = payload.registrationNumber,
                    stableLocation = payload.stableLocation,
                    photoUri = payload.photoUri,
                    notes = payload.notes,
                    cogginsTestDate = payload.cogginsTestDate,
                    cogginsResult = payload.cogginsResult,
                    cogginsExpiryDate = payload.cogginsExpiryDate,
                    ownerId = record.parentServerIds["ownerId"]?.let { localOwnerIdFor(it) },
                    isActive = record.isActive,
                    createdAt = payload.createdAt ?: record.updatedAt,
                    updatedAt = record.updatedAt,
                ),
            )
        database.patientQueries.setServerId(serverId, record.updatedAt, newId)
        return newId
    }

    private fun applyRemote(
        existingId: Long,
        record: SyncRecord,
        payload: PatientPayload,
    ): Long {
        val local = patientRepository.getPatientById(existingId) ?: return ENTITY_NOT_APPLIED
        if (lwwDecision(record, local.updatedAt) == Lww.KEEP) return existingId
        patientRepository.updatePatient(
            Patient(
                id = existingId,
                name = payload.name,
                species = payload.species,
                breed = payload.breed,
                dateOfBirth = payload.dateOfBirth,
                gender = payload.gender,
                microchipId = payload.microchipId,
                ueln = payload.ueln,
                registrationNumber = payload.registrationNumber,
                stableLocation = payload.stableLocation,
                photoUri = payload.photoUri,
                notes = payload.notes,
                cogginsTestDate = payload.cogginsTestDate,
                cogginsResult = payload.cogginsResult,
                cogginsExpiryDate = payload.cogginsExpiryDate,
                ownerId = record.parentServerIds["ownerId"]?.let { localOwnerIdFor(it) } ?: local.ownerId,
                isActive = record.isActive,
                createdAt = local.createdAt,
                updatedAt = record.updatedAt,
            ),
        )
        return existingId
    }
}
