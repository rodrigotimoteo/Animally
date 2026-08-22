package com.github.rodrigotimoteo.animally.domain.follicle.usecase

import com.github.rodrigotimoteo.animally.domain.follicle.IFollicleRepository
import com.github.rodrigotimoteo.animally.domain.follicle.model.Follicle
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single
import kotlin.time.Clock

/**
 * Use case for replacing the follicle set of an ultrasound.
 *
 * The full desired list is provided on every save: existing rows not present
 * in [follicles] are soft-deleted, rows with ids are updated, rows without
 * ids are inserted.
 */
@Single
class SaveFolliclesUseCase(
    @Provided private val repository: IFollicleRepository,
) {
    /**
     * Persists [follicles] as the complete follicle set of [ultrasoundId].
     */
    operator fun invoke(
        ultrasoundId: Long,
        follicles: List<Follicle>,
    ) {
        val now = Clock.System.now()
        val existing = repository.getByUltrasound(ultrasoundId)
        val keptIds = follicles.mapNotNull { it.id.takeIf { id -> id != 0L } }.toSet()
        existing
            .filter { it.id !in keptIds }
            .forEach { repository.setInactive(it.id, now) }
        follicles.forEach { follicle ->
            if (follicle.id == 0L) {
                repository.insert(follicle.copy(ultrasoundId = ultrasoundId))
            } else {
                repository.update(follicle.copy(ultrasoundId = ultrasoundId))
            }
        }
    }
}
