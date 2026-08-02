package com.github.rodrigotimoteo.animally.domain.patient

import com.github.rodrigotimoteo.animally.domain.patient.model.Patient
import kotlin.time.Instant

/**
 * Repository contract for managing [Patient] records.
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

    /**
     * Returns all active patients.
     *
     * @return the list of active patients.
     */
    fun getPatientList(): List<Patient>

    /**
     * Returns the active patient with the given [id], or `null` when not found.
     *
     * @param id the patient identifier to look up.
     * @return the matching active patient, or `null`.
     */
    fun getPatientById(id: Long): Patient?

    /**
     * Inserts a new patient and returns the generated identifier.
     *
     * @param patient the patient to insert.
     * @return the id of the inserted patient.
     */
    fun insertPatient(patient: Patient): Long

    /**
     * Updates an existing patient and returns the number of rows affected.
     *
     * @param patient the patient to update.
     * @return the number of rows affected.
     */
    fun updatePatient(patient: Patient): Long

    /**
     * Soft-deletes the patient with the given [id] by marking it inactive.
     *
     * @param id the patient identifier to mark inactive.
     * @param updatedAt the timestamp to record on the patient.
     * @return the number of rows affected.
     */
    fun setInactive(
        id: Long,
        updatedAt: Instant,
    ): Long

    /**
     * Returns the total number of active records linked to the patient with the given [patientId]
     * across all patient-linked tables.
     *
     * @param patientId the patient identifier to look up.
     * @return the number of active linked records.
     */
    fun countActiveRecords(patientId: Long): Long
}
