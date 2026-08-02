package com.github.rodrigotimoteo.animally.domain.surgery

import com.github.rodrigotimoteo.animally.domain.surgery.model.Surgery
import kotlin.time.Instant

/**
 * Repository contract for accessing [Surgery] records.
 */
interface ISurgeryRepository {
    /**
     * Returns all active surgery records for the patient with the given [patientId],
     * ordered by surgery date descending (most recent first).
     *
     * @param patientId the patient identifier to look up.
     * @return the list of matching active surgery records.
     */
    fun getByPatient(patientId: Long): List<Surgery>

    /**
     * Returns the active surgery record with the given [id], or `null` when not found.
     *
     * @param id the surgery record identifier to look up.
     * @return the matching surgery record, or `null` if none exists.
     */
    fun getById(id: Long): Surgery?

    /**
     * Inserts [surgery] into persistence and returns the generated identifier.
     *
     * @param surgery the surgery record to persist.
     * @return the id of the inserted surgery record.
     */
    fun insert(surgery: Surgery): Long

    /**
     * Updates the persisted data for [surgery].
     *
     * @param surgery the surgery record containing the updated data.
     * @return the number of rows affected.
     */
    fun update(surgery: Surgery): Long

    /**
     * Marks the surgery record identified by [id] as inactive without removing it.
     *
     * @param id the identifier of the surgery record to deactivate.
     * @param updatedAt the current date and time.
     * @return the number of rows affected.
     */
    fun setInactive(
        id: Long,
        updatedAt: Instant,
    ): Long
}
