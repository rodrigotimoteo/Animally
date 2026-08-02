package com.github.rodrigotimoteo.animally.domain.patient

import com.github.rodrigotimoteo.animally.domain.patient.model.Patient

/**
 * Repository contract for reading active [Patient] records linked to an owner.
 */
interface IPatientRepository {
    /**
     * Returns all active patients assigned to the owner with the given [ownerId].
     *
     * @param ownerId the owner identifier to look up.
     * @return the list of matching active patients.
     */
    fun getPatientsByOwnerId(ownerId: Long): List<Patient>

    /**
     * Returns the number of active patients assigned to the owner with the given [ownerId].
     *
     * @param ownerId the owner identifier to look up.
     * @return the number of matching active patients.
     */
    fun countPatientsByOwnerId(ownerId: Long): Long
}
