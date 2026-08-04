package com.github.rodrigotimoteo.animally.presentation.consultation

import androidx.lifecycle.viewModelScope
import com.github.rodrigotimoteo.animally.di.dispatchers.IO_DISPATCHER
import com.github.rodrigotimoteo.animally.domain.consultation.model.Consultation
import com.github.rodrigotimoteo.animally.domain.consultation.usecase.GetConsultationDetailUseCase
import com.github.rodrigotimoteo.animally.domain.consultation.usecase.SaveConsultationUseCase
import com.github.rodrigotimoteo.animally.presentation.common.addEdit.BaseAddEditViewModel
import com.github.rodrigotimoteo.animally.presentation.navigation.AnimallyNavigator
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import org.koin.core.annotation.Named
import kotlin.time.Clock

/**
 * View model for the consultation add/edit form.
 *
 * @param patientId The id of the patient this consultation belongs to.
 * @param consultationId The id of the consultation being edited, or `null` when creating a new one.
 * @param getConsultationDetailUseCase Use case for loading an existing consultation.
 * @param saveConsultationUseCase Use case for persisting the consultation.
 * @param animallyNavigator The navigator to use for navigation.
 * @param ioDispatcher Dispatcher for blocking database work.
 */
class ConsultationEditViewModel(
    private val patientId: Long,
    private val consultationId: Long?,
    private val getConsultationDetailUseCase: GetConsultationDetailUseCase,
    private val saveConsultationUseCase: SaveConsultationUseCase,
    animallyNavigator: AnimallyNavigator,
    @Named(IO_DISPATCHER) private val ioDispatcher: CoroutineDispatcher,
) : BaseAddEditViewModel<ConsultationFormState>(animallyNavigator) {
    init {
        if (consultationId != null) {
            loadConsultation(consultationId)
        } else {
            updateForm(ConsultationFormState())
        }
    }

    private fun loadConsultation(id: Long) {
        updateForm(ConsultationFormState(id = id, isLoading = true))
        viewModelScope.launch {
            runCatching { withContext(ioDispatcher) { getConsultationDetailUseCase(id) } }
                .onSuccess { consultation ->
                    if (consultation == null) {
                        updateForm(ConsultationFormState(id = id, dateError = "Consultation not found"))
                    } else {
                        updateForm(
                            ConsultationFormState(
                                id = consultation.id,
                                date = consultation.date.toString(),
                                subjective = consultation.subjective,
                                objective = consultation.objective,
                                assessment = consultation.assessment,
                                plan = consultation.plan,
                                vetName = consultation.vetName,
                                nextVisitDate = consultation.nextVisitDate?.toString(),
                                createdAt = consultation.createdAt,
                            ),
                        )
                    }
                }.onFailure { error ->
                    updateForm(
                        ConsultationFormState(
                            id = id,
                            dateError = error.message ?: "Failed to load consultation",
                        ),
                    )
                }
        }
    }

    /**
     * Updates the [ConsultationFormState.date].
     */
    fun onDateChange(date: String) {
        formState.value?.let { updateForm(it.copy(date = date, dateError = null)) }
    }

    /**
     * Updates the [ConsultationFormState.subjective].
     */
    fun onSubjectiveChange(value: String) {
        formState.value?.let { updateForm(it.copy(subjective = value)) }
    }

    /**
     * Updates the [ConsultationFormState.objective].
     */
    fun onObjectiveChange(value: String) {
        formState.value?.let { updateForm(it.copy(objective = value)) }
    }

    /**
     * Updates the [ConsultationFormState.assessment].
     */
    fun onAssessmentChange(value: String) {
        formState.value?.let { updateForm(it.copy(assessment = value)) }
    }

    /**
     * Updates the [ConsultationFormState.plan].
     */
    fun onPlanChange(value: String) {
        formState.value?.let { updateForm(it.copy(plan = value)) }
    }

    /**
     * Updates the [ConsultationFormState.vetName].
     */
    fun onVetNameChange(value: String) {
        formState.value?.let { updateForm(it.copy(vetName = value.ifBlank { null })) }
    }

    /**
     * Updates the [ConsultationFormState.nextVisitDate].
     */
    fun onNextVisitDateChange(value: String) {
        formState.value?.let { updateForm(it.copy(nextVisitDate = value.ifBlank { null }, nextVisitDateError = null)) }
    }

    /**
     * Validates and persists the current form.
     */
    override fun save() {
        val form = formState.value ?: return
        val date = parseDateOrNull(form.date)
        if (date == null) {
            val message = if (form.date.isBlank()) "Date is required" else "Invalid date (YYYY-MM-DD)"
            updateForm(form.copy(dateError = message))
            return
        }
        val nextVisitDate = parseDateOrNull(form.nextVisitDate)
        if (form.nextVisitDate != null && nextVisitDate == null) {
            updateForm(form.copy(nextVisitDateError = "Invalid date (YYYY-MM-DD)"))
            return
        }
        viewModelScope.launch {
            updateForm(form.copy(isSaving = true))
            val now = Clock.System.now()
            val consultation =
                Consultation(
                    id = form.id ?: 0L,
                    patientId = patientId,
                    date = date,
                    subjective = form.subjective,
                    objective = form.objective,
                    assessment = form.assessment,
                    plan = form.plan,
                    vetName = form.vetName,
                    nextVisitDate = nextVisitDate,
                    createdAt = form.createdAt ?: now,
                    updatedAt = now,
                )
            runCatching { withContext(ioDispatcher) { saveConsultationUseCase(consultation) } }
                .onSuccess {
                    formState.value?.let { updateForm(it.copy(isSaving = false)) }
                    emitSaved()
                }.onFailure { error ->
                    formState.value?.let {
                        updateForm(
                            it.copy(
                                isSaving = false,
                                dateError = error.message ?: "Failed to save consultation",
                            ),
                        )
                    }
                }
        }
    }

    private fun parseDateOrNull(value: String?): LocalDate? {
        if (value == null) return null
        return runCatching { LocalDate.parse(value) }.getOrNull()
    }
}
