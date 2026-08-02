package com.github.rodrigotimoteo.animally.domain.reproduction.usecase

import com.github.rodrigotimoteo.animally.domain.reproduction.IReproductionRepository
import com.github.rodrigotimoteo.animally.domain.reproduction.model.ReproductionEvent
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single

/**
 * Use case for retrieving all reproduction events of a patient.
 *
 * @param reproductionRepository Repository instance for accessing reproduction event data.
 */
@Single
class GetReproductionEventsByPatientUseCase(
    @Provided private val reproductionRepository: IReproductionRepository,
) {
    /**
     * Retrieves all active reproduction events for the patient with the given [patientId],
     * ordered by event date descending.
     *
     * @param patientId The identifier of the patient.
     * @return The list of matching [ReproductionEvent] objects.
     */
    operator fun invoke(patientId: Long): List<ReproductionEvent> = reproductionRepository.getByPatient(patientId)
}
