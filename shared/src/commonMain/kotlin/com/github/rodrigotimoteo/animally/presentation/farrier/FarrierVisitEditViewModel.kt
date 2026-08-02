package com.github.rodrigotimoteo.animally.presentation.farrier

import androidx.lifecycle.viewModelScope
import com.github.rodrigotimoteo.animally.di.dispatchers.IO_DISPATCHER
import com.github.rodrigotimoteo.animally.domain.farrier.model.FarrierVisit
import com.github.rodrigotimoteo.animally.domain.farrier.usecase.GetFarrierVisitDetailUseCase
import com.github.rodrigotimoteo.animally.domain.farrier.usecase.SaveFarrierVisitUseCase
import com.github.rodrigotimoteo.animally.presentation.common.addEdit.BaseAddEditViewModel
import com.github.rodrigotimoteo.animally.presentation.navigation.AnimallyNavigator
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import org.koin.core.annotation.Named
import kotlin.time.Clock

/**
 * View model for the farrier visit add/edit form.
 *
 * @param patientId The id of the patient this farrier visit belongs to.
 * @param farrierVisitId The id of the farrier visit being edited, or `null` when creating a new one.
 * @param getFarrierVisitDetailUseCase Use case for loading an existing farrier visit.
 * @param saveFarrierVisitUseCase Use case for persisting the farrier visit.
 * @param animallyNavigator The navigator to use for navigation.
 * @param ioDispatcher Dispatcher for blocking database work.
 */
class FarrierVisitEditViewModel(
    private val patientId: Long,
    private val farrierVisitId: Long?,
    private val getFarrierVisitDetailUseCase: GetFarrierVisitDetailUseCase,
    private val saveFarrierVisitUseCase: SaveFarrierVisitUseCase,
    animallyNavigator: AnimallyNavigator,
    @Named(IO_DISPATCHER) private val ioDispatcher: CoroutineDispatcher,
) : BaseAddEditViewModel<FarrierVisitFormState>(animallyNavigator) {
    init {
        if (farrierVisitId != null) {
            loadFarrierVisit(farrierVisitId)
        } else {
            updateForm(FarrierVisitFormState())
        }
    }

    private fun loadFarrierVisit(id: Long) {
        updateForm(FarrierVisitFormState(id = id, isLoading = true))
        viewModelScope.launch {
            runCatching { withContext(ioDispatcher) { getFarrierVisitDetailUseCase(id) } }
                .onSuccess { visit ->
                    if (visit == null) {
                        updateForm(FarrierVisitFormState(id = id, dateError = "Farrier visit not found"))
                    } else {
                        updateForm(
                            FarrierVisitFormState(
                                id = visit.id,
                                date = visit.date.toString(),
                                trimOrShoe = visit.trimOrShoe,
                                shoeType = visit.shoeType,
                                findings = visit.findings,
                                nextDueDate = visit.nextDueDate?.toString(),
                                farrier = visit.farrier,
                                notes = visit.notes,
                                createdAt = visit.createdAt,
                            ),
                        )
                    }
                }.onFailure { error ->
                    updateForm(
                        FarrierVisitFormState(
                            id = id,
                            dateError = error.message ?: "Failed to load farrier visit",
                        ),
                    )
                }
        }
    }

    /**
     * Updates the [FarrierVisitFormState.date].
     */
    fun onDateChange(value: String) {
        formState.value?.let { updateForm(it.copy(date = value, dateError = null)) }
    }

    /**
     * Updates the [FarrierVisitFormState.trimOrShoe].
     */
    fun onTrimOrShoeChange(value: String) {
        formState.value?.let { updateForm(it.copy(trimOrShoe = value.ifBlank { null })) }
    }

    /**
     * Updates the [FarrierVisitFormState.shoeType].
     */
    fun onShoeTypeChange(value: String) {
        formState.value?.let { updateForm(it.copy(shoeType = value.ifBlank { null })) }
    }

    /**
     * Updates the [FarrierVisitFormState.findings].
     */
    fun onFindingsChange(value: String) {
        formState.value?.let { updateForm(it.copy(findings = value.ifBlank { null })) }
    }

    /**
     * Updates the [FarrierVisitFormState.nextDueDate].
     */
    fun onNextDueDateChange(value: String) {
        formState.value?.let { updateForm(it.copy(nextDueDate = value.ifBlank { null }, nextDueDateError = null)) }
    }

    /**
     * Updates the [FarrierVisitFormState.farrier].
     */
    fun onFarrierChange(value: String) {
        formState.value?.let { updateForm(it.copy(farrier = value.ifBlank { null })) }
    }

    /**
     * Updates the [FarrierVisitFormState.notes].
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
            val visit =
                FarrierVisit(
                    id = form.id ?: 0L,
                    patientId = patientId,
                    date = date,
                    trimOrShoe = form.trimOrShoe,
                    shoeType = form.shoeType,
                    findings = form.findings,
                    nextDueDate = nextDueDate,
                    farrier = form.farrier,
                    notes = form.notes,
                    createdAt = form.createdAt ?: now,
                    updatedAt = now,
                )
            runCatching { withContext(ioDispatcher) { saveFarrierVisitUseCase(visit) } }
                .onSuccess {
                    formState.value?.let { updateForm(it.copy(isSaving = false)) }
                    popBackStack()
                }.onFailure { error ->
                    formState.value?.let {
                        updateForm(
                            it.copy(
                                isSaving = false,
                                dateError = error.message ?: "Failed to save farrier visit",
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
