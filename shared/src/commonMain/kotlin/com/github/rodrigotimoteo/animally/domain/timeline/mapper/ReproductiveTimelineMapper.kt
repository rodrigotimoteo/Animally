package com.github.rodrigotimoteo.animally.domain.timeline.mapper

import com.github.rodrigotimoteo.animally.domain.common.RecordType
import com.github.rodrigotimoteo.animally.domain.gestation.model.Gestation
import com.github.rodrigotimoteo.animally.domain.reproduction.model.ReproductionEvent
import com.github.rodrigotimoteo.animally.domain.repromedication.model.ReproMedication
import com.github.rodrigotimoteo.animally.domain.timeline.model.TimelineEntry
import com.github.rodrigotimoteo.animally.domain.ultrasound.model.Ultrasound

/**
 * Maps reproduction record domain models to [TimelineEntry] instances.
 *
 * @param patientName The display name of the owning patient.
 * @return The mapped [TimelineEntry].
 */
internal fun ReproductionEvent.toTimelineEntry(patientName: String): TimelineEntry =
    TimelineEntry(
        patientId = patientId,
        patientName = patientName,
        recordType = RecordType.ReproductionEvent.displayName,
        recordId = id,
        date = date,
        title = "Reproduction",
        subtitle = eventType,
    )

internal fun Ultrasound.toTimelineEntry(patientName: String): TimelineEntry =
    TimelineEntry(
        patientId = patientId,
        patientName = patientName,
        recordType = RecordType.Ultrasound.displayName,
        recordId = id,
        date = date,
        title = "Ultrasound",
        subtitle = ovaryStatus?.takeIf { it.isNotBlank() } ?: findings.orEmpty(),
    )

internal fun Gestation.toTimelineEntry(patientName: String): TimelineEntry =
    TimelineEntry(
        patientId = patientId,
        patientName = patientName,
        recordType = RecordType.Gestation.displayName,
        recordId = id,
        date = breedingDate,
        title = "Gestation",
        subtitle = status,
    )

internal fun ReproMedication.toTimelineEntry(patientName: String): TimelineEntry =
    TimelineEntry(
        patientId = patientId,
        patientName = patientName,
        recordType = RecordType.ReproMedication.displayName,
        recordId = id,
        date = dateAdministered,
        title = "Repro Medication",
        subtitle = medication,
    )
