package com.github.rodrigotimoteo.animally.domain.notification

import com.github.rodrigotimoteo.animally.domain.patient.usecase.CogginsAlert

/**
 * POC stub: Android notification scheduling is deferred to a later phase.
 */
actual class NotificationScheduler {
    /**
     * No-op for the POC. Real AlarmManager scheduling arrives with the notification phase.
     */
    actual fun scheduleCogginsNotifications(alerts: List<CogginsAlert>) = Unit
}
