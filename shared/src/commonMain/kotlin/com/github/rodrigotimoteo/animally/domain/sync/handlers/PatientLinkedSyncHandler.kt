package com.github.rodrigotimoteo.animally.domain.sync.handlers

import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.domain.patient.IPatientRepository
import com.github.rodrigotimoteo.animally.domain.sync.ENTITY_NOT_APPLIED
import com.github.rodrigotimoteo.animally.domain.sync.SyncRecord

/**
 * Base for every handler whose entity is a child of [IPatientRepository]'s
 * [Patient] (parent FK `patientId`).
 *
 * Implements the pull-side control flow once: server-id lookup, LWW decision,
 * and insert-vs-update dispatch. Subclasses only provide the entity-specific
 * payload codec and row (de)serialization.
 *
 * @param P the @Serializable payload DTO of the concrete entity.
 */
abstract class PatientLinkedSyncHandler<P>(
    protected val patientRepository: IPatientRepository,
    database: AnimallyDatabase,
) : EntitySyncHandler(database) {
    /** Decodes the entity body from [record.payload]. */
    protected abstract fun decodePayload(record: SyncRecord): P

    /** Inserts a new local row from the remote [record]; must stamp its server id. */
    protected abstract fun insertRemote(
        record: SyncRecord,
        payload: P,
        serverId: String,
    ): Long

    /** Applies the remote [record] over the existing local row [existingId] (LWW pre-decided). */
    protected abstract fun applyRemote(
        existingId: Long,
        record: SyncRecord,
        payload: P,
    ): Long

    /**
     * Resolves the parent patient for [record] to a local id, or `null` when
     * the patient is not yet synced locally (orphan protection).
     */
    protected fun resolveParentPatientId(record: SyncRecord): Long? {
        val serverId = record.parentServerIds["patientId"] ?: return null
        return localPatientIdFor(serverId)
    }

    /**
     * Default parent mapping for buildRecord: the local patient's server id,
     * or `null` when the patient is unsynced.
     *
     * Server ids are read straight from the generated `selectById` query — the
     * domain mappers do not carry `serverId` into the domain models.
     */
    protected suspend fun defaultPatientParent(patientId: Long): Map<String, String?> =
        mapOf(
            "patientId" to
                patientRepository.getPatientById(patientId)?.let {
                    database.patientQueries
                        .selectById(it.id)
                        .executeAsOneOrNull()
                        ?.serverId
                },
        )

    override suspend fun applyRecord(record: SyncRecord): Long {
        val serverId = record.serverId ?: return ENTITY_NOT_APPLIED
        val existingId = localIdFor(serverId)
        val payload = decodePayload(record)
        return if (existingId == null) {
            insertRemote(record, payload, serverId)
        } else {
            applyRemote(existingId, record, payload)
        }
    }
}
