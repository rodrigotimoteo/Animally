package com.github.rodrigotimoteo.animally.domain.substance

import com.github.rodrigotimoteo.animally.domain.substance.model.ControlledSubstance
import kotlin.time.Instant

/**
 * Repository contract for accessing [ControlledSubstance] records.
 */
interface IControlledSubstanceRepository {
    /**
     * Returns all active controlled substance records for the patient with the given [patientId],
     * ordered by administration date descending (most recent first).
     *
     * @param patientId the patient identifier to look up.
     * @return the list of matching active controlled substance records.
     */
    fun getByPatient(patientId: Long): List<ControlledSubstance>

    /**
     * Returns the active controlled substance record with the given [id], or `null` when not found.
     *
     * @param id the controlled substance record identifier to look up.
     * @return the matching controlled substance record, or `null` if none exists.
     */
    fun getById(id: Long): ControlledSubstance?

    /**
     * Inserts [controlledSubstance] into persistence and returns the generated identifier.
     *
     * @param controlledSubstance the controlled substance record to persist.
     * @return the id of the inserted controlled substance record.
     */
    fun insert(controlledSubstance: ControlledSubstance): Long

    /**
     * Updates the persisted data for [controlledSubstance].
     *
     * @param controlledSubstance the controlled substance record containing the updated data.
     * @return the number of rows affected.
     */
    fun update(controlledSubstance: ControlledSubstance): Long

    /**
     * Marks the controlled substance record identified by [id] as inactive without removing it.
     *
     * @param id the identifier of the controlled substance record to deactivate.
     * @param updatedAt the current date and time.
     * @return the number of rows affected.
     */
    fun setInactive(
        id: Long,
        updatedAt: Instant,
    ): Long
}
