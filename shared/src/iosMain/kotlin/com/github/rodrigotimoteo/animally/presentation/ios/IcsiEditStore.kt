@file:OptIn(ExperimentalObjCName::class)

package com.github.rodrigotimoteo.animally.presentation.ios

import androidx.lifecycle.viewModelScope
import com.github.rodrigotimoteo.animally.bridge.NativeFlow
import com.github.rodrigotimoteo.animally.presentation.icsi.IcsiEditViewModel
import com.github.rodrigotimoteo.animally.presentation.icsi.IcsiFormState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlin.experimental.ExperimentalObjCName
import kotlin.native.ObjCName

/**
 * Swift-facing state of the ICSI add/edit form.
 *
 * Wraps the view model's nullable [IcsiFormState], so the store exposes a
 * non-null [NativeFlow] value.
 */
data class IcsiEditStoreState(
    val form: IcsiFormState?,
)

/**
 * Swift-facing store wrapping [IcsiEditViewModel].
 *
 * Exposes only data actions; navigation is owned by SwiftUI.
 */
@ObjCName("IcsiEditStore")
class IcsiEditStore(
    private val viewModel: IcsiEditViewModel,
) {
    /** Observable form state of the ICSI add/edit screen. */
    val state: NativeFlow<IcsiEditStoreState> =
        NativeFlow(
            viewModel.formState
                .map { IcsiEditStoreState(form = it) }
                .stateIn(
                    scope = viewModel.viewModelScope,
                    started = SharingStarted.Eagerly,
                    initialValue = IcsiEditStoreState(form = null),
                ),
            viewModel.viewModelScope,
        )

    /** Updates the procedure date. */
    fun onDateChange(value: String) {
        viewModel.onDateChange(value)
    }

    /** Updates the number of recovered follicles. */
    fun onFolliclesRecoveredChange(value: String) {
        viewModel.onFolliclesRecoveredChange(value)
    }

    /** Updates the attending veterinarian. */
    fun onVetNameChange(value: String) {
        viewModel.onVetNameChange(value)
    }

    /** Updates the notes. */
    fun onNotesChange(value: String) {
        viewModel.onNotesChange(value)
    }

    /** Validates and persists the form. */
    fun save() {
        viewModel.save()
    }

    /** Clears the current form error. */
    fun dismissError() {
        viewModel.onDismissError()
    }
}
