package com.github.rodrigotimoteo.animally.domain.repromedication.usecase

import com.github.rodrigotimoteo.animally.domain.repromedication.IReproMedicationRepository
import com.github.rodrigotimoteo.animally.domain.repromedication.model.ReproMedication
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single

/**
 * Use case for retrieving detailed information about a specific reproduction medication.
 *
 * @param reproMedicationRepository Repository instance for accessing reproduction medication data.
 */
@Single
class GetReproMedicationDetailUseCase(
    @Provided private val reproMedicationRepository: IReproMedicationRepository,
) {
    /**
     * Retrieves detailed information for a reproduction medication by its ID.
     *
     * @param id The unique identifier of the reproduction medication to retrieve.
     * @return The [ReproMedication] object if found, or `null` if no reproduction medication with the given ID exists.
     */
    operator fun invoke(id: Long): ReproMedication? = reproMedicationRepository.getById(id)
}
