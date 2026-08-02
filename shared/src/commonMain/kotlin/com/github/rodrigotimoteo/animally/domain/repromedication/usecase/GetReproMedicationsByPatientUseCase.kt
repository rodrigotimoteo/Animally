package com.github.rodrigotimoteo.animally.domain.repromedication.usecase

import com.github.rodrigotimoteo.animally.domain.repromedication.IReproMedicationRepository
import com.github.rodrigotimoteo.animally.domain.repromedication.model.ReproMedication
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single

/**
 * Use case for retrieving all reproduction medications of a patient.
 *
 * @param reproMedicationRepository Repository instance for accessing reproduction medication data.
 */
@Single
class GetReproMedicationsByPatientUseCase(
    @Provided private val reproMedicationRepository: IReproMedicationRepository,
) {
    /**
     * Retrieves all active reproduction medications for the patient with the given [patientId],
     * ordered by administration date descending.
     *
     * @param patientId The identifier of the patient.
     * @return The list of matching [ReproMedication] objects.
     */
    operator fun invoke(patientId: Long): List<ReproMedication> = reproMedicationRepository.getByPatient(patientId)
}
