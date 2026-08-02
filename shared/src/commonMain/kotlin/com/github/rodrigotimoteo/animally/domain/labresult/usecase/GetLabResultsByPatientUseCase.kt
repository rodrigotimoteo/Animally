package com.github.rodrigotimoteo.animally.domain.labresult.usecase

import com.github.rodrigotimoteo.animally.domain.labresult.ILabResultRepository
import com.github.rodrigotimoteo.animally.domain.labresult.model.LabResult
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single

/**
 * Use case for retrieving all lab results of a patient.
 *
 * @param labResultRepository Repository instance for accessing lab result data.
 */
@Single
class GetLabResultsByPatientUseCase(
    @Provided private val labResultRepository: ILabResultRepository,
) {
    /**
     * Retrieves all active lab results for the patient with the given [patientId],
     * ordered by test date descending.
     *
     * @param patientId The identifier of the patient.
     * @return The list of matching [LabResult] objects.
     */
    operator fun invoke(patientId: Long): List<LabResult> = labResultRepository.getByPatient(patientId)
}
