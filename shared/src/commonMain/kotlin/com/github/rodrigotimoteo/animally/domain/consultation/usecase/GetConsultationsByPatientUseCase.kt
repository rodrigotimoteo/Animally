package com.github.rodrigotimoteo.animally.domain.consultation.usecase

import com.github.rodrigotimoteo.animally.domain.consultation.IConsultationRepository
import com.github.rodrigotimoteo.animally.domain.consultation.model.Consultation
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single

/**
 * Use case for retrieving all consultations of a patient.
 *
 * @param consultationRepository Repository instance for accessing consultation data.
 */
@Single
class GetConsultationsByPatientUseCase(
    @Provided private val consultationRepository: IConsultationRepository,
) {
    /**
     * Retrieves all active consultations for the patient with the given [patientId],
     * ordered by consultation date descending.
     *
     * @param patientId The identifier of the patient.
     * @return The list of matching [Consultation] objects.
     */
    operator fun invoke(patientId: Long): List<Consultation> = consultationRepository.getByPatient(patientId)
}
