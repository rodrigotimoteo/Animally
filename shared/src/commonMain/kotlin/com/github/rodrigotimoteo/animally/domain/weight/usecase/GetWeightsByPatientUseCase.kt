package com.github.rodrigotimoteo.animally.domain.weight.usecase

import com.github.rodrigotimoteo.animally.domain.weight.IWeightRepository
import com.github.rodrigotimoteo.animally.domain.weight.model.Weight
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single

/**
 * Use case for retrieving all weight entries of a patient.
 *
 * @param weightRepository Repository instance for accessing weight data.
 */
@Single
class GetWeightsByPatientUseCase(
    @Provided private val weightRepository: IWeightRepository,
) {
    /**
     * Retrieves all active weight entries for the patient with the given [patientId],
     * ordered by measurement date descending.
     *
     * @param patientId The identifier of the patient.
     * @return The list of matching [Weight] objects.
     */
    operator fun invoke(patientId: Long): List<Weight> = weightRepository.getByPatient(patientId)
}
