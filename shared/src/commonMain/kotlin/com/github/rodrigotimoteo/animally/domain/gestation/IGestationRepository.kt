package com.github.rodrigotimoteo.animally.domain.gestation

import com.github.rodrigotimoteo.animally.domain.gestation.model.Gestation
import kotlin.time.Instant

/**
 * Repository contract for accessing [Gestation] records.
 */
interface IGestationRepository {
    /**
     * Returns all active gestation records for the patient with the given [patientId],
     * ordered by breeding date descending (most recent first).
     *
     * @param patientId the patient identifier to look up.
     * @return the list of matching active gestation records.
     */
    fun getByPatient(patientId: Long): List<Gestation>

    /**
     * Returns the active gestation record with the given [id], or `null` when not found.
     *
     * @param id the gestation identifier to look up.
     * @return the matching gestation record, or `null` if none exists.
     */
    fun getById(id: Long): Gestation?

    /**
     * Inserts [gestation] into persistence and returns the generated identifier.
     *
     * @param gestation the gestation record to persist.
     * @return the id of the inserted gestation record.
     */
    fun insert(gestation: Gestation): Long

    /**
     * Updates the persisted data for [gestation].
     *
     * @param gestation the gestation record containing the updated data.
     * @return the number of rows affected.
     */
    fun update(gestation: Gestation): Long

    /**
     * Marks the gestation record identified by [id] as inactive without removing it.
     *
     * @param id the identifier of the gestation record to deactivate.
     * @param updatedAt the current date and time.
     * @return the number of rows affected.
     */
    fun setInactive(
        id: Long,
        updatedAt: Instant,
    ): Long
}
