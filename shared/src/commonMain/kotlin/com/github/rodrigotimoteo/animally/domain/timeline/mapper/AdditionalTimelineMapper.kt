package com.github.rodrigotimoteo.animally.domain.timeline.mapper

import com.github.rodrigotimoteo.animally.domain.common.RecordType
import com.github.rodrigotimoteo.animally.domain.customreminder.model.CustomReminder
import com.github.rodrigotimoteo.animally.domain.embryotransfer.model.EmbryoTransfer
import com.github.rodrigotimoteo.animally.domain.icsi.model.Icsi
import com.github.rodrigotimoteo.animally.domain.timeline.model.TimelineEntry

/** Maps the record types that were previously reachable only through search or detail. */
internal fun CustomReminder.toTimelineEntry(patientName: String): TimelineEntry =
    TimelineEntry(
        patientId = patientId,
        patientName = patientName,
        recordType = RecordType.CustomReminder.displayName,
        recordId = id,
        date = dueDate,
        title = "Custom Reminder",
        subtitle = title,
    )

internal fun EmbryoTransfer.toTimelineEntry(patientName: String): TimelineEntry =
    TimelineEntry(
        patientId = patientId,
        patientName = patientName,
        recordType = RecordType.EmbryoTransfer.displayName,
        recordId = id,
        date = date,
        title = "Embryo Transfer",
        subtitle = "$embryoCount embryo${if (embryoCount == 1) "" else "s"}",
    )

internal fun Icsi.toTimelineEntry(patientName: String): TimelineEntry =
    TimelineEntry(
        patientId = patientId,
        patientName = patientName,
        recordType = RecordType.Icsi.displayName,
        recordId = id,
        date = date,
        title = "ICSI",
        subtitle = "$folliclesRecovered follicle${if (folliclesRecovered == 1) "" else "s"} recovered",
    )
