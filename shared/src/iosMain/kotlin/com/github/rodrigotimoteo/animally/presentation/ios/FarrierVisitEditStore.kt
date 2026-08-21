@file:OptIn(ExperimentalObjCName::class)

package com.github.rodrigotimoteo.animally.presentation.ios

import androidx.lifecycle.viewModelScope
import com.github.rodrigotimoteo.animally.bridge.NativeFlow
import com.github.rodrigotimoteo.animally.presentation.farrier.FarrierVisitEditViewModel
import com.github.rodrigotimoteo.animally.presentation.farrier.FarrierVisitFormState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlin.experimental.ExperimentalObjCName
import kotlin.native.ObjCName

/**
 * Swift-facing state of the farrier visit add/edit form.
 *
 * Wraps the view model's nullable [FarrierVisitFormState], so the store exposes
 * a non-null [NativeFlow] value.
 *
 * @property form The current form state, or `null` before the form has loaded.
 */
@ObjCName("FarrierVisitEditStoreState")
data class FarrierVisitEditStoreState(
    val form: FarrierVisitFormState? = null,
)

/**
 * Swift-facing store wrapping [FarrierVisitEditViewModel].
 *
 * Exposes only data actions; navigation is owned by SwiftUI.
 */
@ObjCName("FarrierVisitEditStore")
class FarrierVisitEditStore(
    private val viewModel: FarrierVisitEditViewModel,
) {
    /** Observable form state of the farrier visit add/edit screen. */
    val state: NativeFlow<FarrierVisitEditStoreState> =
        NativeFlow(
            viewModel.formState
                .map { FarrierVisitEditStoreState(form = it) }
                .stateIn(
                    scope = viewModel.viewModelScope,
                    started = SharingStarted.Eagerly,
                    initialValue = FarrierVisitEditStoreState(form = null),
                ),
            viewModel.viewModelScope,
        )

    /** Updates the visit date. */
    fun onDateChange(date: String) {
        viewModel.onDateChange(date)
    }

    /** Updates the trim or shoeing type. */
    fun onTrimOrShoeChange(trimOrShoe: String) {
        viewModel.onTrimOrShoeChange(trimOrShoe)
    }

    /** Updates the shoe type applied. */
    fun onShoeTypeChange(shoeType: String) {
        viewModel.onShoeTypeChange(shoeType)
    }

    /** Updates the findings from the examination. */
    fun onFindingsChange(findings: String) {
        viewModel.onFindingsChange(findings)
    }

    /** Updates the date of the next scheduled visit. */
    fun onNextDueDateChange(nextDueDate: String) {
        viewModel.onNextDueDateChange(nextDueDate)
    }

    /** Updates the name of the farrier. */
    fun onFarrierChange(farrier: String) {
        viewModel.onFarrierChange(farrier)
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
