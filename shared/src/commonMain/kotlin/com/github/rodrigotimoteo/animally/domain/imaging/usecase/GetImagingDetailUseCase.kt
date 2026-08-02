package com.github.rodrigotimoteo.animally.domain.imaging.usecase

import com.github.rodrigotimoteo.animally.domain.imaging.IImagingRepository
import com.github.rodrigotimoteo.animally.domain.imaging.model.Imaging
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single

/**
 * Use case for retrieving detailed information about a specific imaging record.
 *
 * @param imagingRepository Repository instance for accessing imaging data.
 */
@Single
class GetImagingDetailUseCase(
    @Provided private val imagingRepository: IImagingRepository,
) {
    /**
     * Retrieves detailed information for an imaging record by its ID.
     *
     * @param id The unique identifier of the imaging record to retrieve.
     * @return The [Imaging] object if found, or `null` if no imaging record with the given ID exists.
     */
    operator fun invoke(id: Long): Imaging? = imagingRepository.getById(id)
}
