package com.github.rodrigotimoteo.animally.presentation.customreminder

import kotlin.time.Instant

/**
 * UI state for the custom reminder add/edit form.
 *
 * @param id The persisted custom reminder id; `null` when creating a new one.
 * @param title The reminder title.
 * @param dueDate The due date as a display string (ISO `yyyy-MM-dd`).
 * @param linkedRecordType Optional kind of linked record.
 * @param linkedRecordId Optional id of the linked record as display text.
 * @param notes Optional free-form notes.
 * @param titleError Validation message for the title field, or `null` when valid.
 * @param dueDateError Validation message for the due date field, or `null` when valid.
 * @param linkedRecordIdError Validation message for the linked record id field, or `null` when valid.
 * @param createdAt Timestamp the existing reminder was created; `null` for new reminders.
 * @param isLoading Whether the form is still loading an existing custom reminder.
 * @param isSaving Whether a save is currently in progress.
 */
data class CustomReminderFormState(
    val id: Long? = null,
    val title: String = "",
    val dueDate: String = "",
    val linkedRecordType: String? = null,
    val linkedRecordId: String? = null,
    val notes: String? = null,
    val titleError: String? = null,
    val dueDateError: String? = null,
    val linkedRecordIdError: String? = null,
    val createdAt: Instant? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
) {
    val isEditing: Boolean get() = id != null
}
