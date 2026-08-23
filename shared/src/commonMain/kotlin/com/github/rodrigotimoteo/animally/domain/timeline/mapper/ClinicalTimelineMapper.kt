package com.github.rodrigotimoteo.animally.domain.timeline.mapper

import com.github.rodrigotimoteo.animally.domain.common.RecordType
import com.github.rodrigotimoteo.animally.domain.dentistry.model.Dentistry
import com.github.rodrigotimoteo.animally.domain.deworming.model.Deworming
import com.github.rodrigotimoteo.animally.domain.imaging.model.Imaging
import com.github.rodrigotimoteo.animally.domain.labresult.model.LabResult
import com.github.rodrigotimoteo.animally.domain.lameness.model.Lameness
import com.github.rodrigotimoteo.animally.domain.medication.model.Medication
import com.github.rodrigotimoteo.animally.domain.surgery.model.Surgery
import com.github.rodrigotimoteo.animally.domain.timeline.model.TimelineEntry
import com.github.rodrigotimoteo.animally.domain.weight.model.Weight

/**
 * Maps clinical record domain models to [TimelineEntry] instances.
 *
 * @param patientName The display name of the owning patient.
 * @return The mapped [TimelineEntry].
 */
internal fun Weight.toTimelineEntry(patientName: String): TimelineEntry =
    TimelineEntry(
        patientId = patientId,
        patientName = patientName,
        recordType = RecordType.Weight.displayName,
        recordId = id,
        date = date,
        title = "Weight",
        subtitle = "$weightKg kg",
    )

internal fun Deworming.toTimelineEntry(patientName: String): TimelineEntry =
    TimelineEntry(
        patientId = patientId,
        patientName = patientName,
        recordType = RecordType.Deworming.displayName,
        recordId = id,
        date = dateAdministered,
        title = "Deworming",
        subtitle = product,
    )

internal fun Dentistry.toTimelineEntry(patientName: String): TimelineEntry =
    TimelineEntry(
        patientId = patientId,
        patientName = patientName,
        recordType = RecordType.Dentistry.displayName,
        recordId = id,
        date = date,
        title = "Dentistry",
        subtitle = treatment?.takeIf { it.isNotBlank() } ?: findings.orEmpty(),
    )

internal fun Lameness.toTimelineEntry(patientName: String): TimelineEntry =
    TimelineEntry(
        patientId = patientId,
        patientName = patientName,
        recordType = RecordType.Lameness.displayName,
        recordId = id,
        date = date,
        title = "Lameness",
        subtitle = "Grade $gradeAAEP",
    )

internal fun Surgery.toTimelineEntry(patientName: String): TimelineEntry =
    TimelineEntry(
        patientId = patientId,
        patientName = patientName,
        recordType = RecordType.Surgery.displayName,
        recordId = id,
        date = date,
        title = "Surgery",
        subtitle = type.orEmpty(),
    )

internal fun Medication.toTimelineEntryOrNull(patientName: String): TimelineEntry? =
    startDate?.let { date ->
        TimelineEntry(
            patientId = patientId,
            patientName = patientName,
            recordType = RecordType.Medication.displayName,
            recordId = id,
            date = date,
            title = "Medication",
            subtitle = name,
        )
    }

internal fun LabResult.toTimelineEntry(patientName: String): TimelineEntry =
    TimelineEntry(
        patientId = patientId,
        patientName = patientName,
        recordType = RecordType.LabResult.displayName,
        recordId = id,
        date = date,
        title = "Lab Result",
        subtitle = testType,
    )

internal fun Imaging.toTimelineEntry(patientName: String): TimelineEntry =
    TimelineEntry(
        patientId = patientId,
        patientName = patientName,
        recordType = RecordType.Imaging.displayName,
        recordId = id,
        date = date,
        title = "Imaging",
        subtitle = type,
    )
