@file:OptIn(ExperimentalObjCName::class)

package com.github.rodrigotimoteo.animally.presentation.ios

import androidx.lifecycle.viewModelScope
import com.github.rodrigotimoteo.animally.bridge.NativeFlow
import com.github.rodrigotimoteo.animally.presentation.deworming.DewormingEditViewModel
import com.github.rodrigotimoteo.animally.presentation.deworming.DewormingFormState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlin.experimental.ExperimentalObjCName
import kotlin.native.ObjCName

/**
 * Swift-facing state of the deworming add/edit form.
 *
 * Wraps the view model's nullable [DewormingFormState], so the store exposes a
 * non-null [NativeFlow] value.
 *
 * @property form The current form state, or `null` before the form has loaded.
 */
@ObjCName("DewormingEditStoreState")
data class DewormingEditStoreState(
    val form: DewormingFormState? = null,
)

/**
 * Swift-facing store wrapping [DewormingEditViewModel].
 *
 * Exposes only data actions; navigation is owned by SwiftUI.
 */
@ObjCName("DewormingEditStore")
class DewormingEditStore(
    private val viewModel: DewormingEditViewModel,
) {
    /** Observable form state of the deworming add/edit screen. */
    val state: NativeFlow<DewormingEditStoreState> =
        NativeFlow(
            viewModel.formState
                .map { DewormingEditStoreState(form = it) }
                .stateIn(
                    scope = viewModel.viewModelScope,
                    started = SharingStarted.Eagerly,
                    initialValue = DewormingEditStoreState(form = null),
                ),
            viewModel.viewModelScope,
        )

    /** Updates the anthelmintic product administered. */
    fun onProductChange(product: String) {
        viewModel.onProductChange(product)
    }

    /** Updates the administration date. */
    fun onDateAdministeredChange(dateAdministered: String) {
        viewModel.onDateAdministeredChange(dateAdministered)
    }

    /** Updates the date the next deworming is due. */
    fun onNextDueDateChange(nextDueDate: String) {
        viewModel.onNextDueDateChange(nextDueDate)
    }

    /** Updates the dosage information. */
    fun onDoseChange(dose: String) {
        viewModel.onDoseChange(dose)
    }

    /** Updates the name of the attending veterinarian. */
    fun onVetNameChange(vetName: String) {
        viewModel.onVetNameChange(vetName)
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
