package com.github.rodrigotimoteo.animally.domain.owner.usecase

import com.github.rodrigotimoteo.animally.domain.owner.IOwnerRepository
import com.github.rodrigotimoteo.animally.domain.owner.model.Owner
import com.github.rodrigotimoteo.animally.domain.search.ISearchRepository
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single

/**
 * Use case for persisting a new or updated owner.
 *
 * A single save path for both create and edit flows: owners with `id == 0L`
 * are inserted, all others are updated.
 *
 * @param ownerRepository Repository instance for accessing owner data.
 * @param searchRepository Repository instance for the global search index.
 */
@Single
class SaveOwnerUseCase(
    @Provided private val ownerRepository: IOwnerRepository,
    @Provided private val searchRepository: ISearchRepository,
) {
    /**
     * Persists the given [owner] and returns the generated identifier for new owners.
     *
     * @param owner the owner to persist.
     * @return the id of the persisted owner.
     */
    operator fun invoke(owner: Owner): Long {
        val savedId =
            if (owner.id == 0L) {
                ownerRepository.insertOwner(owner)
            } else {
                ownerRepository.updateOwner(owner)
                owner.id
            }
        val searchableText =
            listOfNotNull(owner.name, owner.email, owner.phone, owner.address).joinToString(" ")
        searchRepository.indexRecord(
            recordType = ISearchRepository.TYPE_OWNER,
            patientId = 0L,
            recordId = savedId,
            date = null,
            searchableText = searchableText,
        )
        return savedId
    }
}
