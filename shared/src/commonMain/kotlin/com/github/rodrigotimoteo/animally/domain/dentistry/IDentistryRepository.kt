package com.github.rodrigotimoteo.animally.domain.dentistry

import com.github.rodrigotimoteo.animally.domain.dentistry.model.Dentistry
import kotlin.time.Instant

/**
 * Repository contract for accessing [Dentistry] records.
 */
interface IDentistryRepository {
    /**
     * Returns all active dentistry records for the patient with the given [patientId].
     *
     * @param patientId the patient identifier to look up.
     * @return the list of matching active dentistry records.
     */
    fun getByPatient(patientId: Long): List<Dentistry>

    /**
     * Returns the active dentistry record with the given [id], or `null` when not found.
     *
     * @param id the dentistry record identifier to look up.
     * @return the matching dentistry record, or `null` if none exists.
     */
    fun getById(id: Long): Dentistry?

    /**
     * Inserts [dentistry] into persistence and returns the generated identifier.
     *
     * @param dentistry the dentistry record to persist.
     * @return the id of the inserted dentistry record.
     */
    fun insert(dentistry: Dentistry): Long

    /**
     * Updates the persisted data for [dentistry].
     *
     * @param dentistry the dentistry record containing the updated data.
     * @return the number of rows affected.
     */
    fun update(dentistry: Dentistry): Long

    /**
     * Marks the dentistry record identified by [id] as inactive without removing it.
     *
     * @param id the identifier of the dentistry record to deactivate.
     * @param updatedAt the current date and time.
     * @return the number of rows affected.
     */
    fun setInactive(
        id: Long,
        updatedAt: Instant,
    ): Long
}
