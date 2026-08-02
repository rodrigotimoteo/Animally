package com.github.rodrigotimoteo.animally.domain.repromedication

import com.github.rodrigotimoteo.animally.domain.repromedication.model.ReproMedication
import kotlin.time.Instant

/**
 * Repository contract for accessing [ReproMedication] records.
 */
interface IReproMedicationRepository {
    /**
     * Returns all active reproduction medications for the patient with the given [patientId],
     * ordered by administration date descending (most recent first).
     *
     * @param patientId the patient identifier to look up.
     * @return the list of matching active reproduction medications.
     */
    fun getByPatient(patientId: Long): List<ReproMedication>

    /**
     * Returns the active reproduction medication with the given [id], or `null` when not found.
     *
     * @param id the reproduction medication identifier to look up.
     * @return the matching reproduction medication, or `null` if none exists.
     */
    fun getById(id: Long): ReproMedication?

    /**
     * Inserts [reproMedication] into persistence and returns the generated identifier.
     *
     * @param reproMedication the reproduction medication to persist.
     * @return the id of the inserted reproduction medication.
     */
    fun insert(reproMedication: ReproMedication): Long

    /**
     * Updates the persisted data for [reproMedication].
     *
     * @param reproMedication the reproduction medication containing the updated data.
     * @return the number of rows affected.
     */
    fun update(reproMedication: ReproMedication): Long

    /**
     * Marks the reproduction medication identified by [id] as inactive without removing it.
     *
     * @param id the identifier of the reproduction medication to deactivate.
     * @param updatedAt the current date and time.
     * @return the number of rows affected.
     */
    fun setInactive(
        id: Long,
        updatedAt: Instant,
    ): Long
}
