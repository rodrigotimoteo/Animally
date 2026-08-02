package com.github.rodrigotimoteo.animally.domain.patient.usecase

import com.github.rodrigotimoteo.animally.domain.patient.IPatientRepository
import com.github.rodrigotimoteo.animally.domain.patient.model.Patient
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single

/**
 * Use case for persisting a new or updated patient.
 *
 * A single save path for both create and edit flows: patients with `id == 0L`
 * are inserted, all others are updated.
 *
 * @param patientRepository Repository instance for accessing patient data.
 */
@Single
class SavePatientUseCase(
    @Provided private val patientRepository: IPatientRepository,
) {
    /**
     * Persists the given [patient] and returns the generated identifier for new patients.
     *
     * @param patient the patient to persist.
     * @return the id of the persisted patient.
     */
    operator fun invoke(patient: Patient): Long =
        if (patient.id == 0L) {
            patientRepository.insertPatient(patient)
        } else {
            patientRepository.updatePatient(patient)
        }
}
