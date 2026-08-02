package com.github.rodrigotimoteo.animally.domain.ultrasound.usecase

import com.github.rodrigotimoteo.animally.domain.ultrasound.IUltrasoundRepository
import com.github.rodrigotimoteo.animally.domain.ultrasound.model.Ultrasound
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single

/**
 * Use case for persisting a new or updated ultrasound.
 *
 * A single save path for both create and edit flows: ultrasounds with `id == 0L`
 * are inserted, all others are updated.
 *
 * @param ultrasoundRepository Repository instance for accessing ultrasound data.
 */
@Single
class SaveUltrasoundUseCase(
    @Provided private val ultrasoundRepository: IUltrasoundRepository,
) {
    /**
     * Persists the given [ultrasound] and returns the generated identifier for new ultrasounds.
     *
     * @param ultrasound the ultrasound to persist.
     * @return the id of the persisted ultrasound.
     */
    operator fun invoke(ultrasound: Ultrasound): Long =
        if (ultrasound.id == 0L) {
            ultrasoundRepository.insert(ultrasound)
        } else {
            ultrasoundRepository.update(ultrasound)
        }
}
