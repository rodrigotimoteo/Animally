package com.github.rodrigotimoteo.animally.domain.follicle.usecase

import com.github.rodrigotimoteo.animally.domain.follicle.IFollicleRepository
import com.github.rodrigotimoteo.animally.domain.follicle.model.Follicle
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single

/**
 * Use case for retrieving all active follicles of an ultrasound.
 */
@Single
class GetFolliclesByUltrasoundUseCase(
    @Provided private val repository: IFollicleRepository,
) {
    /**
     * Returns all active follicles recorded for [ultrasoundId].
     */
    operator fun invoke(ultrasoundId: Long): List<Follicle> = repository.getByUltrasound(ultrasoundId)
}
