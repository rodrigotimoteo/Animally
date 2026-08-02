package com.github.rodrigotimoteo.animally.domain.deworming.usecase

import com.github.rodrigotimoteo.animally.domain.deworming.IDewormingRepository
import com.github.rodrigotimoteo.animally.domain.deworming.model.Deworming
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single

/**
 * Use case for retrieving detailed information about a specific deworming record.
 *
 * @param dewormingRepository Repository instance for accessing deworming data.
 */
@Single
class GetDewormingDetailUseCase(
    @Provided private val dewormingRepository: IDewormingRepository,
) {
    /**
     * Retrieves detailed information for a deworming record by its ID.
     *
     * @param id The unique identifier of the deworming record to retrieve.
     * @return The [Deworming] object if found, or `null` if no record with the given ID exists.
     */
    operator fun invoke(id: Long): Deworming? = dewormingRepository.getById(id)
}
