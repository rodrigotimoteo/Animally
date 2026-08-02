package com.github.rodrigotimoteo.animally.domain.weight.usecase

import com.github.rodrigotimoteo.animally.domain.weight.IWeightRepository
import com.github.rodrigotimoteo.animally.domain.weight.model.Weight
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single

/**
 * Use case for retrieving detailed information about a specific weight entry.
 *
 * @param weightRepository Repository instance for accessing weight data.
 */
@Single
class GetWeightDetailUseCase(
    @Provided private val weightRepository: IWeightRepository,
) {
    /**
     * Retrieves detailed information for a weight entry by its ID.
     *
     * @param id The unique identifier of the weight entry to retrieve.
     * @return The [Weight] object if found, or `null` if no entry with the given ID exists.
     */
    operator fun invoke(id: Long): Weight? = weightRepository.getById(id)
}
