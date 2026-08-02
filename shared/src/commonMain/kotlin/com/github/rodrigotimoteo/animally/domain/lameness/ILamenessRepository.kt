package com.github.rodrigotimoteo.animally.domain.lameness

import com.github.rodrigotimoteo.animally.domain.lameness.model.Lameness
import kotlin.time.Instant

/**
 * Repository contract for accessing [Lameness] records.
 */
interface ILamenessRepository {
    /**
     * Returns all active lameness records for the patient with the given [patientId],
     * ordered by lameness date descending (most recent first).
     *
     * @param patientId the patient identifier to look up.
     * @return the list of matching active lameness records.
     */
    fun getByPatient(patientId: Long): List<Lameness>

    /**
     * Returns the active lameness record with the given [id], or `null` when not found.
     *
     * @param id the lameness record identifier to look up.
     * @return the matching lameness record, or `null` if none exists.
     */
    fun getById(id: Long): Lameness?

    /**
     * Inserts [lameness] into persistence and returns the generated identifier.
     *
     * @param lameness the lameness record to persist.
     * @return the id of the inserted lameness record.
     */
    fun insert(lameness: Lameness): Long

    /**
     * Updates the persisted data for [lameness].
     *
     * @param lameness the lameness record containing the updated data.
     * @return the number of rows affected.
     */
    fun update(lameness: Lameness): Long

    /**
     * Marks the lameness record identified by [id] as inactive without removing it.
     *
     * @param id the identifier of the lameness record to deactivate.
     * @param updatedAt the current date and time.
     * @return the number of rows affected.
     */
    fun setInactive(
        id: Long,
        updatedAt: Instant,
    ): Long
}
