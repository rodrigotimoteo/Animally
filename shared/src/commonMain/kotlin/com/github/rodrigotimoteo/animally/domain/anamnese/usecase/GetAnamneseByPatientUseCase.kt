package com.github.rodrigotimoteo.animally.domain.anamnese.usecase

import com.github.rodrigotimoteo.animally.domain.anamnese.IAnamneseRepository
import com.github.rodrigotimoteo.animally.domain.anamnese.model.Anamnese
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single

/**
 * Use case for retrieving a patient's anamnese record.
 *
 * @param anamneseRepository Repository instance for accessing anamnese data.
 */
@Single
class GetAnamneseByPatientUseCase(
    @Provided private val anamneseRepository: IAnamneseRepository,
) {
    /**
     * Retrieves the anamnese record for the patient with the given [patientId].
     *
     * @param patientId The identifier of the patient.
     * @return The [Anamnese] object if found, or `null` if no record exists.
     */
    operator fun invoke(patientId: Long): Anamnese? = anamneseRepository.getByPatient(patientId)
}
