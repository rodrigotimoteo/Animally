package com.github.rodrigotimoteo.animally.domain.imaging

import com.github.rodrigotimoteo.animally.domain.imaging.model.Imaging
import kotlin.time.Instant

/**
 * Repository contract for accessing [Imaging] records.
 */
interface IImagingRepository {
    /**
     * Returns all active imaging records for the patient with the given [patientId],
     * ordered by imaging date descending (most recent first).
     *
     * @param patientId the patient identifier to look up.
     * @return the list of matching active imaging records.
     */
    fun getByPatient(patientId: Long): List<Imaging>

    /**
     * Returns the active imaging record with the given [id], or `null` when not found.
     *
     * @param id the imaging record identifier to look up.
     * @return the matching imaging record, or `null` if none exists.
     */
    fun getById(id: Long): Imaging?

    /**
     * Inserts [imaging] into persistence and returns the generated identifier.
     *
     * @param imaging the imaging record to persist.
     * @return the id of the inserted imaging record.
     */
    fun insert(imaging: Imaging): Long

    /**
     * Updates the persisted data for [imaging].
     *
     * @param imaging the imaging record containing the updated data.
     * @return the number of rows affected.
     */
    fun update(imaging: Imaging): Long

    /**
     * Marks the imaging record identified by [id] as inactive without removing it.
     *
     * @param id the identifier of the imaging record to deactivate.
     * @param updatedAt the current date and time.
     * @return the number of rows affected.
     */
    fun setInactive(
        id: Long,
        updatedAt: Instant,
    ): Long
}
