package com.github.rodrigotimoteo.animally.domain.ultrasound.usecase

import com.github.rodrigotimoteo.animally.domain.ultrasound.IUltrasoundRepository
import com.github.rodrigotimoteo.animally.domain.ultrasound.model.Ultrasound
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single

/**
 * Use case for retrieving detailed information about a specific ultrasound.
 *
 * @param ultrasoundRepository Repository instance for accessing ultrasound data.
 */
@Single
class GetUltrasoundDetailUseCase(
    @Provided private val ultrasoundRepository: IUltrasoundRepository,
) {
    /**
     * Retrieves detailed information for an ultrasound by its ID.
     *
     * @param id The unique identifier of the ultrasound to retrieve.
     * @return The [Ultrasound] object if found, or `null` if no ultrasound with the given ID exists.
     */
    operator fun invoke(id: Long): Ultrasound? = ultrasoundRepository.getById(id)
}
