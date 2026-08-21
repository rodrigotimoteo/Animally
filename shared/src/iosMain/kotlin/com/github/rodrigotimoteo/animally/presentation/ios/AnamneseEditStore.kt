@file:OptIn(ExperimentalObjCName::class)

package com.github.rodrigotimoteo.animally.presentation.ios

import androidx.lifecycle.viewModelScope
import com.github.rodrigotimoteo.animally.bridge.NativeFlow
import com.github.rodrigotimoteo.animally.presentation.anamnese.AnamneseFormState
import com.github.rodrigotimoteo.animally.presentation.anamnese.AnamneseViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlin.experimental.ExperimentalObjCName
import kotlin.native.ObjCName

/**
 * Swift-facing state of the anamnese form.
 *
 * Wraps the view model's nullable [AnamneseFormState], so the store exposes a
 * non-null [NativeFlow] value.
 *
 * @property form The current form state, or `null` before the form has loaded.
 */
@ObjCName("AnamneseEditStoreState")
data class AnamneseEditStoreState(
    val form: AnamneseFormState? = null,
)

/**
 * Swift-facing store wrapping [AnamneseViewModel].
 *
 * Exposes only data actions; navigation is owned by SwiftUI.
 */
@ObjCName("AnamneseEditStore")
class AnamneseEditStore(
    private val viewModel: AnamneseViewModel,
) {
    /** Observable form state of the anamnese screen. */
    val state: NativeFlow<AnamneseEditStoreState> =
        NativeFlow(
            viewModel.formState
                .map { AnamneseEditStoreState(form = it) }
                .stateIn(
                    scope = viewModel.viewModelScope,
                    started = SharingStarted.Eagerly,
                    initialValue = AnamneseEditStoreState(form = null),
                ),
            viewModel.viewModelScope,
        )

    /** Updates the free-form general medical history. */
    fun onGeneralHistoryChange(value: String) {
        viewModel.onGeneralHistoryChange(value)
    }

    /** Updates the free-form chronic conditions. */
    fun onChronicConditionsChange(value: String) {
        viewModel.onChronicConditionsChange(value)
    }

    /** Updates the free-form allergies. */
    fun onAllergiesChange(value: String) {
        viewModel.onAllergiesChange(value)
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
