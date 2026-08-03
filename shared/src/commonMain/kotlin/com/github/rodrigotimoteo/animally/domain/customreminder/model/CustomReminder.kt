package com.github.rodrigotimoteo.animally.domain.customreminder.model

import kotlinx.datetime.LocalDate
import kotlin.time.Instant

/**
 * A user-created reminder not tied to any calculated due date.
 *
 * Distinct from the derived [com.github.rodrigotimoteo.animally.domain.reminder.model.Reminder]
 * model: custom reminders are persisted records the user creates directly.
 *
 * @property id Unique identifier for the reminder.
 * @property patientId Identifier of the patient the reminder belongs to.
 * @property title Short description of the reminder.
 * @property dueDate The date the reminder is due.
 * @property linkedRecordType Optional kind of linked record, e.g. "Vaccination".
 * @property linkedRecordId Optional id of the linked record.
 * @property notes Optional free-form notes.
 * @property isActive Indicates whether the reminder is active. Defaults to `true`.
 * @property createdAt Timestamp when the reminder was created.
 * @property updatedAt Timestamp when the reminder was last modified.
 */
data class CustomReminder(
    val id: Long,
    val patientId: Long,
    val title: String,
    val dueDate: LocalDate,
    val linkedRecordType: String? = null,
    val linkedRecordId: Long? = null,
    val notes: String? = null,
    val isActive: Boolean = true,
    val createdAt: Instant,
    val updatedAt: Instant,
    val serverId: String? = null,
)
