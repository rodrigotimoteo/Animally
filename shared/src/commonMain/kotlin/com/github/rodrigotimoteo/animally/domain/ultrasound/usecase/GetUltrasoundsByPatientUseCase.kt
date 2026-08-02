package com.github.rodrigotimoteo.animally.domain.ultrasound.usecase

import com.github.rodrigotimoteo.animally.domain.ultrasound.IUltrasoundRepository
import com.github.rodrigotimoteo.animally.domain.ultrasound.model.Ultrasound
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single

/**
 * Use case for retrieving all ultrasounds of a patient.
 *
 * @param ultrasoundRepository Repository instance for accessing ultrasound data.
 */
@Single
class GetUltrasoundsByPatientUseCase(
    @Provided private val ultrasoundRepository: IUltrasoundRepository,
) {
    /**
     * Retrieves all active ultrasounds for the patient with the given [patientId],
     * ordered by examination date descending.
     *
     * @param patientId The identifier of the patient.
     * @return The list of matching [Ultrasound] objects.
     */
    operator fun invoke(patientId: Long): List<Ultrasound> = ultrasoundRepository.getByPatient(patientId)
}
