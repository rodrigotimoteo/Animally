package com.github.rodrigotimoteo.animally.domain.labresult

import com.github.rodrigotimoteo.animally.domain.labresult.model.LabResult
import kotlin.time.Instant

/**
 * Repository contract for accessing [LabResult] records.
 */
interface ILabResultRepository {
    /**
     * Returns all active lab results for the patient with the given [patientId],
     * ordered by test date descending (most recent first).
     *
     * @param patientId the patient identifier to look up.
     * @return the list of matching active lab results.
     */
    fun getByPatient(patientId: Long): List<LabResult>

    /**
     * Returns the active lab result with the given [id], or `null` when not found.
     *
     * @param id the lab result identifier to look up.
     * @return the matching lab result, or `null` if none exists.
     */
    fun getById(id: Long): LabResult?

    /**
     * Inserts [labResult] into persistence and returns the generated identifier.
     *
     * @param labResult the lab result to persist.
     * @return the id of the inserted lab result.
     */
    fun insert(labResult: LabResult): Long

    /**
     * Updates the persisted data for [labResult].
     *
     * @param labResult the lab result containing the updated data.
     * @return the number of rows affected.
     */
    fun update(labResult: LabResult): Long

    /**
     * Marks the lab result identified by [id] as inactive without removing it.
     *
     * @param id the identifier of the lab result to deactivate.
     * @param updatedAt the current date and time.
     * @return the number of rows affected.
     */
    fun setInactive(
        id: Long,
        updatedAt: Instant,
    ): Long
}
