package com.github.rodrigotimoteo.animally.domain.consultation

import com.github.rodrigotimoteo.animally.domain.consultation.model.Consultation
import kotlin.time.Instant

/**
 * Repository contract for accessing [Consultation] records.
 */
interface IConsultationRepository {
    /**
     * Returns all active consultations for the patient with the given [patientId],
     * ordered by consultation date descending (most recent first).
     *
     * @param patientId the patient identifier to look up.
     * @return the list of matching active consultations.
     */
    fun getByPatient(patientId: Long): List<Consultation>

    /**
     * Returns the active consultation with the given [id], or `null` when not found.
     *
     * @param id the consultation identifier to look up.
     * @return the matching consultation, or `null` if none exists.
     */
    fun getById(id: Long): Consultation?

    /**
     * Inserts [consultation] into persistence and returns the generated identifier.
     *
     * @param consultation the consultation to persist.
     * @return the id of the inserted consultation.
     */
    fun insert(consultation: Consultation): Long

    /**
     * Updates the persisted data for [consultation].
     *
     * @param consultation the consultation containing the updated data.
     * @return the number of rows affected.
     */
    fun update(consultation: Consultation): Long

    /**
     * Marks the consultation identified by [id] as inactive without removing it.
     *
     * @param id the identifier of the consultation to deactivate.
     * @param updatedAt the current date and time.
     * @return the number of rows affected.
     */
    fun setInactive(
        id: Long,
        updatedAt: Instant,
    ): Long
}
