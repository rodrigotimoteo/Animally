@file:OptIn(ExperimentalObjCName::class)

package com.github.rodrigotimoteo.animally.presentation.ios

import androidx.lifecycle.viewModelScope
import com.github.rodrigotimoteo.animally.bridge.NativeFlow
import com.github.rodrigotimoteo.animally.presentation.medication.MedicationEditViewModel
import com.github.rodrigotimoteo.animally.presentation.medication.MedicationFormState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlin.experimental.ExperimentalObjCName
import kotlin.native.ObjCName

/**
 * Swift-facing state of the medication add/edit form.
 *
 * Wraps the view model's nullable [MedicationFormState], so the store exposes a
 * non-null [NativeFlow] value.
 *
 * @property form The current form state, or `null` before the form has loaded.
 */
@ObjCName("MedicationEditStoreState")
data class MedicationEditStoreState(
    val form: MedicationFormState? = null,
)

/**
 * Swift-facing store wrapping [MedicationEditViewModel].
 *
 * Exposes only data actions; navigation is owned by SwiftUI.
 */
@ObjCName("MedicationEditStore")
class MedicationEditStore(
    private val viewModel: MedicationEditViewModel,
) {
    /** Observable form state of the medication add/edit screen. */
    val state: NativeFlow<MedicationEditStoreState> =
        NativeFlow(
            viewModel.formState
                .map { MedicationEditStoreState(form = it) }
                .stateIn(
                    scope = viewModel.viewModelScope,
                    started = SharingStarted.Eagerly,
                    initialValue = MedicationEditStoreState(form = null),
                ),
            viewModel.viewModelScope,
        )

    /** Updates the medication name. */
    fun onNameChange(value: String) {
        viewModel.onNameChange(value)
    }

    /** Updates the dosage. */
    fun onDosageChange(value: String) {
        viewModel.onDosageChange(value)
    }

    /** Updates the administration route. */
    fun onRouteChange(value: String) {
        viewModel.onRouteChange(value)
    }

    /** Updates the frequency. */
    fun onFrequencyChange(value: String) {
        viewModel.onFrequencyChange(value)
    }

    /** Updates the start date. */
    fun onStartDateChange(value: String) {
        viewModel.onStartDateChange(value)
    }

    /** Updates the end date. */
    fun onEndDateChange(value: String) {
        viewModel.onEndDateChange(value)
    }

    /** Updates the name of the prescribing veterinarian. */
    fun onPrescribedByChange(value: String) {
        viewModel.onPrescribedByChange(value)
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
