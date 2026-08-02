package com.github.rodrigotimoteo.animally.domain.lameness.usecase

import com.github.rodrigotimoteo.animally.domain.lameness.ILamenessRepository
import com.github.rodrigotimoteo.animally.domain.lameness.model.Lameness
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single

/**
 * Use case for retrieving all lameness evaluations of a patient.
 *
 * @param lamenessRepository Repository instance for accessing lameness data.
 */
@Single
class GetLamenessListByPatientUseCase(
    @Provided private val lamenessRepository: ILamenessRepository,
) {
    /**
     * Retrieves all active lameness evaluations for the patient with the given [patientId],
     * ordered by lameness date descending.
     *
     * @param patientId The identifier of the patient.
     * @return The list of matching [Lameness] objects.
     */
    operator fun invoke(patientId: Long): List<Lameness> = lamenessRepository.getByPatient(patientId)
}
