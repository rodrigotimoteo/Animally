@file:OptIn(ExperimentalObjCName::class)

package com.github.rodrigotimoteo.animally.presentation.ios

import androidx.lifecycle.viewModelScope
import com.github.rodrigotimoteo.animally.bridge.NativeFlow
import com.github.rodrigotimoteo.animally.presentation.vaccination.VaccinationEditViewModel
import com.github.rodrigotimoteo.animally.presentation.vaccination.VaccinationFormState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlin.experimental.ExperimentalObjCName
import kotlin.native.ObjCName

/**
 * Swift-facing state of the vaccination add/edit form.
 *
 * Wraps the view model's nullable [VaccinationFormState], so the store exposes
 * a non-null [NativeFlow] value.
 *
 * @property form The current form state, or `null` before the form has loaded.
 */
@ObjCName("VaccinationEditStoreState")
data class VaccinationEditStoreState(
    val form: VaccinationFormState? = null,
)

/**
 * Swift-facing store wrapping [VaccinationEditViewModel].
 *
 * Exposes only data actions; navigation is owned by SwiftUI.
 */
@ObjCName("VaccinationEditStore")
class VaccinationEditStore(
    private val viewModel: VaccinationEditViewModel,
) {
    /** Observable form state of the vaccination add/edit screen. */
    val state: NativeFlow<VaccinationEditStoreState> =
        NativeFlow(
            viewModel.formState
                .map { VaccinationEditStoreState(form = it) }
                .stateIn(
                    scope = viewModel.viewModelScope,
                    started = SharingStarted.Eagerly,
                    initialValue = VaccinationEditStoreState(form = null),
                ),
            viewModel.viewModelScope,
        )

    /** Updates the name of the administered vaccine. */
    fun onVaccineNameChange(vaccineName: String) {
        viewModel.onVaccineNameChange(vaccineName)
    }

    /** Updates the administration date. */
    fun onDateAdministeredChange(dateAdministered: String) {
        viewModel.onDateAdministeredChange(dateAdministered)
    }

    /** Updates the name of the attending veterinarian. */
    fun onVetNameChange(vetName: String) {
        viewModel.onVetNameChange(vetName)
    }

    /** Updates the batch number of the vaccine. */
    fun onBatchNumberChange(batchNumber: String) {
        viewModel.onBatchNumberChange(batchNumber)
    }

    /** Updates the administration site. */
    fun onSiteChange(site: String) {
        viewModel.onSiteChange(site)
    }

    /** Updates the free-form notes. */
    fun onNotesChange(notes: String) {
        viewModel.onNotesChange(notes)
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
