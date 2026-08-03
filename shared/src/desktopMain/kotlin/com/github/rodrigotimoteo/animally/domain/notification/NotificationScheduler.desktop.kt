@file:Suppress("ktlint:standard:filename")

package com.github.rodrigotimoteo.animally.domain.notification

import com.github.rodrigotimoteo.animally.domain.patient.usecase.CogginsAlert
import com.github.rodrigotimoteo.animally.domain.reminder.model.Reminder

/**
 * Desktop no-op scheduler — local notifications are not supported on the
 * desktop target for the POC.
 */
actual class NotificationScheduler {
    actual fun scheduleCogginsNotifications(alerts: List<CogginsAlert>) = Unit

    actual fun scheduleReminder(
        reminder: Reminder,
        channelId: String,
    ) = Unit
}
