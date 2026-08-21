@file:OptIn(ExperimentalObjCName::class)

package com.github.rodrigotimoteo.animally.presentation.ios

import androidx.lifecycle.viewModelScope
import com.github.rodrigotimoteo.animally.bridge.NativeFlow
import com.github.rodrigotimoteo.animally.presentation.consultation.ConsultationEditViewModel
import com.github.rodrigotimoteo.animally.presentation.consultation.ConsultationFormState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlin.experimental.ExperimentalObjCName
import kotlin.native.ObjCName

/**
 * Swift-facing state of the consultation add/edit form.
 *
 * Wraps the view model's nullable [ConsultationFormState], so the store exposes
 * a non-null [NativeFlow] value.
 *
 * @property form The current form state, or `null` before the form has loaded.
 */
@ObjCName("ConsultationEditStoreState")
data class ConsultationEditStoreState(
    val form: ConsultationFormState? = null,
)

/**
 * Swift-facing store wrapping [ConsultationEditViewModel].
 *
 * Exposes only data actions; navigation is owned by SwiftUI.
 */
@ObjCName("ConsultationEditStore")
class ConsultationEditStore(
    private val viewModel: ConsultationEditViewModel,
) {
    /** Observable form state of the consultation add/edit screen. */
    val state: NativeFlow<ConsultationEditStoreState> =
        NativeFlow(
            viewModel.formState
                .map { ConsultationEditStoreState(form = it) }
                .stateIn(
                    scope = viewModel.viewModelScope,
                    started = SharingStarted.Eagerly,
                    initialValue = ConsultationEditStoreState(form = null),
                ),
            viewModel.viewModelScope,
        )

    /** Updates the consultation date. */
    fun onDateChange(date: String) {
        viewModel.onDateChange(date)
    }

    /** Updates the SOAP Subjective — the owner's description of the issue. */
    fun onSubjectiveChange(subjective: String) {
        viewModel.onSubjectiveChange(subjective)
    }

    /** Updates the SOAP Objective — the exam findings. */
    fun onObjectiveChange(objective: String) {
        viewModel.onObjectiveChange(objective)
    }

    /** Updates the SOAP Assessment — the diagnosis. */
    fun onAssessmentChange(assessment: String) {
        viewModel.onAssessmentChange(assessment)
    }

    /** Updates the SOAP Plan — the treatment. */
    fun onPlanChange(plan: String) {
        viewModel.onPlanChange(plan)
    }

    /** Updates the name of the attending veterinarian. */
    fun onVetNameChange(vetName: String) {
        viewModel.onVetNameChange(vetName)
    }

    /** Updates the date of the next scheduled visit. */
    fun onNextVisitDateChange(nextVisitDate: String) {
        viewModel.onNextVisitDateChange(nextVisitDate)
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
