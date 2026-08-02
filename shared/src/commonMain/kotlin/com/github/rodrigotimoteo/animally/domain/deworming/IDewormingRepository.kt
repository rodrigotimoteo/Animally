package com.github.rodrigotimoteo.animally.domain.deworming

import com.github.rodrigotimoteo.animally.domain.deworming.model.Deworming
import kotlin.time.Instant

/**
 * Repository contract for accessing [Deworming] records.
 */
interface IDewormingRepository {
    /**
     * Returns all active deworming records for the patient with the given [patientId].
     *
     * @param patientId the patient identifier to look up.
     * @return the list of matching active deworming records.
     */
    fun getByPatient(patientId: Long): List<Deworming>

    /**
     * Returns the active deworming record with the given [id], or `null` when not found.
     *
     * @param id the deworming record identifier to look up.
     * @return the matching deworming record, or `null` if none exists.
     */
    fun getById(id: Long): Deworming?

    /**
     * Inserts [deworming] into persistence and returns the generated identifier.
     *
     * @param deworming the deworming record to persist.
     * @return the id of the inserted deworming record.
     */
    fun insert(deworming: Deworming): Long

    /**
     * Updates the persisted data for [deworming].
     *
     * @param deworming the deworming record containing the updated data.
     * @return the number of rows affected.
     */
    fun update(deworming: Deworming): Long

    /**
     * Marks the deworming record identified by [id] as inactive without removing it.
     *
     * @param id the identifier of the deworming record to deactivate.
     * @param updatedAt the current date and time.
     * @return the number of rows affected.
     */
    fun setInactive(
        id: Long,
        updatedAt: Instant,
    ): Long
}
