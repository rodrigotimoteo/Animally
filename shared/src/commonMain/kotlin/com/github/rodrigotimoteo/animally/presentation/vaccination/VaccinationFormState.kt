package com.github.rodrigotimoteo.animally.presentation.vaccination

import kotlin.time.Instant

/**
 * UI state for the vaccination add/edit form.
 *
 * @param id The persisted vaccination id; `null` when creating a new one.
 * @param vaccineName Name of the administered vaccine.
 * @param dateAdministered Date the vaccine was administered (ISO `yyyy-MM-dd`).
 * @param vetName Optional name of the attending veterinarian.
 * @param batchNumber Optional batch number of the vaccine.
 * @param site Optional administration site.
 * @param notes Optional free-form notes.
 * @param nextDueDate Optional preview of the next due date computed on save.
 * @param vaccineNameError Validation message for the vaccine name field, or `null` when valid.
 * @param dateError Validation message for the date field, or `null` when valid.
 * @param isLoading Whether the form is still loading an existing vaccination.
 * @param isSaving Whether a save is currently in progress.
 */
data class VaccinationFormState(
    val id: Long? = null,
    val vaccineName: String = "",
    val dateAdministered: String = "",
    val vetName: String? = null,
    val batchNumber: String? = null,
    val site: String? = null,
    val notes: String? = null,
    val nextDueDate: String? = null,
    val vaccineNameError: String? = null,
    val dateError: String? = null,
    val createdAt: Instant? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
) {
    val isEditing: Boolean get() = id != null
}
