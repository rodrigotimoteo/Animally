package com.github.rodrigotimoteo.animally.domain.notification

import com.github.rodrigotimoteo.animally.domain.patient.usecase.CogginsAlert

/**
 * Thin platform stub for scheduling Coggins expiry notifications.
 *
 * POC scope: the alerting use case and the settings listing are the deliverable; platform
 * notification scheduling is not implemented yet. Actuals are no-ops that compile on both
 * Android and iOS so the scheduling entry point exists for a later phase.
 */
expect class NotificationScheduler() {
    /**
     * Schedules platform notifications for the given [alerts].
     *
     * @param alerts The Coggins alerts to notify the user about.
     */
    fun scheduleCogginsNotifications(alerts: List<CogginsAlert>)
}
