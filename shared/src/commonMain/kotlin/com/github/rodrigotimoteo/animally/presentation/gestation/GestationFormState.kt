package com.github.rodrigotimoteo.animally.presentation.gestation

import kotlin.time.Instant

/**
 * UI state for the gestation add/edit form.
 *
 * @param id The persisted gestation id; `null` when creating a new one.
 * @param breedingDate The breeding date as a display string (ISO `yyyy-MM-dd`).
 * @param status Current status of the pregnancy.
 * @param fetalCount Optional number of fetuses as a display string.
 * @param lastCheckDate Optional date of the last pregnancy check as a display string.
 * @param notes Optional free-form notes.
 * @param breedingDateError Validation message for the breeding-date field, or `null` when valid.
 * @param statusError Validation message for the status field, or `null` when valid.
 * @param fetalCountError Validation message for the fetal-count field, or `null` when valid.
 * @param lastCheckDateError Validation message for the last-check-date field, or `null` when valid.
 * @param createdAt The creation timestamp of the persisted gestation record, when editing.
 * @param isLoading Whether the form is still loading an existing gestation record.
 * @param isSaving Whether a save is currently in progress.
 */
data class GestationFormState(
    val id: Long? = null,
    val breedingDate: String = "",
    val status: String = "",
    val fetalCount: String? = null,
    val lastCheckDate: String? = null,
    val notes: String? = null,
    val breedingDateError: String? = null,
    val statusError: String? = null,
    val fetalCountError: String? = null,
    val lastCheckDateError: String? = null,
    val createdAt: Instant? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
) {
    val isEditing: Boolean get() = id != null
}
