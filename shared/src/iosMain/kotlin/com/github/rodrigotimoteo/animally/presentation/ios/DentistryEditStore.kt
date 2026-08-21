@file:OptIn(ExperimentalObjCName::class)

package com.github.rodrigotimoteo.animally.presentation.ios

import androidx.lifecycle.viewModelScope
import com.github.rodrigotimoteo.animally.bridge.NativeFlow
import com.github.rodrigotimoteo.animally.presentation.dentistry.DentistryEditViewModel
import com.github.rodrigotimoteo.animally.presentation.dentistry.DentistryFormState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlin.experimental.ExperimentalObjCName
import kotlin.native.ObjCName

/**
 * Swift-facing state of the dentistry add/edit form.
 *
 * Wraps the view model's nullable [DentistryFormState], so the store exposes a
 * non-null [NativeFlow] value.
 *
 * @property form The current form state, or `null` before the form has loaded.
 */
@ObjCName("DentistryEditStoreState")
data class DentistryEditStoreState(
    val form: DentistryFormState? = null,
)

/**
 * Swift-facing store wrapping [DentistryEditViewModel].
 *
 * Exposes only data actions; navigation is owned by SwiftUI.
 */
@ObjCName("DentistryEditStore")
class DentistryEditStore(
    private val viewModel: DentistryEditViewModel,
) {
    /** Observable form state of the dentistry add/edit screen. */
    val state: NativeFlow<DentistryEditStoreState> =
        NativeFlow(
            viewModel.formState
                .map { DentistryEditStoreState(form = it) }
                .stateIn(
                    scope = viewModel.viewModelScope,
                    started = SharingStarted.Eagerly,
                    initialValue = DentistryEditStoreState(form = null),
                ),
            viewModel.viewModelScope,
        )

    /** Updates the dental check date. */
    fun onDateChange(date: String) {
        viewModel.onDateChange(date)
    }

    /** Updates the findings from the examination. */
    fun onFindingsChange(findings: String) {
        viewModel.onFindingsChange(findings)
    }

    /** Updates the treatment performed. */
    fun onTreatmentChange(treatment: String) {
        viewModel.onTreatmentChange(treatment)
    }

    /** Updates the date the next dental check is due. */
    fun onNextDueDateChange(nextDueDate: String) {
        viewModel.onNextDueDateChange(nextDueDate)
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
