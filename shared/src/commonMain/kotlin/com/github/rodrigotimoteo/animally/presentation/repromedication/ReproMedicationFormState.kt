
package com.github.rodrigotimoteo.animally.presentation.repromedication

import com.github.rodrigotimoteo.animally.presentation.common.todayIso
import kotlin.time.Instant

/**
 * UI state for the reproduction-medication add/edit form.
 *
 * @param id The persisted reproduction-medication id; `null` when creating a new one.
 * @param medication The name of the administered medication.
 * @param dateAdministered The administration date as a display string (ISO `yyyy-MM-dd`).
 * @param dosage Optional dosage information.
 * @param purpose Optional purpose of the medication.
 * @param vetName Optional name of the attending veterinarian.
 * @param notes Optional free-form notes.
 * @param medicationError Validation message for the medication field, or `null` when valid.
 * @param dateError Validation message for the date field, or `null` when valid.
 * @param createdAt The creation timestamp of the persisted record, when editing.
 * @param isLoading Whether the form is still loading an existing record.
 * @param isSaving Whether a save is currently in progress.
 */
data class ReproMedicationFormState(
    val id: Long? = null,
    val medication: String = "",
    val dateAdministered: String = todayIso(),
    val dosage: String? = null,
    val purpose: String? = null,
    val vetName: String? = null,
    val notes: String? = null,
    val medicationError: String? = null,
    val dateError: String? = null,
    val createdAt: Instant? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
) {
    val isEditing: Boolean get() = id != null
}
