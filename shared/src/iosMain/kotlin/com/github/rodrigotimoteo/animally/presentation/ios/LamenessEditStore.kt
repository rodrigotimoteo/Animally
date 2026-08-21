@file:OptIn(ExperimentalObjCName::class)

package com.github.rodrigotimoteo.animally.presentation.ios

import androidx.lifecycle.viewModelScope
import com.github.rodrigotimoteo.animally.bridge.NativeFlow
import com.github.rodrigotimoteo.animally.presentation.lameness.LamenessEditViewModel
import com.github.rodrigotimoteo.animally.presentation.lameness.LamenessFormState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlin.experimental.ExperimentalObjCName
import kotlin.native.ObjCName

/**
 * Swift-facing state of the lameness add/edit form.
 *
 * Wraps the view model's nullable [LamenessFormState], so the store exposes a
 * non-null [NativeFlow] value.
 *
 * @property form The current form state, or `null` before the form has loaded.
 */
@ObjCName("LamenessEditStoreState")
data class LamenessEditStoreState(
    val form: LamenessFormState? = null,
)

/**
 * Swift-facing store wrapping [LamenessEditViewModel].
 *
 * Exposes only data actions; navigation is owned by SwiftUI.
 */
@ObjCName("LamenessEditStore")
class LamenessEditStore(
    private val viewModel: LamenessEditViewModel,
) {
    /** Observable form state of the lameness add/edit screen. */
    val state: NativeFlow<LamenessEditStoreState> =
        NativeFlow(
            viewModel.formState
                .map { LamenessEditStoreState(form = it) }
                .stateIn(
                    scope = viewModel.viewModelScope,
                    started = SharingStarted.Eagerly,
                    initialValue = LamenessEditStoreState(form = null),
                ),
            viewModel.viewModelScope,
        )

    /** Updates the evaluation date. */
    fun onDateChange(date: String) {
        viewModel.onDateChange(date)
    }

    /** Updates the AAEP lameness grade (1-5). */
    fun onGradeAAEPChange(value: String) {
        viewModel.onGradeAAEPChange(value)
    }

    /** Updates the affected limb location. */
    fun onLimbLocationChange(value: String) {
        viewModel.onLimbLocationChange(value)
    }

    /** Updates the flexion test result. */
    fun onFlexionTestChange(value: String) {
        viewModel.onFlexionTestChange(value)
    }

    /** Updates the diagnosis. */
    fun onDiagnosisChange(value: String) {
        viewModel.onDiagnosisChange(value)
    }

    /** Updates the treatment. */
    fun onTreatmentChange(value: String) {
        viewModel.onTreatmentChange(value)
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
