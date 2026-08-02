package com.github.rodrigotimoteo.animally.domain.patient.usecase

import com.github.rodrigotimoteo.animally.domain.patient.IPatientRepository
import com.github.rodrigotimoteo.animally.domain.patient.model.Patient
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single

/**
 * Use case for retrieving a list of all patients.
 *
 * This use case is responsible for fetching all patient data from the repository
 * and is typically used in presentation layers to display a list of patients.
 *
 * @param patientRepository Repository instance for accessing patient data.
 */
@Single
class GetPatientListUseCase(
    @Provided private val patientRepository: IPatientRepository,
) {
    /**
     * Retrieves a list of all patients.
     *
     * @return A list of [Patient] objects containing all patients in the system.
     */
    operator fun invoke(): List<Patient> = patientRepository.getPatientList()
}
