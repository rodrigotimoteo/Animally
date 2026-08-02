package com.github.rodrigotimoteo.animally.domain.gestation.usecase

import com.github.rodrigotimoteo.animally.domain.gestation.IGestationRepository
import com.github.rodrigotimoteo.animally.domain.gestation.model.Gestation
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single

/**
 * Use case for retrieving detailed information about a specific gestation record.
 *
 * @param gestationRepository Repository instance for accessing gestation data.
 */
@Single
class GetGestationDetailUseCase(
    @Provided private val gestationRepository: IGestationRepository,
) {
    /**
     * Retrieves detailed information for a gestation record by its ID.
     *
     * @param id The unique identifier of the gestation record to retrieve.
     * @return The [Gestation] object if found, or `null` if no gestation record with the given ID exists.
     */
    operator fun invoke(id: Long): Gestation? = gestationRepository.getById(id)
}
