package com.github.rodrigotimoteo.animally.domain.substance.usecase

import com.github.rodrigotimoteo.animally.domain.substance.IControlledSubstanceRepository
import com.github.rodrigotimoteo.animally.domain.substance.model.ControlledSubstance
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single

/**
 * Use case for retrieving all controlled substance records of a patient.
 *
 * @param substanceRepository Repository instance for accessing controlled substance data.
 */
@Single
class GetControlledSubstancesByPatientUseCase(
    @Provided private val substanceRepository: IControlledSubstanceRepository,
) {
    /**
     * Retrieves all active controlled substance records for the patient with the given [patientId],
     * ordered by administration date descending.
     *
     * @param patientId The identifier of the patient.
     * @return The list of matching [ControlledSubstance] objects.
     */
    operator fun invoke(patientId: Long): List<ControlledSubstance> = substanceRepository.getByPatient(patientId)
}
