package com.github.rodrigotimoteo.animally.domain.notification

import com.github.rodrigotimoteo.animally.domain.reminder.model.Reminder
import org.koin.core.annotation.Single

/**
 * Abstraction over [NotificationScheduler] used by persistence flows.
 *
 * Decouples reminder persistence from the final platform scheduler so it can be
 * substituted in tests. The default implementation delegates to [NotificationScheduler].
 */
interface ReminderScheduler {
    /**
     * Schedules a platform notification for [reminder].
     *
     * @param reminder The reminder to notify the user about.
     */
    fun schedule(reminder: Reminder)
}

/**
 * Default [ReminderScheduler] backed by the platform [NotificationScheduler].
 */
@Single
class NotificationReminderScheduler : ReminderScheduler {
    private val delegate = NotificationScheduler()

    override fun schedule(reminder: Reminder) {
        delegate.scheduleReminder(reminder)
    }
}
