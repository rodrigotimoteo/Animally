package com.github.rodrigotimoteo.animally.domain.lameness.usecase

import com.github.rodrigotimoteo.animally.domain.common.RecordType
import com.github.rodrigotimoteo.animally.domain.lameness.ILamenessRepository
import com.github.rodrigotimoteo.animally.domain.search.ISearchRepository
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single
import kotlin.time.Clock

/**
 * Use case for soft-deleting a record by marking it inactive.
 *
 * @param lamenessRepository Repository instance for accessing the records.
 */
@Single
class DeleteLamenessUseCase(
    @Provided private val lamenessRepository: ILamenessRepository,
    @Provided private val searchRepository: ISearchRepository,
) {
    /**
     * Marks the record identified by [id] as inactive.
     *
     * @param id the identifier of the record to delete.
     */
    operator fun invoke(id: Long) {
        lamenessRepository.setInactive(id, Clock.System.now())
        searchRepository.deleteRecord(RecordType.Lameness.wireName, id)
    }
}
