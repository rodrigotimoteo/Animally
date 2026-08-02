package com.github.rodrigotimoteo.animally.domain.notification

import com.github.rodrigotimoteo.animally.domain.patient.usecase.CogginsAlert
import com.github.rodrigotimoteo.animally.domain.reminder.model.Reminder
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
        alerts.forEachIndexed { index, alert ->
            LocalNotifications.notifier.notify(
                id = index,
                title = "Coggins ${alert.status.name}",
                body = "${alert.patient.name} — Coggins expires ${alert.expiryDate}",
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
            title = reminder.title
            body = "${reminder.recordType} due ${reminder.dueDate} — ${reminder.patientName}"
            scheduledAt = reminder.fireAt(TimeZone.currentSystemDefault())
        }
    }

    private fun ensureInitialized() {
        ensureKmpNotifierInitialized()
    }
}
