@file:OptIn(ExperimentalObjCName::class)

package com.github.rodrigotimoteo.animally.presentation.ios

import com.github.rodrigotimoteo.animally.bridge.NativeFlow
import com.github.rodrigotimoteo.animally.presentation.ios.base.GenericEditStore
import com.github.rodrigotimoteo.animally.presentation.vaccination.VaccinationEditViewModel
import com.github.rodrigotimoteo.animally.presentation.vaccination.VaccinationFormState
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
 * Delegates NativeFlow binding and save/dismiss to [GenericEditStore].
 */
@ObjCName("VaccinationEditStore")
class VaccinationEditStore(
    viewModel: VaccinationEditViewModel,
) : GenericEditStore<VaccinationEditViewModel, VaccinationFormState, VaccinationEditStoreState>(viewModel) {
    /** Observable form state of the vaccination add/edit screen. */
    override val state: NativeFlow<VaccinationEditStoreState> =
        viewModel.formState.toStoreStateFlow(VaccinationEditStoreState()) { VaccinationEditStoreState(form = it) }

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
}
