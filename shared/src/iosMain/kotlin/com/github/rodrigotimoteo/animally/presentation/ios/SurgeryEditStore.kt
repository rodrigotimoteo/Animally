@file:OptIn(ExperimentalObjCName::class)

package com.github.rodrigotimoteo.animally.presentation.ios

import androidx.lifecycle.viewModelScope
import com.github.rodrigotimoteo.animally.bridge.NativeFlow
import com.github.rodrigotimoteo.animally.presentation.surgery.SurgeryEditViewModel
import com.github.rodrigotimoteo.animally.presentation.surgery.SurgeryFormState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlin.experimental.ExperimentalObjCName
import kotlin.native.ObjCName

/**
 * Swift-facing state of the surgery add/edit form.
 *
 * Wraps the view model's nullable [SurgeryFormState], so the store exposes a
 * non-null [NativeFlow] value.
 *
 * @property form The current form state, or `null` before the form has loaded.
 */
@ObjCName("SurgeryEditStoreState")
data class SurgeryEditStoreState(
    val form: SurgeryFormState? = null,
)

/**
 * Swift-facing store wrapping [SurgeryEditViewModel].
 *
 * Exposes only data actions; navigation is owned by SwiftUI.
 */
@ObjCName("SurgeryEditStore")
class SurgeryEditStore(
    private val viewModel: SurgeryEditViewModel,
) {
    /** Observable form state of the surgery add/edit screen. */
    val state: NativeFlow<SurgeryEditStoreState> =
        NativeFlow(
            viewModel.formState
                .map { SurgeryEditStoreState(form = it) }
                .stateIn(
                    scope = viewModel.viewModelScope,
                    started = SharingStarted.Eagerly,
                    initialValue = SurgeryEditStoreState(form = null),
                ),
            viewModel.viewModelScope,
        )

    /** Updates the surgery date. */
    fun onDateChange(date: String) {
        viewModel.onDateChange(date)
    }

    /** Updates the surgery type. */
    fun onTypeChange(value: String) {
        viewModel.onTypeChange(value)
    }

    /** Updates the description. */
    fun onDescriptionChange(value: String) {
        viewModel.onDescriptionChange(value)
    }

    /** Updates the outcome. */
    fun onOutcomeChange(value: String) {
        viewModel.onOutcomeChange(value)
    }

    /** Updates the name of the surgeon. */
    fun onSurgeonChange(value: String) {
        viewModel.onSurgeonChange(value)
    }

    /** Updates the anesthesia used. */
    fun onAnesthesiaChange(value: String) {
        viewModel.onAnesthesiaChange(value)
    }

    /** Updates the analgesia used. */
    fun onAnalgesiaChange(value: String) {
        viewModel.onAnalgesiaChange(value)
    }

    /** Updates the complications observed. */
    fun onComplicationsChange(value: String) {
        viewModel.onComplicationsChange(value)
    }

    /** Updates the recovery notes. */
    fun onRecoveryNotesChange(value: String) {
        viewModel.onRecoveryNotesChange(value)
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
