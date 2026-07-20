package com.github.rodrigotimoteo.animally.domain.owner.usecase

import com.github.rodrigotimoteo.animally.domain.owner.IOwnerRepository
import com.github.rodrigotimoteo.animally.domain.owner.model.Owner
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single

/**
 * Use case for retrieving detailed information about a specific owner.
 *
 * This use case is responsible for fetching owner data from the repository
 * and is typically used in presentation layers to display owner details.
 *
 * @param ownerRepository Repository instance for accessing owner data.
 */
@Single
class GetOwnerDetailUseCase(
    @Provided private val ownerRepository: IOwnerRepository,
) {
    /**
     * Retrieves detailed information for an owner by their ID.
     *
     * @param id The unique identifier of the owner to retrieve.
     * @return The [Owner] object if found, or `null` if no owner with the given ID exists.
     */
    operator fun invoke(id: Long): Owner? = ownerRepository.getOwnerById(id)
}
