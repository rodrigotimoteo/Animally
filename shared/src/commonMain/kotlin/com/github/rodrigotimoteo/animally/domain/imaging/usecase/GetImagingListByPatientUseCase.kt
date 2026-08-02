package com.github.rodrigotimoteo.animally.domain.imaging.usecase

import com.github.rodrigotimoteo.animally.domain.imaging.IImagingRepository
import com.github.rodrigotimoteo.animally.domain.imaging.model.Imaging
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single

/**
 * Use case for retrieving all imaging records of a patient.
 *
 * @param imagingRepository Repository instance for accessing imaging data.
 */
@Single
class GetImagingListByPatientUseCase(
    @Provided private val imagingRepository: IImagingRepository,
) {
    /**
     * Retrieves all active imaging records for the patient with the given [patientId],
     * ordered by imaging date descending.
     *
     * @param patientId The identifier of the patient.
     * @return The list of matching [Imaging] objects.
     */
    operator fun invoke(patientId: Long): List<Imaging> = imagingRepository.getByPatient(patientId)
}
