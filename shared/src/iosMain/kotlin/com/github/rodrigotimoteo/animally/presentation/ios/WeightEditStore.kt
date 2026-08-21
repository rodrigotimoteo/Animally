@file:OptIn(ExperimentalObjCName::class)

package com.github.rodrigotimoteo.animally.presentation.ios

import androidx.lifecycle.viewModelScope
import com.github.rodrigotimoteo.animally.bridge.NativeFlow
import com.github.rodrigotimoteo.animally.presentation.weight.WeightEditViewModel
import com.github.rodrigotimoteo.animally.presentation.weight.WeightFormState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
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
 */
@ObjCName("WeightEditStore")
class WeightEditStore(
    private val viewModel: WeightEditViewModel,
) {
    /** Observable form state of the weight add/edit screen. */
    val state: NativeFlow<WeightEditStoreState> =
        NativeFlow(
            viewModel.formState
                .map { WeightEditStoreState(form = it) }
                .stateIn(
                    scope = viewModel.viewModelScope,
                    started = SharingStarted.Eagerly,
                    initialValue = WeightEditStoreState(form = null),
                ),
            viewModel.viewModelScope,
        )

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

    /** Validates and persists the current form. */
    fun save() {
        viewModel.save()
    }

    /** Dismisses the error surfaced by the form, if any. */
    fun dismissError() {
        viewModel.onDismissError()
    }
}
