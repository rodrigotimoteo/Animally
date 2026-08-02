package com.github.rodrigotimoteo.animally.presentation.labresult

import kotlin.time.Instant

/**
 * UI state for the lab result add/edit form.
 *
 * @param id The persisted lab result id; `null` when creating a new one.
 * @param testType The type of laboratory test performed.
 * @param date The test date as a display string (ISO `yyyy-MM-dd`).
 * @param results Optional test result values.
 * @param normalRange Optional reference range for the test.
 * @param vetName Optional name of the attending veterinarian.
 * @param notes Optional free-text notes.
 * @param testTypeError Validation message for the test type field, or `null` when valid.
 * @param dateError Validation message for the date field, or `null` when valid.
 * @param createdAt The creation timestamp of the record being edited, or `null` when new.
 * @param isLoading Whether the form is still loading an existing lab result.
 * @param isSaving Whether a save is currently in progress.
 */
data class LabResultFormState(
    val id: Long? = null,
    val testType: String = "",
    val date: String = "",
    val results: String? = null,
    val normalRange: String? = null,
    val vetName: String? = null,
    val notes: String? = null,
    val testTypeError: String? = null,
    val dateError: String? = null,
    val createdAt: Instant? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
) {
    val isEditing: Boolean get() = id != null
}
