package com.github.rodrigotimoteo.animally.domain.vaccination

import com.github.rodrigotimoteo.animally.domain.vaccination.model.Vaccination
import kotlin.time.Instant

/**
 * Repository contract for accessing [Vaccination] records.
 */
interface IVaccinationRepository {
    /**
     * Returns all active vaccinations for the patient with the given [patientId].
     *
     * @param patientId the patient identifier to look up.
     * @return the list of matching active vaccinations.
     */
    fun getByPatient(patientId: Long): List<Vaccination>

    /**
     * Returns the active vaccination with the given [id], or `null` when not found.
     *
     * @param id the vaccination identifier to look up.
     * @return the matching vaccination, or `null` if none exists.
     */
    fun getById(id: Long): Vaccination?

    /**
     * Inserts [vaccination] into persistence and returns the generated identifier.
     *
     * @param vaccination the vaccination to persist.
     * @return the id of the inserted vaccination.
     */
    fun insert(vaccination: Vaccination): Long

    /**
     * Updates the persisted data for [vaccination].
     *
     * @param vaccination the vaccination containing the updated data.
     * @return the number of rows affected.
     */
    fun update(vaccination: Vaccination): Long

    /**
     * Marks the vaccination identified by [id] as inactive without removing it.
     *
     * @param id the identifier of the vaccination to deactivate.
     * @param updatedAt the current date and time.
     * @return the number of rows affected.
     */
    fun setInactive(
        id: Long,
        updatedAt: Instant,
    ): Long
}
