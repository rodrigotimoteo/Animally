package com.github.rodrigotimoteo.animally.domain.substance.usecase

import com.github.rodrigotimoteo.animally.domain.substance.IControlledSubstanceRepository
import com.github.rodrigotimoteo.animally.domain.substance.model.ControlledSubstance
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single

/**
 * Use case for retrieving detailed information about a specific controlled substance record.
 *
 * @param substanceRepository Repository instance for accessing controlled substance data.
 */
@Single
class GetControlledSubstanceDetailUseCase(
    @Provided private val substanceRepository: IControlledSubstanceRepository,
) {
    /**
     * Retrieves detailed information for a controlled substance record by its ID.
     *
     * @param id The unique identifier of the controlled substance record to retrieve.
     * @return The [ControlledSubstance] object if found, or `null` if no record with the given ID exists.
     */
    operator fun invoke(id: Long): ControlledSubstance? = substanceRepository.getById(id)
}
