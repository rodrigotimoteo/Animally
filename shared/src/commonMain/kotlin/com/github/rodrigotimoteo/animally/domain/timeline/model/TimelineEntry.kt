package com.github.rodrigotimoteo.animally.domain.timeline.model

import kotlinx.datetime.LocalDate

/**
 * A single item in the timeline feed, representing one record of any type.
 *
 * @property patientId The identifier of the patient owning the record.
 * @property patientName The display name of the patient owning the record.
 * @property recordType Discriminator for the source record type (e.g. "Vaccination").
 * @property recordId The identifier of the source record.
 * @property date The date the record is aggregated under.
 * @property title Short human-readable label of the record type (e.g. "Vaccination").
 * @property subtitle Human-readable summary of the record (e.g. vaccine name).
 */
data class TimelineEntry(
    val patientId: Long,
    val patientName: String,
    val recordType: String,
    val recordId: Long,
    val date: LocalDate,
    val title: String,
    val subtitle: String,
)
