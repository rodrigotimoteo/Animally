package com.github.rodrigotimoteo.animally.presentation.substance

import kotlin.time.Instant

/**
 * UI state for the controlled-substance add/edit form.
 *
 * @param id The persisted controlled-substance id; `null` when creating a new one.
 * @param drugName The name of the controlled-substance drug.
 * @param dose The dose administered.
 * @param unit Optional dose unit.
 * @param route Optional route of administration.
 * @param administeredBy Optional name of the veterinarian who administered the drug.
 * @param witness Optional name of the witness.
 * @param date The administration date as a display string (ISO `yyyy-MM-dd`).
 * @param reason Optional reason for administration.
 * @param notes Optional notes.
 * @param drugNameError Validation message for the drug-name field, or `null` when valid.
 * @param doseError Validation message for the dose field, or `null` when valid.
 * @param dateError Validation message for the date field, or `null` when valid.
 * @param isLoading Whether the form is still loading an existing record.
 * @param isSaving Whether a save is currently in progress.
 */
data class ControlledSubstanceFormState(
    val id: Long? = null,
    val drugName: String = "",
    val dose: String = "",
    val unit: String? = null,
    val route: String? = null,
    val administeredBy: String? = null,
    val witness: String? = null,
    val date: String = "",
    val reason: String? = null,
    val notes: String? = null,
    val drugNameError: String? = null,
    val doseError: String? = null,
    val dateError: String? = null,
    val createdAt: Instant? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
) {
    val isEditing: Boolean get() = id != null
}
