package com.github.rodrigotimoteo.animally.presentation.weight

import kotlin.time.Instant

/**
 * UI state for the weight add/edit form.
 *
 * @param id The persisted weight id; `null` when creating a new one.
 * @param weightKg The measured weight in kilograms as a display string.
 * @param date The measurement date as a display string (ISO `yyyy-MM-dd`).
 * @param notes Optional free-form notes.
 * @param weightError Validation message for the weight field, or `null` when valid.
 * @param dateError Validation message for the date field, or `null` when valid.
 * @param createdAt The creation timestamp of the record being edited, or `null` when new.
 * @param isLoading Whether the form is still loading an existing weight entry.
 * @param isSaving Whether a save is currently in progress.
 */
data class WeightFormState(
    val id: Long? = null,
    val weightKg: String = "",
    val date: String = "",
    val notes: String? = null,
    val weightError: String? = null,
    val dateError: String? = null,
    val createdAt: Instant? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
) {
    val isEditing: Boolean get() = id != null
}
