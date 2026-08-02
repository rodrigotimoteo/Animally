package com.github.rodrigotimoteo.animally.domain.vaccination.usecase

import com.github.rodrigotimoteo.animally.domain.vaccination.IVaccinationRepository
import com.github.rodrigotimoteo.animally.domain.vaccination.model.Vaccination
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single

/**
 * Use case for retrieving all vaccinations of a patient.
 *
 * @param vaccinationRepository Repository instance for accessing vaccination data.
 */
@Single
class GetVaccinationsByPatientUseCase(
    @Provided private val vaccinationRepository: IVaccinationRepository,
) {
    /**
     * Retrieves all active vaccinations for the patient with the given [patientId].
     *
     * @param patientId The identifier of the patient.
     * @return The list of matching [Vaccination] objects.
     */
    operator fun invoke(patientId: Long): List<Vaccination> = vaccinationRepository.getByPatient(patientId)
}
