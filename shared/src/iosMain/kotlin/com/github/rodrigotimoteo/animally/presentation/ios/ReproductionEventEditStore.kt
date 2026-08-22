@file:OptIn(ExperimentalObjCName::class)

package com.github.rodrigotimoteo.animally.presentation.ios

import androidx.lifecycle.viewModelScope
import com.github.rodrigotimoteo.animally.bridge.NativeFlow
import com.github.rodrigotimoteo.animally.presentation.reproduction.ReproductionEventEditViewModel
import com.github.rodrigotimoteo.animally.presentation.reproduction.ReproductionEventFormState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlin.experimental.ExperimentalObjCName
import kotlin.native.ObjCName

/**
 * Swift-facing state of the reproduction-event add/edit form.
 *
 * Wraps the view model's nullable [ReproductionEventFormState], so the store
 * exposes a non-null [NativeFlow] value.
 *
 * @property form The current form state, or `null` before the form has loaded.
 */
@ObjCName("ReproductionEventEditStoreState")
data class ReproductionEventEditStoreState(
    val form: ReproductionEventFormState? = null,
)

/**
 * Swift-facing store wrapping [ReproductionEventEditViewModel].
 *
 * Exposes only data actions; navigation is owned by SwiftUI.
 */
@ObjCName("ReproductionEventEditStore")
class ReproductionEventEditStore(
    private val viewModel: ReproductionEventEditViewModel,
) {
    /** Observable form state of the reproduction-event add/edit screen. */
    val state: NativeFlow<ReproductionEventEditStoreState> =
        NativeFlow(
            viewModel.formState
                .map { ReproductionEventEditStoreState(form = it) }
                .stateIn(
                    scope = viewModel.viewModelScope,
                    started = SharingStarted.Eagerly,
                    initialValue = ReproductionEventEditStoreState(form = null),
                ),
            viewModel.viewModelScope,
        )

    /** Updates the event type (heat, breeding, pregnancy check, foaling). */
    fun onEventTypeChange(value: String) {
        viewModel.onEventTypeChange(value)
    }

    /** Updates the event date. */
    fun onDateChange(date: String) {
        viewModel.onDateChange(date)
    }

    /** Updates the event details. */
    fun onDetailsChange(value: String) {
        viewModel.onDetailsChange(value)
    }

    /** Updates the initial reproductive exam findings. */
    fun onInitialExamFindingsChange(value: String) {
        viewModel.onInitialExamFindingsChange(value)
    }

    /** Updates the stallion used for breeding. */
    fun onStallionNameChange(value: String) {
        viewModel.onStallionNameChange(value)
    }

    /** Updates the breeding type (NATURAL_COVER / ARTIFICIAL_INSEMINATION / EMBRYO_RECIPIENT). */
    fun onBreedingTypeChange(value: String) {
        viewModel.onBreedingTypeChange(value)
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
