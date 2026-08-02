package com.github.rodrigotimoteo.animally.domain.farrier.usecase

import com.github.rodrigotimoteo.animally.domain.farrier.IFarrierVisitRepository
import com.github.rodrigotimoteo.animally.domain.farrier.model.FarrierVisit
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single

/**
 * Use case for retrieving detailed information about a specific farrier visit.
 *
 * @param farrierVisitRepository Repository instance for accessing farrier visit data.
 */
@Single
class GetFarrierVisitDetailUseCase(
    @Provided private val farrierVisitRepository: IFarrierVisitRepository,
) {
    /**
     * Retrieves detailed information for a farrier visit by its ID.
     *
     * @param id The unique identifier of the farrier visit to retrieve.
     * @return The [FarrierVisit] object if found, or `null` if no visit with the given ID exists.
     */
    operator fun invoke(id: Long): FarrierVisit? = farrierVisitRepository.getById(id)
}
