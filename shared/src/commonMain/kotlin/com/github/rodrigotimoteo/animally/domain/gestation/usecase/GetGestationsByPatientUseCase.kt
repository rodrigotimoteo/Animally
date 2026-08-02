package com.github.rodrigotimoteo.animally.domain.gestation.usecase

import com.github.rodrigotimoteo.animally.domain.gestation.IGestationRepository
import com.github.rodrigotimoteo.animally.domain.gestation.model.Gestation
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single

/**
 * Use case for retrieving all gestation records of a patient.
 *
 * @param gestationRepository Repository instance for accessing gestation data.
 */
@Single
class GetGestationsByPatientUseCase(
    @Provided private val gestationRepository: IGestationRepository,
) {
    /**
     * Retrieves all active gestation records for the patient with the given [patientId],
     * ordered by breeding date descending.
     *
     * @param patientId The identifier of the patient.
     * @return The list of matching [Gestation] objects.
     */
    operator fun invoke(patientId: Long): List<Gestation> = gestationRepository.getByPatient(patientId)
}
