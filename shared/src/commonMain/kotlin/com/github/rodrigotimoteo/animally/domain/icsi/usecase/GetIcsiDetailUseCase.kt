package com.github.rodrigotimoteo.animally.domain.icsi.usecase

import com.github.rodrigotimoteo.animally.domain.icsi.IIcsiRepository
import com.github.rodrigotimoteo.animally.domain.icsi.model.Icsi
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single

/**
 * Use case for retrieving a single ICSI record.
 */
@Single
class GetIcsiDetailUseCase(
    @Provided private val repository: IIcsiRepository,
) {
    /**
     * Returns the active record with [id], or `null` when not found.
     */
    operator fun invoke(id: Long): Icsi? = repository.getById(id)
}
