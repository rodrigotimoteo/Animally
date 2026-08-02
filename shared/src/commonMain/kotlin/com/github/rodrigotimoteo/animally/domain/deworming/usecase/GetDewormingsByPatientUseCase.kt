package com.github.rodrigotimoteo.animally.domain.deworming.usecase

import com.github.rodrigotimoteo.animally.domain.deworming.IDewormingRepository
import com.github.rodrigotimoteo.animally.domain.deworming.model.Deworming
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single

/**
 * Use case for retrieving all deworming records of a patient.
 *
 * @param dewormingRepository Repository instance for accessing deworming data.
 */
@Single
class GetDewormingsByPatientUseCase(
    @Provided private val dewormingRepository: IDewormingRepository,
) {
    /**
     * Retrieves all active deworming records for the patient with the given [patientId].
     *
     * @param patientId The identifier of the patient.
     * @return The list of matching [Deworming] objects.
     */
    operator fun invoke(patientId: Long): List<Deworming> = dewormingRepository.getByPatient(patientId)
}
