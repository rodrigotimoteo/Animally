package com.github.rodrigotimoteo.animally.domain.lameness.usecase

import com.github.rodrigotimoteo.animally.domain.lameness.ILamenessRepository
import com.github.rodrigotimoteo.animally.domain.lameness.model.Lameness
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single

/**
 * Use case for retrieving detailed information about a specific lameness evaluation.
 *
 * @param lamenessRepository Repository instance for accessing lameness data.
 */
@Single
class GetLamenessDetailUseCase(
    @Provided private val lamenessRepository: ILamenessRepository,
) {
    /**
     * Retrieves detailed information for a lameness evaluation by its ID.
     *
     * @param id The unique identifier of the lameness evaluation to retrieve.
     * @return The [Lameness] object if found, or `null` if no lameness evaluation with the given ID exists.
     */
    operator fun invoke(id: Long): Lameness? = lamenessRepository.getById(id)
}
