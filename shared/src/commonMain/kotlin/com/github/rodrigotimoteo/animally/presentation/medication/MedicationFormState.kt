package com.github.rodrigotimoteo.animally.presentation.medication

import kotlin.time.Instant

/**
 * UI state for the medication add/edit form.
 *
 * @param id The persisted medication id; `null` when creating a new one.
 * @param name The name of the medication.
 * @param dosage The dosage of the medication.
 * @param route Optional route of administration.
 * @param frequency Optional administration frequency.
 * @param startDate Optional start date as a display string (ISO `yyyy-MM-dd`).
 * @param endDate Optional end date as a display string (ISO `yyyy-MM-dd`).
 * @param prescribedBy Optional name of the prescribing veterinarian.
 * @param notes Optional notes.
 * @param nameError Validation message for the name field, or `null` when valid.
 * @param dosageError Validation message for the dosage field, or `null` when valid.
 * @param startDateError Validation message for the start-date field, or `null` when valid.
 * @param endDateError Validation message for the end-date field, or `null` when valid.
 * @param isLoading Whether the form is still loading an existing medication.
 * @param isSaving Whether a save is currently in progress.
 */
data class MedicationFormState(
    val id: Long? = null,
    val name: String = "",
    val dosage: String = "",
    val route: String? = null,
    val frequency: String? = null,
    val startDate: String? = null,
    val endDate: String? = null,
    val prescribedBy: String? = null,
    val notes: String? = null,
    val nameError: String? = null,
    val dosageError: String? = null,
    val startDateError: String? = null,
    val endDateError: String? = null,
    val createdAt: Instant? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
) {
    val isEditing: Boolean get() = id != null
}
