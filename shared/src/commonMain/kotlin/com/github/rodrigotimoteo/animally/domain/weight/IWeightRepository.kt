package com.github.rodrigotimoteo.animally.domain.weight

import com.github.rodrigotimoteo.animally.domain.weight.model.Weight
import kotlin.time.Instant

/**
 * Repository contract for accessing [Weight] records.
 */
interface IWeightRepository {
    /**
     * Returns all active weight entries for the patient with the given [patientId],
     * ordered by measurement date descending (most recent first).
     *
     * @param patientId the patient identifier to look up.
     * @return the list of matching active weight entries.
     */
    fun getByPatient(patientId: Long): List<Weight>

    /**
     * Returns the active weight entry with the given [id], or `null` when not found.
     *
     * @param id the weight entry identifier to look up.
     * @return the matching weight entry, or `null` if none exists.
     */
    fun getById(id: Long): Weight?

    /**
     * Inserts [weight] into persistence and returns the generated identifier.
     *
     * @param weight the weight entry to persist.
     * @return the id of the inserted weight entry.
     */
    fun insert(weight: Weight): Long

    /**
     * Updates the persisted data for [weight].
     *
     * @param weight the weight entry containing the updated data.
     * @return the number of rows affected.
     */
    fun update(weight: Weight): Long

    /**
     * Marks the weight entry identified by [id] as inactive without removing it.
     *
     * @param id the identifier of the weight entry to deactivate.
     * @param updatedAt the current date and time.
     * @return the number of rows affected.
     */
    fun setInactive(
        id: Long,
        updatedAt: Instant,
    ): Long
}
