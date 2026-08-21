@file:OptIn(ExperimentalObjCName::class)

package com.github.rodrigotimoteo.animally.presentation.ios

import androidx.lifecycle.viewModelScope
import com.github.rodrigotimoteo.animally.bridge.NativeFlow
import com.github.rodrigotimoteo.animally.presentation.repromedication.ReproMedicationEditViewModel
import com.github.rodrigotimoteo.animally.presentation.repromedication.ReproMedicationFormState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlin.experimental.ExperimentalObjCName
import kotlin.native.ObjCName

/**
 * Swift-facing state of the reproduction-medication add/edit form.
 *
 * Wraps the view model's nullable [ReproMedicationFormState], so the store
 * exposes a non-null [NativeFlow] value.
 *
 * @property form The current form state, or `null` before the form has loaded.
 */
@ObjCName("ReproMedicationEditStoreState")
data class ReproMedicationEditStoreState(
    val form: ReproMedicationFormState? = null,
)

/**
 * Swift-facing store wrapping [ReproMedicationEditViewModel].
 *
 * Exposes only data actions; navigation is owned by SwiftUI.
 */
@ObjCName("ReproMedicationEditStore")
class ReproMedicationEditStore(
    private val viewModel: ReproMedicationEditViewModel,
) {
    /** Observable form state of the reproduction-medication add/edit screen. */
    val state: NativeFlow<ReproMedicationEditStoreState> =
        NativeFlow(
            viewModel.formState
                .map { ReproMedicationEditStoreState(form = it) }
                .stateIn(
                    scope = viewModel.viewModelScope,
                    started = SharingStarted.Eagerly,
                    initialValue = ReproMedicationEditStoreState(form = null),
                ),
            viewModel.viewModelScope,
        )

    /** Updates the medication name. */
    fun onMedicationChange(value: String) {
        viewModel.onMedicationChange(value)
    }

    /** Updates the administration date. */
    fun onDateAdministeredChange(value: String) {
        viewModel.onDateAdministeredChange(value)
    }

    /** Updates the dosage. */
    fun onDosageChange(value: String) {
        viewModel.onDosageChange(value)
    }

    /** Updates the purpose of the treatment. */
    fun onPurposeChange(value: String) {
        viewModel.onPurposeChange(value)
    }

    /** Updates the name of the attending veterinarian. */
    fun onVetNameChange(value: String) {
        viewModel.onVetNameChange(value)
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
