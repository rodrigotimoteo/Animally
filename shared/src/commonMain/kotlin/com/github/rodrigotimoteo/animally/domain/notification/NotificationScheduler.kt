package com.github.rodrigotimoteo.animally.domain.notification

import com.github.rodrigotimoteo.animally.domain.patient.usecase.CogginsAlert
import com.github.rodrigotimoteo.animally.domain.reminder.model.Reminder
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant

/** Default notification channel used for reminders. */
const val REMINDER_CHANNEL_ID = "reminders"

/**
 * Schedules platform notifications through KMPNotifier local notifications.
 *
 * The Android actual creates the notification channel on first use and schedules each reminder
 * on its due date. The iOS actual requests notification permission on first use and posts local
 * notifications.
 */
expect class NotificationScheduler() {
    /**
     * Posts a notification per [alerts] for Coggins tests requiring attention.
     *
     * @param alerts The Coggins alerts to notify the user about.
     */
    fun scheduleCogginsNotifications(alerts: List<CogginsAlert>)

    /**
     * Schedules a notification for [reminder] on its due date.
     *
     * @param reminder The reminder to notify the user about.
     * @param channelId The notification channel used on Android.
     */
    fun scheduleReminder(
        reminder: Reminder,
        channelId: String = REMINDER_CHANNEL_ID,
    )
}

/**
 * Epoch-millisecond fire time for [Reminder] notifications: 09:00 local time on the due date.
 *
 * Overdue reminders produce a timestamp in the past, which the platform schedulers deliver
 * immediately.
 */
fun Reminder.fireAt(zone: TimeZone): Long = dueDate.atTime(hour = 9, minute = 0).toInstant(zone).toEpochMilliseconds()

/**
 * Stable, non-negative notification id for [Reminder], derived from the patient and record type.
 *
 * Ids stay stable across calls so a re-run replaces the previously scheduled notification
 * instead of duplicating it.
 */
fun Reminder.notificationId(): Int {
    val hash = patientId * NOTIFICATION_ID_PRIME + recordType.hashCode()
    return (hash and NOTIFICATION_ID_MASK.toLong()).toInt()
}

private const val NOTIFICATION_ID_PRIME = 31L

private const val NOTIFICATION_ID_MASK = Int.MAX_VALUE
