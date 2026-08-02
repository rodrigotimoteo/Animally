package com.github.rodrigotimoteo.animally.domain.dentistry.usecase

import com.github.rodrigotimoteo.animally.domain.dentistry.IDentistryRepository
import com.github.rodrigotimoteo.animally.domain.dentistry.model.Dentistry
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single

/**
 * Use case for retrieving all dentistry records of a patient.
 *
 * @param dentistryRepository Repository instance for accessing dentistry data.
 */
@Single
class GetDentistryListByPatientUseCase(
    @Provided private val dentistryRepository: IDentistryRepository,
) {
    /**
     * Retrieves all active dentistry records for the patient with the given [patientId].
     *
     * @param patientId The identifier of the patient.
     * @return The list of matching [Dentistry] objects.
     */
    operator fun invoke(patientId: Long): List<Dentistry> = dentistryRepository.getByPatient(patientId)
}
