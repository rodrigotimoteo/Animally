package com.github.rodrigotimoteo.animally.domain.reminder.model

import kotlinx.datetime.LocalDate

/**
 * A due-date reminder derived from a medical record.
 *
 * @property patientId The patient the record belongs to.
 * @property patientName The patient's display name.
 * @property recordType The kind of record, e.g. "Vaccination" or "Dentistry".
 * @property title The reminder title, e.g. the vaccine name.
 * @property dueDate The date the follow-up is due. May be before today when overdue.
 */
data class Reminder(
    val patientId: Long,
    val patientName: String,
    val recordType: String,
    val title: String,
    val dueDate: LocalDate,
)
