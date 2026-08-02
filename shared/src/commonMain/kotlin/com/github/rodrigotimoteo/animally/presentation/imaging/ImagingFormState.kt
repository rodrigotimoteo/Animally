package com.github.rodrigotimoteo.animally.presentation.imaging

import kotlin.time.Instant

/**
 * UI state for the imaging add/edit form.
 *
 * @param id The persisted imaging id; `null` when creating a new one.
 * @param type The type of imaging study performed.
 * @param date The imaging date as a display string (ISO `yyyy-MM-dd`).
 * @param findings Optional interpretation of the imaging.
 * @param imageUris Optional comma-separated list of image file paths.
 * @param vetName Optional name of the attending veterinarian.
 * @param notes Optional free-text notes.
 * @param typeError Validation message for the type field, or `null` when valid.
 * @param dateError Validation message for the date field, or `null` when valid.
 * @param createdAt The creation timestamp of the record being edited, or `null` when new.
 * @param isLoading Whether the form is still loading an existing imaging record.
 * @param isSaving Whether a save is currently in progress.
 */
data class ImagingFormState(
    val id: Long? = null,
    val type: String = "",
    val date: String = "",
    val findings: String? = null,
    val imageUris: String? = null,
    val vetName: String? = null,
    val notes: String? = null,
    val typeError: String? = null,
    val dateError: String? = null,
    val createdAt: Instant? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
) {
    val isEditing: Boolean get() = id != null
}
