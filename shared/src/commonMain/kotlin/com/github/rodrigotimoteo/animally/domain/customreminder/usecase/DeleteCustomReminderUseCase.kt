package com.github.rodrigotimoteo.animally.domain.customreminder.usecase

import com.github.rodrigotimoteo.animally.domain.common.RecordType
import com.github.rodrigotimoteo.animally.domain.customreminder.ICustomReminderRepository
import com.github.rodrigotimoteo.animally.domain.search.ISearchRepository
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
 * @param searchRepository Repository instance for the global search index.
 */
@Single
class DeleteCustomReminderUseCase(
    @Provided private val customReminderRepository: ICustomReminderRepository,
    @Provided private val searchRepository: ISearchRepository,
) {
    /**
     * Marks the custom reminder identified by [id] as inactive.
     *
     * @param id the identifier of the custom reminder to deactivate.
     * @return the number of rows affected.
     */
    operator fun invoke(id: Long): Long {
        val rows = customReminderRepository.setInactive(id, Clock.System.now())
        if (rows > 0L) {
            searchRepository.deleteRecord(RecordType.CustomReminder.wireName, id)
        }
        return rows
    }
}
