package com.github.rodrigotimoteo.animally.domain.reproduction

import com.github.rodrigotimoteo.animally.domain.reproduction.model.ReproductionEvent
import kotlin.time.Instant

/**
 * Repository contract for accessing [ReproductionEvent] records.
 */
interface IReproductionRepository {
    /**
     * Returns all active reproduction events for the patient with the given [patientId],
     * ordered by event date descending (most recent first).
     *
     * @param patientId the patient identifier to look up.
     * @return the list of matching active reproduction events.
     */
    fun getByPatient(patientId: Long): List<ReproductionEvent>

    /**
     * Returns the active reproduction event with the given [id], or `null` when not found.
     *
     * @param id the reproduction event identifier to look up.
     * @return the matching reproduction event, or `null` if none exists.
     */
    fun getById(id: Long): ReproductionEvent?

    /**
     * Inserts [reproductionEvent] into persistence and returns the generated identifier.
     *
     * @param reproductionEvent the reproduction event to persist.
     * @return the id of the inserted reproduction event.
     */
    fun insert(reproductionEvent: ReproductionEvent): Long

    /**
     * Updates the persisted data for [reproductionEvent].
     *
     * @param reproductionEvent the reproduction event containing the updated data.
     * @return the number of rows affected.
     */
    fun update(reproductionEvent: ReproductionEvent): Long

    /**
     * Marks the reproduction event identified by [id] as inactive without removing it.
     *
     * @param id the identifier of the reproduction event to deactivate.
     * @param updatedAt the current date and time.
     * @return the number of rows affected.
     */
    fun setInactive(
        id: Long,
        updatedAt: Instant,
    ): Long
}
