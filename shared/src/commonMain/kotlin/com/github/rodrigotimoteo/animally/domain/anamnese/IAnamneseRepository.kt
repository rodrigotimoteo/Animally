package com.github.rodrigotimoteo.animally.domain.anamnese

import com.github.rodrigotimoteo.animally.domain.anamnese.model.Anamnese

/**
 * Repository contract for accessing a patient's [Anamnese] record.
 *
 * Anamnese is 1:1 with a patient, so saves are upserts: persisting an anamnese
 * for a patient that already has one updates the existing row.
 */
interface IAnamneseRepository {
    /**
     * Returns the anamnese record for the patient with the given [patientId],
     * or `null` when the patient has none.
     *
     * @param patientId the patient identifier to look up.
     * @return the matching anamnese, or `null` if none exists.
     */
    fun getByPatient(patientId: Long): Anamnese?

    /**
     * Upserts the given [anamnese] and returns the generated identifier for new records.
     *
     * @param anamnese the anamnese to persist.
     * @return the id of the persisted anamnese.
     */
    fun save(anamnese: Anamnese): Long
}
