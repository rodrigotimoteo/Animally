package com.github.rodrigotimoteo.animally.domain.patient.usecase

import com.github.rodrigotimoteo.animally.domain.patient.IPatientRepository
import com.github.rodrigotimoteo.animally.domain.patient.model.Patient
import com.github.rodrigotimoteo.animally.domain.search.ISearchRepository
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single

/**
 * Use case for persisting a new or updated patient.
 *
 * A single save path for both create and edit flows: patients with `id == 0L`
 * are inserted, all others are updated.
 *
 * @param patientRepository Repository instance for accessing patient data.
 * @param searchRepository Repository instance for the global search index.
 */
@Single
class SavePatientUseCase(
    @Provided private val patientRepository: IPatientRepository,
    @Provided private val searchRepository: ISearchRepository,
) {
    /**
     * Persists the given [patient] and returns the generated identifier for new patients.
     *
     * @param patient the patient to persist.
     * @return the id of the persisted patient.
     */
    operator fun invoke(patient: Patient): Long {
        val savedId =
            if (patient.id == 0L) {
                patientRepository.insertPatient(patient)
            } else {
                patientRepository.updatePatient(patient)
                patient.id
            }
        val searchableText = listOfNotNull(patient.name, patient.breed, patient.microchipId).joinToString(" ")
        searchRepository.indexRecord(ISearchRepository.TYPE_PATIENT, savedId, savedId, null, searchableText)
        return savedId
    }
}
