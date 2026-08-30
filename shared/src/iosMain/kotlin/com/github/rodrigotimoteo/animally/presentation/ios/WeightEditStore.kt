@file:OptIn(ExperimentalObjCName::class)

package com.github.rodrigotimoteo.animally.presentation.ios

import com.github.rodrigotimoteo.animally.bridge.NativeFlow
import com.github.rodrigotimoteo.animally.presentation.ios.base.GenericEditStore
import com.github.rodrigotimoteo.animally.presentation.weight.WeightEditViewModel
import com.github.rodrigotimoteo.animally.presentation.weight.WeightFormState
import kotlin.experimental.ExperimentalObjCName
import kotlin.native.ObjCName

/**
 * Swift-facing state of the weight add/edit form.
 *
 * Wraps the view model's nullable [WeightFormState], so the store exposes a
 * non-null [NativeFlow] value.
 *
 * @property form The current form state, or `null` before the form has loaded.
 */
@ObjCName("WeightEditStoreState")
data class WeightEditStoreState(
    val form: WeightFormState? = null,
)

/**
 * Swift-facing store wrapping [WeightEditViewModel].
 *
 * Exposes only data actions; navigation is owned by SwiftUI.
 * Delegates NativeFlow binding and save/dismiss to [GenericEditStore].
 */
@ObjCName("WeightEditStore")
class WeightEditStore(
    viewModel: WeightEditViewModel,
) : GenericEditStore<WeightEditViewModel, WeightFormState, WeightEditStoreState>(viewModel) {
    /** Observable form state of the weight add/edit screen. */
    override val state: NativeFlow<WeightEditStoreState> =
        viewModel.formState.toStoreStateFlow(WeightEditStoreState()) { WeightEditStoreState(form = it) }

    /** Updates the measured weight in kilograms. */
    fun onWeightKgChange(weightKg: String) {
        viewModel.onWeightKgChange(weightKg)
    }

    /** Updates the measurement date. */
    fun onDateChange(date: String) {
        viewModel.onDateChange(date)
    }

    /** Updates the free-form notes. */
    fun onNotesChange(notes: String) {
        viewModel.onNotesChange(notes)
    }
}
