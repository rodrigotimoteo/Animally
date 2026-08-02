package com.github.rodrigotimoteo.animally.presentation.patientEdit

import kotlin.time.Instant

/**
 * UI state for the patient add/edit form.
 *
 * @param id The persisted patient id; `null` when creating a new patient.
 * @param name The patient's name.
 * @param species The patient's species. Defaults to `Equine`.
 * @param breed Optional breed.
 * @param dateOfBirth Optional date of birth as a display string (ISO `yyyy-MM-dd`).
 * @param gender Optional gender.
 * @param microchipId Optional microchip identifier.
 * @param ueln Optional Unique Equine Life Number.
 * @param registrationNumber Optional studbook or federation registration number.
 * @param stableLocation Optional stable location.
 * @param photoUri Optional photo URI.
 * @param notes Optional free-form notes.
 * @param ownerId Optional id of the linked owner.
 * @param createdAt The original creation timestamp, preserved when editing.
 * @param nameError Validation message for the name field, or `null` when valid.
 * @param uelnError Validation message for the UELN field, or `null` when valid.
 * @param isLoading Whether the form is still loading an existing patient.
 * @param isSaving Whether a save is currently in progress.
 */
data class PatientFormState(
    val id: Long? = null,
    val name: String = "",
    val species: String = "Equine",
    val breed: String? = null,
    val dateOfBirth: String? = null,
    val gender: String? = null,
    val microchipId: String? = null,
    val ueln: String? = null,
    val registrationNumber: String? = null,
    val stableLocation: String? = null,
    val photoUri: String? = null,
    val notes: String? = null,
    val ownerId: Long? = null,
    val createdAt: Instant? = null,
    val nameError: String? = null,
    val uelnError: String? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
) {
    val isEditing: Boolean get() = id != null
}
