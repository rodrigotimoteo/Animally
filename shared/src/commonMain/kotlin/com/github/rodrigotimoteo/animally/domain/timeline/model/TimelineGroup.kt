package com.github.rodrigotimoteo.animally.domain.timeline.model

import kotlinx.datetime.LocalDate

/**
 * A group of timeline entries sharing the same date.
 *
 * @property date The date shared by all entries in the group.
 * @property entries The entries aggregated under [date], in deterministic record-type order.
 */
data class TimelineGroup(
    val date: LocalDate,
    val entries: List<TimelineEntry>,
)
