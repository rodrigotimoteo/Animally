@file:OptIn(ExperimentalObjCName::class)

package com.github.rodrigotimoteo.animally.presentation.ios

import androidx.lifecycle.viewModelScope
import com.github.rodrigotimoteo.animally.bridge.NativeFlow
import com.github.rodrigotimoteo.animally.presentation.gestation.GestationEditViewModel
import com.github.rodrigotimoteo.animally.presentation.gestation.GestationFormState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlin.experimental.ExperimentalObjCName
import kotlin.native.ObjCName

/**
 * Swift-facing state of the gestation add/edit form.
 *
 * Wraps the view model's nullable [GestationFormState], so the store exposes a
 * non-null [NativeFlow] value.
 *
 * @property form The current form state, or `null` before the form has loaded.
 */
@ObjCName("GestationEditStoreState")
data class GestationEditStoreState(
    val form: GestationFormState? = null,
)

/**
 * Swift-facing store wrapping [GestationEditViewModel].
 *
 * Exposes only data actions; navigation is owned by SwiftUI.
 */
@ObjCName("GestationEditStore")
class GestationEditStore(
    private val viewModel: GestationEditViewModel,
) {
    /** Observable form state of the gestation add/edit screen. */
    val state: NativeFlow<GestationEditStoreState> =
        NativeFlow(
            viewModel.formState
                .map { GestationEditStoreState(form = it) }
                .stateIn(
                    scope = viewModel.viewModelScope,
                    started = SharingStarted.Eagerly,
                    initialValue = GestationEditStoreState(form = null),
                ),
            viewModel.viewModelScope,
        )

    /** Updates the breeding date. */
    fun onBreedingDateChange(value: String) {
        viewModel.onBreedingDateChange(value)
    }

    /** Updates the gestation status. */
    fun onStatusChange(value: String) {
        viewModel.onStatusChange(value)
    }

    /** Updates the fetal count. */
    fun onFetalCountChange(value: String) {
        viewModel.onFetalCountChange(value)
    }

    /** Updates the last pregnancy-check date. */
    fun onLastCheckDateChange(value: String) {
        viewModel.onLastCheckDateChange(value)
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
