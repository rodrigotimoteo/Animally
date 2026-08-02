package com.github.rodrigotimoteo.animally.domain.timeline.model

/**
 * The aggregated timeline feed, grouped by date in descending order.
 *
 * @property patientId The identifier of the patient the feed belongs to, or `null` for the global feed.
 * @property patientName The display name of the patient, or `null` for the global feed.
 * @property groups The timeline groups, sorted by date descending.
 */
data class TimelineFeed(
    val patientId: Long?,
    val patientName: String?,
    val groups: List<TimelineGroup>,
)
