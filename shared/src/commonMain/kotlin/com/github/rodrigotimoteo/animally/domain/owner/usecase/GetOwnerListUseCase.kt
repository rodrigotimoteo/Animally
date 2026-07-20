package com.github.rodrigotimoteo.animally.domain.owner.usecase

import com.github.rodrigotimoteo.animally.domain.owner.IOwnerRepository
import com.github.rodrigotimoteo.animally.domain.owner.model.Owner
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single

/**
 * Use case for retrieving a list of all owners.
 *
 * This use case is responsible for fetching all owner data from the repository
 * and is typically used in presentation layers to display a list of owners.
 *
 * @param ownerRepository Repository instance for accessing owner data.
 */
@Single
class GetOwnerListUseCase(
    @Provided private val ownerRepository: IOwnerRepository,
) {
    /**
     * Retrieves a list of all owners.
     *
     * @return A list of [Owner] objects containing all owners in the system.
     */
    operator fun invoke(): List<Owner> = ownerRepository.getOwnerList()
}
