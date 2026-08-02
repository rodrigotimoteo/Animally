package com.github.rodrigotimoteo.animally.domain.imaging.usecase

import com.github.rodrigotimoteo.animally.domain.imaging.IImagingRepository
import com.github.rodrigotimoteo.animally.domain.imaging.model.Imaging
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single

/**
 * Use case for persisting a new or updated imaging record.
 *
 * A single save path for both create and edit flows: imaging records with `id == 0L`
 * are inserted, all others are updated.
 *
 * @param imagingRepository Repository instance for accessing imaging data.
 */
@Single
class SaveImagingUseCase(
    @Provided private val imagingRepository: IImagingRepository,
) {
    /**
     * Persists the given [imaging] and returns the generated identifier for new imaging records.
     *
     * @param imaging the imaging record to persist.
     * @return the id of the persisted imaging record.
     */
    operator fun invoke(imaging: Imaging): Long =
        if (imaging.id == 0L) {
            imagingRepository.insert(imaging)
        } else {
            imagingRepository.update(imaging)
        }
}
