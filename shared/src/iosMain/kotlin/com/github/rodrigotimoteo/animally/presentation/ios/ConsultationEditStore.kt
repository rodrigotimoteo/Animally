@file:OptIn(ExperimentalObjCName::class)

package com.github.rodrigotimoteo.animally.presentation.ios

import com.github.rodrigotimoteo.animally.bridge.NativeFlow
import com.github.rodrigotimoteo.animally.presentation.consultation.ConsultationEditViewModel
import com.github.rodrigotimoteo.animally.presentation.consultation.ConsultationFormState
import com.github.rodrigotimoteo.animally.presentation.ios.base.GenericEditStore
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
 * Delegates NativeFlow binding and save/dismiss to [GenericEditStore].
 */
@ObjCName("ConsultationEditStore")
class ConsultationEditStore(
    viewModel: ConsultationEditViewModel,
) : GenericEditStore<ConsultationEditViewModel, ConsultationFormState, ConsultationEditStoreState>(viewModel) {
    /** Observable form state of the consultation add/edit screen. */
    override val state: NativeFlow<ConsultationEditStoreState> =
        viewModel.formState.toStoreStateFlow(ConsultationEditStoreState()) { ConsultationEditStoreState(form = it) }

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
}
