package com.github.rodrigotimoteo.animally.domain.farrier.usecase

import com.github.rodrigotimoteo.animally.domain.common.RecordType
import com.github.rodrigotimoteo.animally.domain.farrier.IFarrierVisitRepository
import com.github.rodrigotimoteo.animally.domain.search.ISearchRepository
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single
import kotlin.time.Clock

/**
 * Use case for soft-deleting a record by marking it inactive.
 *
 * @param farrierVisitRepository Repository instance for accessing the records.
 */
@Single
class DeleteFarrierVisitUseCase(
    @Provided private val farrierVisitRepository: IFarrierVisitRepository,
    @Provided private val searchRepository: ISearchRepository,
) {
    /**
     * Marks the record identified by [id] as inactive.
     *
     * @param id the identifier of the record to delete.
     */
    operator fun invoke(id: Long) {
        farrierVisitRepository.setInactive(id, Clock.System.now())
        searchRepository.deleteRecord(RecordType.FarrierVisit.wireName, id)
    }
}
