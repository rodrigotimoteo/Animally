package com.github.rodrigotimoteo.animally.domain.notification

import com.github.rodrigotimoteo.animally.domain.patient.usecase.CogginsAlert
import com.github.rodrigotimoteo.animally.domain.reminder.model.Reminder
import com.github.rodrigotimoteo.animally.shared.R
import com.mmk.kmpnotifier.KMPNotifier
import com.mmk.kmpnotifier.local.LocalNotifications
import com.mmk.kmpnotifier.notification.configuration.NotificationPlatformConfiguration
import kotlinx.datetime.TimeZone

/**
 * Android notification scheduler backed by KMPNotifier local notifications.
 *
 * The notification channel is created on first use through the KMPNotifier Android
 * configuration. Reminders are scheduled on the due date through the platform alarm
 * scheduler; Coggins alerts are posted immediately.
 */
actual class NotificationScheduler {
    actual fun scheduleCogginsNotifications(alerts: List<CogginsAlert>) {
        if (alerts.isEmpty()) return
        ensureInitialized(REMINDER_CHANNEL_ID)
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
        ensureInitialized(channelId)
        LocalNotifications.notifier.notify {
            id = reminder.notificationId()
            title = reminder.title
            body = "${reminder.recordType} due ${reminder.dueDate} — ${reminder.patientName}"
            scheduledAt = reminder.fireAt(TimeZone.currentSystemDefault())
        }
    }

    private fun ensureInitialized(channelId: String) {
        if (KMPNotifier.isInitialized) return
        val channel = channelId.takeIf { it.isNotBlank() } ?: REMINDER_CHANNEL_ID
        val channelData =
            NotificationPlatformConfiguration.Android.NotificationChannelData(
                id = channel,
                name = "Reminders",
                description = "Vaccination and dentistry due-date reminders",
                soundUri = null,
            )
        KMPNotifier.initialize(
            NotificationPlatformConfiguration.Android(
                notificationIconResId = R.drawable.ic_stat_reminder,
                notificationIconColorResId = null,
                notificationChannelData = channelData,
                showPushNotification = true,
            ),
            LocalNotifications,
        )
    }
}
