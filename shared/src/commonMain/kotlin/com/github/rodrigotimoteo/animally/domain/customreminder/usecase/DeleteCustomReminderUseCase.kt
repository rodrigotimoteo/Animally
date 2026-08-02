package com.github.rodrigotimoteo.animally.domain.customreminder.usecase

import com.github.rodrigotimoteo.animally.domain.customreminder.ICustomReminderRepository
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single
import kotlin.time.Clock

/**
 * Use case for deactivating a custom reminder.
 *
 * The reminder is soft-deleted by marking it inactive. Cancelling the already-scheduled
 * platform notification is a no-op for now; the notification id stays stable so a later
 * re-save with the same id replaces it.
 *
 * @param customReminderRepository Repository instance for accessing custom reminder data.
 */
@Single
class DeleteCustomReminderUseCase(
    @Provided private val customReminderRepository: ICustomReminderRepository,
) {
    /**
     * Marks the custom reminder identified by [id] as inactive.
     *
     * @param id the identifier of the custom reminder to deactivate.
     * @return the number of rows affected.
     */
    operator fun invoke(id: Long): Long = customReminderRepository.setInactive(id, Clock.System.now())
}
