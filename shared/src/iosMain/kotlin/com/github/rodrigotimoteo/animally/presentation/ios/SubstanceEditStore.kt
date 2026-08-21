@file:OptIn(ExperimentalObjCName::class)

package com.github.rodrigotimoteo.animally.presentation.ios

import androidx.lifecycle.viewModelScope
import com.github.rodrigotimoteo.animally.bridge.NativeFlow
import com.github.rodrigotimoteo.animally.presentation.substance.ControlledSubstanceEditViewModel
import com.github.rodrigotimoteo.animally.presentation.substance.ControlledSubstanceFormState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlin.experimental.ExperimentalObjCName
import kotlin.native.ObjCName

/**
 * Swift-facing state of the controlled-substance add/edit form.
 *
 * Wraps the view model's nullable [ControlledSubstanceFormState], so the store
 * exposes a non-null [NativeFlow] value.
 *
 * @property form The current form state, or `null` before the form has loaded.
 */
@ObjCName("SubstanceEditStoreState")
data class SubstanceEditStoreState(
    val form: ControlledSubstanceFormState? = null,
)

/**
 * Swift-facing store wrapping [ControlledSubstanceEditViewModel].
 *
 * Exposes only data actions; navigation is owned by SwiftUI.
 */
@ObjCName("SubstanceEditStore")
class SubstanceEditStore(
    private val viewModel: ControlledSubstanceEditViewModel,
) {
    /** Observable form state of the controlled-substance add/edit screen. */
    val state: NativeFlow<SubstanceEditStoreState> =
        NativeFlow(
            viewModel.formState
                .map { SubstanceEditStoreState(form = it) }
                .stateIn(
                    scope = viewModel.viewModelScope,
                    started = SharingStarted.Eagerly,
                    initialValue = SubstanceEditStoreState(form = null),
                ),
            viewModel.viewModelScope,
        )

    /** Updates the drug name. */
    fun onDrugNameChange(value: String) {
        viewModel.onDrugNameChange(value)
    }

    /** Updates the administered dose. */
    fun onDoseChange(value: String) {
        viewModel.onDoseChange(value)
    }

    /** Updates the dose unit. */
    fun onUnitChange(value: String) {
        viewModel.onUnitChange(value)
    }

    /** Updates the administration route. */
    fun onRouteChange(value: String) {
        viewModel.onRouteChange(value)
    }

    /** Updates the name of the administering veterinarian. */
    fun onAdministeredByChange(value: String) {
        viewModel.onAdministeredByChange(value)
    }

    /** Updates the witness of the administration. */
    fun onWitnessChange(value: String) {
        viewModel.onWitnessChange(value)
    }

    /** Updates the administration date. */
    fun onDateChange(value: String) {
        viewModel.onDateChange(value)
    }

    /** Updates the reason for administration. */
    fun onReasonChange(value: String) {
        viewModel.onReasonChange(value)
    }

    /** Updates the free-form notes. */
    fun onNotesChange(value: String) {
        viewModel.onNotesChange(value)
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
