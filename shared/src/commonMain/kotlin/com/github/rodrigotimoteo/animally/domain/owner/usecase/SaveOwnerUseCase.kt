package com.github.rodrigotimoteo.animally.domain.owner.usecase

import com.github.rodrigotimoteo.animally.domain.owner.IOwnerRepository
import com.github.rodrigotimoteo.animally.domain.owner.model.Owner
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single

/**
 * Use case for persisting a new or updated owner.
 *
 * A single save path for both create and edit flows: owners with `id == 0L`
 * are inserted, all others are updated.
 *
 * @param ownerRepository Repository instance for accessing owner data.
 */
@Single
class SaveOwnerUseCase(
    @Provided private val ownerRepository: IOwnerRepository,
) {
    /**
     * Persists the given [owner] and returns the generated identifier for new owners.
     *
     * @param owner the owner to persist.
     * @return the id of the persisted owner.
     */
    operator fun invoke(owner: Owner): Long =
        if (owner.id == 0L) {
            ownerRepository.insertOwner(owner)
        } else {
            ownerRepository.updateOwner(owner)
        }
}
