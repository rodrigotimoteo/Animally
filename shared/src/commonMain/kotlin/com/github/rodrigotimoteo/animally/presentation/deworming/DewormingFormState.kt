package com.github.rodrigotimoteo.animally.presentation.deworming

import kotlin.time.Instant

/**
 * UI state for the deworming add/edit form.
 *
 * @param id The persisted deworming id; `null` when creating a new one.
 * @param product The anthelmintic product administered.
 * @param dateAdministered The administration date as a display string (ISO `yyyy-MM-dd`).
 * @param nextDueDate Optional date the next deworming is due.
 * @param dose Optional dosage information.
 * @param vetName Optional name of the attending veterinarian.
 * @param notes Optional free-form notes.
 * @param productError Validation message for the product field, or `null` when valid.
 * @param dateError Validation message for the date field, or `null` when valid.
 * @param nextDueDateError Validation message for the next-due-date field, or `null` when valid.
 * @param createdAt Timestamp the existing record was created; `null` for new records.
 * @param isLoading Whether the form is still loading an existing deworming.
 * @param isSaving Whether a save is currently in progress.
 */
data class DewormingFormState(
    val id: Long? = null,
    val product: String = "",
    val dateAdministered: String = "",
    val nextDueDate: String? = null,
    val dose: String? = null,
    val vetName: String? = null,
    val notes: String? = null,
    val productError: String? = null,
    val dateError: String? = null,
    val nextDueDateError: String? = null,
    val createdAt: Instant? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
) {
    val isEditing: Boolean get() = id != null
}
