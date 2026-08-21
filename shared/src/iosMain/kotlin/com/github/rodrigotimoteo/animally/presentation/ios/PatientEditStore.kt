@file:OptIn(ExperimentalObjCName::class)

package com.github.rodrigotimoteo.animally.presentation.ios

import androidx.lifecycle.viewModelScope
import com.github.rodrigotimoteo.animally.bridge.NativeFlow
import com.github.rodrigotimoteo.animally.domain.owner.model.Owner
import com.github.rodrigotimoteo.animally.presentation.patientEdit.CogginsField
import com.github.rodrigotimoteo.animally.presentation.patientEdit.PatientEditViewModel
import com.github.rodrigotimoteo.animally.presentation.patientEdit.PatientFormState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlin.experimental.ExperimentalObjCName
import kotlin.native.ObjCName

/**
 * Swift-facing state of the patient add/edit form.
 *
 * Wraps the view model's nullable [PatientFormState] together with the
 * owners available for the owner picker, so the store exposes a non-null
 * [NativeFlow] value.
 *
 * @property form The current form state, or `null` before the form has loaded.
 * @property owners The owners available for the owner picker.
 */
@ObjCName("PatientEditStoreState")
data class PatientEditStoreState(
    val form: PatientFormState? = null,
    val owners: List<Owner> = emptyList(),
)

/**
 * Swift-facing store wrapping [PatientEditViewModel].
 *
 * Exposes only data actions; navigation is owned by SwiftUI. The function
 * count is inherently high — one onChange delegate per [PatientFormState]
 * field keeps the Swift surface flat and self-documenting; splitting it
 * would fragment that API.
 */
@Suppress("TooManyFunctions")
@ObjCName("PatientEditStore")
class PatientEditStore(
    private val viewModel: PatientEditViewModel,
) {
    /** Observable form state of the patient add/edit screen. */
    val state: NativeFlow<PatientEditStoreState> =
        NativeFlow(
            combine(viewModel.formState, viewModel.owners) { form, owners ->
                PatientEditStoreState(form = form, owners = owners)
            }.stateIn(
                scope = viewModel.viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = PatientEditStoreState(form = null),
            ),
            viewModel.viewModelScope,
        )

    /** Updates the patient's name. */
    fun onNameChange(name: String) {
        viewModel.onNameChange(name)
    }

    /** Updates the patient's species. */
    fun onSpeciesChange(species: String) {
        viewModel.onSpeciesChange(species)
    }

    /** Updates the patient's breed. */
    fun onBreedChange(breed: String) {
        viewModel.onBreedChange(breed)
    }

    /** Updates the patient's date of birth. */
    fun onDateOfBirthChange(dateOfBirth: String) {
        viewModel.onDateOfBirthChange(dateOfBirth)
    }

    /** Updates the patient's gender. */
    fun onGenderChange(gender: String) {
        viewModel.onGenderChange(gender)
    }

    /** Updates the patient's microchip identifier. */
    fun onMicrochipIdChange(microchipId: String) {
        viewModel.onMicrochipIdChange(microchipId)
    }

    /** Updates the patient's Unique Equine Life Number. */
    fun onUelnChange(ueln: String) {
        viewModel.onUelnChange(ueln)
    }

    /** Updates the patient's studbook or federation registration number. */
    fun onRegistrationNumberChange(registrationNumber: String) {
        viewModel.onRegistrationNumberChange(registrationNumber)
    }

    /** Updates the patient's stable location. */
    fun onStableLocationChange(stableLocation: String) {
        viewModel.onStableLocationChange(stableLocation)
    }

    /** Updates the patient's photo URI. */
    fun onPhotoUriChange(photoUri: String) {
        viewModel.onPhotoUriChange(photoUri)
    }

    /** Updates the patient's free-form notes. */
    fun onNotesChange(notes: String) {
        viewModel.onNotesChange(notes)
    }

    /**
     * Updates the Coggins field identified by [field].
     */
    fun onCogginsChange(
        field: CogginsField,
        value: String,
    ) {
        viewModel.onCogginsChange(field, value)
    }

    /** Updates the id of the linked owner. */
    fun onOwnerChange(ownerId: Long?) {
        viewModel.onOwnerChange(ownerId)
    }

    /** Validates and persists the current form. */
    fun save() {
        viewModel.save()
    }

    /** Dismisses the error surfaced by the form, if any. */
    fun dismissError() {
        viewModel.onDismissError()
    }
}
