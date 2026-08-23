package com.github.rodrigotimoteo.animally.domain.timeline.mapper

import com.github.rodrigotimoteo.animally.domain.common.RecordType
import com.github.rodrigotimoteo.animally.domain.consultation.model.Consultation
import com.github.rodrigotimoteo.animally.domain.farrier.model.FarrierVisit
import com.github.rodrigotimoteo.animally.domain.substance.model.ControlledSubstance
import com.github.rodrigotimoteo.animally.domain.timeline.model.TimelineEntry
import com.github.rodrigotimoteo.animally.domain.vaccination.model.Vaccination

/**
 * Maps preventive-care record domain models to [TimelineEntry] instances.
 *
 * @param patientName The display name of the owning patient.
 * @return The mapped [TimelineEntry].
 */
internal fun FarrierVisit.toTimelineEntry(patientName: String): TimelineEntry =
    TimelineEntry(
        patientId = patientId,
        patientName = patientName,
        recordType = RecordType.FarrierVisit.displayName,
        recordId = id,
        date = date,
        title = "Farrier",
        subtitle = trimOrShoe.orEmpty(),
    )

internal fun Vaccination.toTimelineEntry(patientName: String): TimelineEntry =
    TimelineEntry(
        patientId = patientId,
        patientName = patientName,
        recordType = RecordType.Vaccination.displayName,
        recordId = id,
        date = dateAdministered,
        title = "Vaccination",
        subtitle = vaccineName,
    )

internal fun ControlledSubstance.toTimelineEntry(patientName: String): TimelineEntry =
    TimelineEntry(
        patientId = patientId,
        patientName = patientName,
        recordType = RecordType.ControlledSubstance.displayName,
        recordId = id,
        date = date,
        title = "Controlled Substance",
        subtitle = drugName,
    )

internal fun Consultation.toTimelineEntry(patientName: String): TimelineEntry =
    TimelineEntry(
        patientId = patientId,
        patientName = patientName,
        recordType = RecordType.Consultation.displayName,
        recordId = id,
        date = date,
        title = "Consultation",
        subtitle = assessment.ifBlank { "SOAP" },
    )
