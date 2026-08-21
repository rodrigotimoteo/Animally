@file:OptIn(ExperimentalObjCName::class)

package com.github.rodrigotimoteo.animally.presentation.ios

import androidx.lifecycle.viewModelScope
import com.github.rodrigotimoteo.animally.bridge.NativeFlow
import com.github.rodrigotimoteo.animally.presentation.labresult.LabResultEditViewModel
import com.github.rodrigotimoteo.animally.presentation.labresult.LabResultFormState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlin.experimental.ExperimentalObjCName
import kotlin.native.ObjCName

/**
 * Swift-facing state of the lab result add/edit form.
 *
 * Wraps the view model's nullable [LabResultFormState], so the store exposes a
 * non-null [NativeFlow] value.
 *
 * @property form The current form state, or `null` before the form has loaded.
 */
@ObjCName("LabResultEditStoreState")
data class LabResultEditStoreState(
    val form: LabResultFormState? = null,
)

/**
 * Swift-facing store wrapping [LabResultEditViewModel].
 *
 * Exposes only data actions; navigation is owned by SwiftUI.
 */
@ObjCName("LabResultEditStore")
class LabResultEditStore(
    private val viewModel: LabResultEditViewModel,
) {
    /** Observable form state of the lab result add/edit screen. */
    val state: NativeFlow<LabResultEditStoreState> =
        NativeFlow(
            viewModel.formState
                .map { LabResultEditStoreState(form = it) }
                .stateIn(
                    scope = viewModel.viewModelScope,
                    started = SharingStarted.Eagerly,
                    initialValue = LabResultEditStoreState(form = null),
                ),
            viewModel.viewModelScope,
        )

    /** Updates the test type. */
    fun onTestTypeChange(value: String) {
        viewModel.onTestTypeChange(value)
    }

    /** Updates the test date. */
    fun onDateChange(value: String) {
        viewModel.onDateChange(value)
    }

    /** Updates the results. */
    fun onResultsChange(value: String) {
        viewModel.onResultsChange(value)
    }

    /** Updates the normal reference range. */
    fun onNormalRangeChange(value: String) {
        viewModel.onNormalRangeChange(value)
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
