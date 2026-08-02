package com.github.rodrigotimoteo.animally.domain.farrier.usecase

import com.github.rodrigotimoteo.animally.domain.farrier.IFarrierVisitRepository
import com.github.rodrigotimoteo.animally.domain.farrier.model.FarrierVisit
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single

/**
 * Use case for retrieving all farrier visits of a patient.
 *
 * @param farrierVisitRepository Repository instance for accessing farrier visit data.
 */
@Single
class GetFarrierVisitsByPatientUseCase(
    @Provided private val farrierVisitRepository: IFarrierVisitRepository,
) {
    /**
     * Retrieves all active farrier visits for the patient with the given [patientId].
     *
     * @param patientId The identifier of the patient.
     * @return The list of matching [FarrierVisit] objects.
     */
    operator fun invoke(patientId: Long): List<FarrierVisit> = farrierVisitRepository.getByPatient(patientId)
}
