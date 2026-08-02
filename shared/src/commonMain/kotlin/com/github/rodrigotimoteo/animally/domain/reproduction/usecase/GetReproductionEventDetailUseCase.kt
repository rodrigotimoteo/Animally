package com.github.rodrigotimoteo.animally.domain.reproduction.usecase

import com.github.rodrigotimoteo.animally.domain.reproduction.IReproductionRepository
import com.github.rodrigotimoteo.animally.domain.reproduction.model.ReproductionEvent
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single

/**
 * Use case for retrieving detailed information about a specific reproduction event.
 *
 * @param reproductionRepository Repository instance for accessing reproduction event data.
 */
@Single
class GetReproductionEventDetailUseCase(
    @Provided private val reproductionRepository: IReproductionRepository,
) {
    /**
     * Retrieves detailed information for a reproduction event by its ID.
     *
     * @param id The unique identifier of the reproduction event to retrieve.
     * @return The [ReproductionEvent] object if found, or `null` if no reproduction event with the given ID exists.
     */
    operator fun invoke(id: Long): ReproductionEvent? = reproductionRepository.getById(id)
}
