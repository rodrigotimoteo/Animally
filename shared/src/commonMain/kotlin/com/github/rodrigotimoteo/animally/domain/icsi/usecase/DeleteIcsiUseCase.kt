package com.github.rodrigotimoteo.animally.domain.icsi.usecase

import com.github.rodrigotimoteo.animally.domain.icsi.IIcsiRepository
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single
import kotlin.time.Clock

/**
 * Use case for soft-deleting an ICSI record by marking it inactive.
 */
@Single
class DeleteIcsiUseCase(
    @Provided private val repository: IIcsiRepository,
) {
    /**
     * Marks the record identified by [id] as inactive.
     */
    operator fun invoke(id: Long) {
        repository.setInactive(id, Clock.System.now())
    }
}
