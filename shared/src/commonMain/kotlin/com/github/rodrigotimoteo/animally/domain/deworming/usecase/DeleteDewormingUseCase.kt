package com.github.rodrigotimoteo.animally.domain.deworming.usecase

import com.github.rodrigotimoteo.animally.domain.deworming.IDewormingRepository
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single
import kotlin.time.Clock

/**
 * Use case for soft-deleting a record by marking it inactive.
 *
 * @param dewormingRepository Repository instance for accessing the records.
 */
@Single
class DeleteDewormingUseCase(
    @Provided private val dewormingRepository: IDewormingRepository,
) {
    /**
     * Marks the record identified by [id] as inactive.
     *
     * @param id the identifier of the record to delete.
     */
    operator fun invoke(id: Long) {
        dewormingRepository.setInactive(id, Clock.System.now())
    }
}
