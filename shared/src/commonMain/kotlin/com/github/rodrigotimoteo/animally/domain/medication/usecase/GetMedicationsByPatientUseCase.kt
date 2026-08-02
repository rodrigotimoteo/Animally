package com.github.rodrigotimoteo.animally.domain.medication.usecase

import com.github.rodrigotimoteo.animally.domain.medication.IMedicationRepository
import com.github.rodrigotimoteo.animally.domain.medication.model.Medication
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single

/**
 * Use case for retrieving all medications of a patient.
 *
 * @param medicationRepository Repository instance for accessing medication data.
 */
@Single
class GetMedicationsByPatientUseCase(
    @Provided private val medicationRepository: IMedicationRepository,
) {
    /**
     * Retrieves all active medications for the patient with the given [patientId],
     * ordered by medication start date descending.
     *
     * @param patientId The identifier of the patient.
     * @return The list of matching [Medication] objects.
     */
    operator fun invoke(patientId: Long): List<Medication> = medicationRepository.getByPatient(patientId)
}
