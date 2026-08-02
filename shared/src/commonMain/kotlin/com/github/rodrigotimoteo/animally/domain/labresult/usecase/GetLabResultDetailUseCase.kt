package com.github.rodrigotimoteo.animally.domain.labresult.usecase

import com.github.rodrigotimoteo.animally.domain.labresult.ILabResultRepository
import com.github.rodrigotimoteo.animally.domain.labresult.model.LabResult
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single

/**
 * Use case for retrieving detailed information about a specific lab result.
 *
 * @param labResultRepository Repository instance for accessing lab result data.
 */
@Single
class GetLabResultDetailUseCase(
    @Provided private val labResultRepository: ILabResultRepository,
) {
    /**
     * Retrieves detailed information for a lab result by its ID.
     *
     * @param id The unique identifier of the lab result to retrieve.
     * @return The [LabResult] object if found, or `null` if no lab result with the given ID exists.
     */
    operator fun invoke(id: Long): LabResult? = labResultRepository.getById(id)
}
