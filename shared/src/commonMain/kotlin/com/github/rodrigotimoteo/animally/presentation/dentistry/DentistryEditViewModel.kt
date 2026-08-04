package com.github.rodrigotimoteo.animally.presentation.dentistry

import androidx.lifecycle.viewModelScope
import com.github.rodrigotimoteo.animally.di.dispatchers.IO_DISPATCHER
import com.github.rodrigotimoteo.animally.domain.dentistry.model.Dentistry
import com.github.rodrigotimoteo.animally.domain.dentistry.usecase.GetDentistryDetailUseCase
import com.github.rodrigotimoteo.animally.domain.dentistry.usecase.SaveDentistryUseCase
import com.github.rodrigotimoteo.animally.presentation.common.addEdit.BaseAddEditViewModel
import com.github.rodrigotimoteo.animally.presentation.navigation.AnimallyNavigator
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import org.koin.core.annotation.Named
import kotlin.time.Clock

/**
 * View model for the dentistry add/edit form.
 *
 * @param patientId The id of the patient this dentistry record belongs to.
 * @param dentistryId The id of the dentistry record being edited, or `null` when creating a new one.
 * @param getDentistryDetailUseCase Use case for loading an existing dentistry record.
 * @param saveDentistryUseCase Use case for persisting the dentistry record.
 * @param animallyNavigator The navigator to use for navigation.
 * @param ioDispatcher Dispatcher for blocking database work.
 */
class DentistryEditViewModel(
    private val patientId: Long,
    private val dentistryId: Long?,
    private val getDentistryDetailUseCase: GetDentistryDetailUseCase,
    private val saveDentistryUseCase: SaveDentistryUseCase,
    animallyNavigator: AnimallyNavigator,
    @Named(IO_DISPATCHER) private val ioDispatcher: CoroutineDispatcher,
) : BaseAddEditViewModel<DentistryFormState>(animallyNavigator) {
    init {
        if (dentistryId != null) {
            loadDentistry(dentistryId)
        } else {
            updateForm(DentistryFormState())
        }
    }

    private fun loadDentistry(id: Long) {
        updateForm(DentistryFormState(id = id, isLoading = true))
        viewModelScope.launch {
            runCatching { withContext(ioDispatcher) { getDentistryDetailUseCase(id) } }
                .onSuccess { dentistry ->
                    if (dentistry == null) {
                        updateForm(DentistryFormState(id = id, dateError = "Dentistry record not found"))
                    } else {
                        updateForm(
                            DentistryFormState(
                                id = dentistry.id,
                                date = dentistry.date.toString(),
                                findings = dentistry.findings,
                                treatment = dentistry.treatment,
                                nextDueDate = dentistry.nextDueDate?.toString(),
                                vetName = dentistry.vetName,
                                notes = dentistry.notes,
                                createdAt = dentistry.createdAt,
                            ),
                        )
                    }
                }.onFailure { error ->
                    updateForm(
                        DentistryFormState(
                            id = id,
                            dateError = error.message ?: "Failed to load dentistry record",
                        ),
                    )
                }
        }
    }

    /**
     * Updates the [DentistryFormState.date].
     */
    fun onDateChange(value: String) {
        formState.value?.let { updateForm(it.copy(date = value, dateError = null)) }
    }

    /**
     * Updates the [DentistryFormState.findings].
     */
    fun onFindingsChange(value: String) {
        formState.value?.let { updateForm(it.copy(findings = value.ifBlank { null })) }
    }

    /**
     * Updates the [DentistryFormState.treatment].
     */
    fun onTreatmentChange(value: String) {
        formState.value?.let { updateForm(it.copy(treatment = value.ifBlank { null })) }
    }

    /**
     * Updates the [DentistryFormState.nextDueDate].
     */
    fun onNextDueDateChange(value: String) {
        formState.value?.let { updateForm(it.copy(nextDueDate = value.ifBlank { null }, nextDueDateError = null)) }
    }

    /**
     * Updates the [DentistryFormState.vetName].
     */
    fun onVetNameChange(value: String) {
        formState.value?.let { updateForm(it.copy(vetName = value.ifBlank { null })) }
    }

    /**
     * Updates the [DentistryFormState.notes].
     */
    fun onNotesChange(value: String) {
        formState.value?.let { updateForm(it.copy(notes = value.ifBlank { null })) }
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
        val nextDueDate = parseDateOrNull(form.nextDueDate)
        if (form.nextDueDate != null && nextDueDate == null) {
            updateForm(form.copy(nextDueDateError = "Invalid date (YYYY-MM-DD)"))
            return
        }
        viewModelScope.launch {
            updateForm(form.copy(isSaving = true))
            val now = Clock.System.now()
            val dentistry =
                Dentistry(
                    id = form.id ?: 0L,
                    patientId = patientId,
                    date = date,
                    findings = form.findings,
                    treatment = form.treatment,
                    nextDueDate = nextDueDate,
                    vetName = form.vetName,
                    notes = form.notes,
                    createdAt = form.createdAt ?: now,
                    updatedAt = now,
                )
            runCatching { withContext(ioDispatcher) { saveDentistryUseCase(dentistry) } }
                .onSuccess {
                    formState.value?.let { updateForm(it.copy(isSaving = false)) }
                    emitSaved()
                }.onFailure { error ->
                    formState.value?.let {
                        updateForm(
                            it.copy(
                                isSaving = false,
                                dateError = error.message ?: "Failed to save dentistry record",
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
