package com.github.rodrigotimoteo.animally.domain.customreminder.usecase

import com.github.rodrigotimoteo.animally.domain.common.RecordType
import com.github.rodrigotimoteo.animally.domain.customreminder.ICustomReminderRepository
import com.github.rodrigotimoteo.animally.domain.customreminder.model.CustomReminder
import com.github.rodrigotimoteo.animally.domain.notification.ReminderScheduler
import com.github.rodrigotimoteo.animally.domain.reminder.model.Reminder
import com.github.rodrigotimoteo.animally.domain.search.ISearchRepository
import com.github.rodrigotimoteo.animally.domain.search.SearchableText
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single

/**
 * Use case for persisting a new or updated custom reminder.
 *
 * A single save path for both create and edit flows: reminders with `id == 0L`
 * are inserted, all others are updated. After persisting, a platform notification
 * is scheduled for the reminder's due date.
 *
 * @param customReminderRepository Repository instance for accessing custom reminder data.
 * @param reminderScheduler Scheduler used to schedule the reminder notification.
 * @param searchRepository Repository instance for the global search index.
 */
@Single
class SaveCustomReminderUseCase(
    @Provided private val customReminderRepository: ICustomReminderRepository,
    @Provided private val reminderScheduler: ReminderScheduler,
    @Provided private val searchRepository: ISearchRepository,
) {
    /**
     * Persists the given [customReminder] and returns the generated identifier for new reminders.
     *
     * The persisted id is embedded in the derived reminder's record type so that each custom
     * reminder maps to a stable, unique notification id.
     *
     * @param customReminder the custom reminder to persist.
     * @return the id of the persisted custom reminder.
     */
    operator fun invoke(customReminder: CustomReminder): Long {
        val isNew = customReminder.id == 0L
        val persistedId =
            if (isNew) {
                customReminderRepository.insert(customReminder)
            } else {
                customReminderRepository.update(customReminder)
                customReminder.id
            }
        searchRepository.indexRecord(
            recordType = RecordType.CustomReminder.wireName,
            patientId = customReminder.patientId,
            recordId = persistedId,
            date = customReminder.dueDate,
            searchableText = SearchableText.customReminder(customReminder),
        )
        reminderScheduler.schedule(
            Reminder(
                patientId = customReminder.patientId,
                patientName = "",
                recordType = "Custom-$persistedId",
                title = customReminder.title,
                dueDate = customReminder.dueDate,
            ),
        )
        return persistedId
    }
}
