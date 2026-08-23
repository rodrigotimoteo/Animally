package com.github.rodrigotimoteo.animally.domain.imaging.usecase

import com.github.rodrigotimoteo.animally.domain.common.RecordType
import com.github.rodrigotimoteo.animally.domain.imaging.IImagingRepository
import com.github.rodrigotimoteo.animally.domain.search.ISearchRepository
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single
import kotlin.time.Clock

/**
 * Use case for soft-deleting a record by marking it inactive.
 *
 * @param imagingRepository Repository instance for accessing the records.
 */
@Single
class DeleteImagingUseCase(
    @Provided private val imagingRepository: IImagingRepository,
    @Provided private val searchRepository: ISearchRepository,
) {
    /**
     * Marks the record identified by [id] as inactive.
     *
     * @param id the identifier of the record to delete.
     */
    operator fun invoke(id: Long) {
        imagingRepository.setInactive(id, Clock.System.now())
        searchRepository.deleteRecord(RecordType.Imaging.wireName, id)
    }
}
