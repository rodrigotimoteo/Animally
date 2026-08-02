package com.github.rodrigotimoteo.animally.domain.ultrasound

import com.github.rodrigotimoteo.animally.domain.ultrasound.model.Ultrasound
import kotlin.time.Instant

/**
 * Repository contract for accessing [Ultrasound] records.
 */
interface IUltrasoundRepository {
    /**
     * Returns all active ultrasounds for the patient with the given [patientId],
     * ordered by examination date descending (most recent first).
     *
     * @param patientId the patient identifier to look up.
     * @return the list of matching active ultrasounds.
     */
    fun getByPatient(patientId: Long): List<Ultrasound>

    /**
     * Returns the active ultrasound with the given [id], or `null` when not found.
     *
     * @param id the ultrasound identifier to look up.
     * @return the matching ultrasound, or `null` if none exists.
     */
    fun getById(id: Long): Ultrasound?

    /**
     * Inserts [ultrasound] into persistence and returns the generated identifier.
     *
     * @param ultrasound the ultrasound to persist.
     * @return the id of the inserted ultrasound.
     */
    fun insert(ultrasound: Ultrasound): Long

    /**
     * Updates the persisted data for [ultrasound].
     *
     * @param ultrasound the ultrasound containing the updated data.
     * @return the number of rows affected.
     */
    fun update(ultrasound: Ultrasound): Long

    /**
     * Marks the ultrasound identified by [id] as inactive without removing it.
     *
     * @param id the identifier of the ultrasound to deactivate.
     * @param updatedAt the current date and time.
     * @return the number of rows affected.
     */
    fun setInactive(
        id: Long,
        updatedAt: Instant,
    ): Long
}
