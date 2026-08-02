package com.github.rodrigotimoteo.animally.domain.medication.usecase

import com.github.rodrigotimoteo.animally.domain.medication.IMedicationRepository
import com.github.rodrigotimoteo.animally.domain.medication.model.Medication
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single

/**
 * Use case for retrieving detailed information about a specific medication.
 *
 * @param medicationRepository Repository instance for accessing medication data.
 */
@Single
class GetMedicationDetailUseCase(
    @Provided private val medicationRepository: IMedicationRepository,
) {
    /**
     * Retrieves detailed information for a medication by its ID.
     *
     * @param id The unique identifier of the medication to retrieve.
     * @return The [Medication] object if found, or `null` if no medication with the given ID exists.
     */
    operator fun invoke(id: Long): Medication? = medicationRepository.getById(id)
}
