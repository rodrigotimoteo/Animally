package com.github.rodrigotimoteo.animally.domain.surgery.usecase

import com.github.rodrigotimoteo.animally.domain.surgery.ISurgeryRepository
import com.github.rodrigotimoteo.animally.domain.surgery.model.Surgery
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single

/**
 * Use case for retrieving all surgeries of a patient.
 *
 * @param surgeryRepository Repository instance for accessing surgery data.
 */
@Single
class GetSurgeriesByPatientUseCase(
    @Provided private val surgeryRepository: ISurgeryRepository,
) {
    /**
     * Retrieves all active surgeries for the patient with the given [patientId],
     * ordered by surgery date descending.
     *
     * @param patientId The identifier of the patient.
     * @return The list of matching [Surgery] objects.
     */
    operator fun invoke(patientId: Long): List<Surgery> = surgeryRepository.getByPatient(patientId)
}
