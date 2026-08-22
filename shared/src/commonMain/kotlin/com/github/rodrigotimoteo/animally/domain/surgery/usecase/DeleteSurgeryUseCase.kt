package com.github.rodrigotimoteo.animally.domain.surgery.usecase

import com.github.rodrigotimoteo.animally.domain.surgery.ISurgeryRepository
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single
import kotlin.time.Clock

/**
 * Use case for soft-deleting a record by marking it inactive.
 *
 * @param surgeryRepository Repository instance for accessing the records.
 */
@Single
class DeleteSurgeryUseCase(
    @Provided private val surgeryRepository: ISurgeryRepository,
) {
    /**
     * Marks the record identified by [id] as inactive.
     *
     * @param id the identifier of the record to delete.
     */
    operator fun invoke(id: Long) {
        surgeryRepository.setInactive(id, Clock.System.now())
    }
}
