package com.github.rodrigotimoteo.animally.domain.timeline.provider

import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.domain.timeline.model.TimelineEntry

/**
 * Contributes timeline entries from one domain slice.
 *
 * @param patientId scope of the feed — `null` for global timeline
 * @param nameFor resolves owning patient display name for a given patient id
 * @param database database to read rows from
 * @param sink mutable destination that the provider appends to
 */
internal interface TimelineProvider {
    fun collect(
        patientId: Long?,
        nameFor: (Long) -> String,
        database: AnimallyDatabase,
        sink: MutableList<TimelineEntry>,
    )
}
