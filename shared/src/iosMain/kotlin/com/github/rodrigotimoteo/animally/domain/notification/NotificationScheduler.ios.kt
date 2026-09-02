package com.github.rodrigotimoteo.animally.domain.notification

import com.github.rodrigotimoteo.animally.domain.patient.usecase.CogginsAlert
import com.github.rodrigotimoteo.animally.domain.reminder.model.Reminder
import com.mmk.kmpnotifier.local.LocalNotifications
import kotlinx.datetime.TimeZone

/**
 * iOS notification scheduler backed by KMPNotifier local notifications.
 *
 * Permission is requested on first use. Reminders are scheduled on the due date; Coggins
 * alerts are posted immediately.
 */
actual class NotificationScheduler {
    actual fun scheduleCogginsNotifications(alerts: List<CogginsAlert>) {
        if (alerts.isEmpty()) return
        ensureInitialized()
        alerts.forEachIndexed { index, _ ->
            LocalNotifications.notifier.notify(
                id = index,
                title = "Health reminder",
                body = "A health reminder needs your attention.",
            )
        }
    }

    actual fun scheduleReminder(
        reminder: Reminder,
        channelId: String,
    ) {
        ensureInitialized()
        LocalNotifications.notifier.notify {
            id = reminder.notificationId()
            title = "Scheduled reminder"
            body = "A scheduled reminder is due."
            scheduledAt = reminder.fireAt(TimeZone.currentSystemDefault())
        }
    }

    private fun ensureInitialized() {
        ensureKmpNotifierInitialized()
    }
}
