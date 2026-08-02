package com.github.rodrigotimoteo.animally.domain.farrier

import com.github.rodrigotimoteo.animally.domain.farrier.model.FarrierVisit
import kotlin.time.Instant

/**
 * Repository contract for accessing [FarrierVisit] records.
 */
interface IFarrierVisitRepository {
    /**
     * Returns all active farrier visits for the patient with the given [patientId].
     *
     * @param patientId the patient identifier to look up.
     * @return the list of matching active farrier visits.
     */
    fun getByPatient(patientId: Long): List<FarrierVisit>

    /**
     * Returns the active farrier visit with the given [id], or `null` when not found.
     *
     * @param id the farrier visit identifier to look up.
     * @return the matching farrier visit, or `null` if none exists.
     */
    fun getById(id: Long): FarrierVisit?

    /**
     * Inserts [farrierVisit] into persistence and returns the generated identifier.
     *
     * @param farrierVisit the farrier visit to persist.
     * @return the id of the inserted farrier visit.
     */
    fun insert(farrierVisit: FarrierVisit): Long

    /**
     * Updates the persisted data for [farrierVisit].
     *
     * @param farrierVisit the farrier visit containing the updated data.
     * @return the number of rows affected.
     */
    fun update(farrierVisit: FarrierVisit): Long

    /**
     * Marks the farrier visit identified by [id] as inactive without removing it.
     *
     * @param id the identifier of the farrier visit to deactivate.
     * @param updatedAt the current date and time.
     * @return the number of rows affected.
     */
    fun setInactive(
        id: Long,
        updatedAt: Instant,
    ): Long
}
