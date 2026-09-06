package com.github.rodrigotimoteo.animally.domain.customreminder.usecase

import com.github.rodrigotimoteo.animally.domain.common.RecordType
import com.github.rodrigotimoteo.animally.domain.customreminder.ICustomReminderRepository
import com.github.rodrigotimoteo.animally.domain.notification.ReminderScheduler
import com.github.rodrigotimoteo.animally.domain.reminder.model.Reminder
import com.github.rodrigotimoteo.animally.domain.search.ISearchRepository
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single
import kotlin.time.Clock

/**
 * Use case for deactivating a custom reminder.
 *
 * The reminder is soft-deleted by marking it inactive and cancelling its stable
 * platform notification id.
 *
 * @param customReminderRepository Repository instance for accessing custom reminder data.
 * @param reminderScheduler Scheduler used to cancel the reminder notification.
 * @param searchRepository Repository instance for the global search index.
 */
@Single
class DeleteCustomReminderUseCase(
    @Provided private val customReminderRepository: ICustomReminderRepository,
    @Provided private val reminderScheduler: ReminderScheduler,
    @Provided private val searchRepository: ISearchRepository,
) {
    /**
     * Marks the custom reminder identified by [id] as inactive.
     *
     * @param id the identifier of the custom reminder to deactivate.
     * @return the number of rows affected.
     */
    operator fun invoke(id: Long): Long {
        val reminder = customReminderRepository.getById(id)
        val rows = customReminderRepository.setInactive(id, Clock.System.now())
        if (rows > 0L) {
            searchRepository.deleteRecord(RecordType.CustomReminder.wireName, id)
            reminder?.let {
                reminderScheduler.cancel(
                    Reminder(
                        patientId = it.patientId,
                        patientName = "",
                        recordType = "Custom-$id",
                        title = it.title,
                        dueDate = it.dueDate,
                    ),
                )
            }
        }
        return rows
    }
}
