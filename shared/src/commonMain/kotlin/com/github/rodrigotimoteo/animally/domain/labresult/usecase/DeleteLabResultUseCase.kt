package com.github.rodrigotimoteo.animally.domain.labresult.usecase

import com.github.rodrigotimoteo.animally.domain.common.RecordType
import com.github.rodrigotimoteo.animally.domain.labresult.ILabResultRepository
import com.github.rodrigotimoteo.animally.domain.search.ISearchRepository
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single
import kotlin.time.Clock

/**
 * Use case for soft-deleting a record by marking it inactive.
 *
 * @param labResultRepository Repository instance for accessing the records.
 */
@Single
class DeleteLabResultUseCase(
    @Provided private val labResultRepository: ILabResultRepository,
    @Provided private val searchRepository: ISearchRepository,
) {
    /**
     * Marks the record identified by [id] as inactive.
     *
     * @param id the identifier of the record to delete.
     */
    operator fun invoke(id: Long) {
        labResultRepository.setInactive(id, Clock.System.now())
        searchRepository.deleteRecord(RecordType.LabResult.wireName, id)
    }
}
