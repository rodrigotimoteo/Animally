package com.github.rodrigotimoteo.animally.domain.patient.usecase

import com.github.rodrigotimoteo.animally.domain.patient.IPatientRepository
import com.github.rodrigotimoteo.animally.domain.patient.model.Patient
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single

/**
 * Use case for retrieving detailed information about a specific patient.
 *
 * This use case is responsible for fetching patient data from the repository
 * and is typically used in presentation layers to display patient details.
 *
 * @param patientRepository Repository instance for accessing patient data.
 */
@Single
class GetPatientDetailUseCase(
    @Provided private val patientRepository: IPatientRepository,
) {
    /**
     * Retrieves detailed information for a patient by their ID.
     *
     * @param id The unique identifier of the patient to retrieve.
     * @return The [Patient] object if found, or `null` if no patient with the given ID exists.
     */
    operator fun invoke(id: Long): Patient? = patientRepository.getPatientById(id)
}
