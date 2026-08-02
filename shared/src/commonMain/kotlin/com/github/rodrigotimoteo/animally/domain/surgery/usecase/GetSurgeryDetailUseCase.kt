package com.github.rodrigotimoteo.animally.domain.surgery.usecase

import com.github.rodrigotimoteo.animally.domain.surgery.ISurgeryRepository
import com.github.rodrigotimoteo.animally.domain.surgery.model.Surgery
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single

/**
 * Use case for retrieving detailed information about a specific surgery.
 *
 * @param surgeryRepository Repository instance for accessing surgery data.
 */
@Single
class GetSurgeryDetailUseCase(
    @Provided private val surgeryRepository: ISurgeryRepository,
) {
    /**
     * Retrieves detailed information for a surgery by its ID.
     *
     * @param id The unique identifier of the surgery to retrieve.
     * @return The [Surgery] object if found, or `null` if no surgery with the given ID exists.
     */
    operator fun invoke(id: Long): Surgery? = surgeryRepository.getById(id)
}
