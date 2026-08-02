package com.github.rodrigotimoteo.animally.domain.medication

import com.github.rodrigotimoteo.animally.domain.medication.model.Medication
import kotlin.time.Instant

/**
 * Repository contract for accessing [Medication] records.
 */
interface IMedicationRepository {
    /**
     * Returns all active medication records for the patient with the given [patientId],
     * ordered by medication start date descending (most recent first).
     *
     * @param patientId the patient identifier to look up.
     * @return the list of matching active medication records.
     */
    fun getByPatient(patientId: Long): List<Medication>

    /**
     * Returns the active medication record with the given [id], or `null` when not found.
     *
     * @param id the medication record identifier to look up.
     * @return the matching medication record, or `null` if none exists.
     */
    fun getById(id: Long): Medication?

    /**
     * Inserts [medication] into persistence and returns the generated identifier.
     *
     * @param medication the medication record to persist.
     * @return the id of the inserted medication record.
     */
    fun insert(medication: Medication): Long

    /**
     * Updates the persisted data for [medication].
     *
     * @param medication the medication record containing the updated data.
     * @return the number of rows affected.
     */
    fun update(medication: Medication): Long

    /**
     * Marks the medication record identified by [id] as inactive without removing it.
     *
     * @param id the identifier of the medication record to deactivate.
     * @param updatedAt the current date and time.
     * @return the number of rows affected.
     */
    fun setInactive(
        id: Long,
        updatedAt: Instant,
    ): Long
}
