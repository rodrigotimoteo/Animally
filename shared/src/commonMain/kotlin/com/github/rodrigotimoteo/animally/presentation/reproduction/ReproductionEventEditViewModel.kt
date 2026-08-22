package com.github.rodrigotimoteo.animally.presentation.reproduction

import androidx.lifecycle.viewModelScope
import com.github.rodrigotimoteo.animally.di.dispatchers.IO_DISPATCHER
import com.github.rodrigotimoteo.animally.domain.reproduction.model.ReproductionEvent
import com.github.rodrigotimoteo.animally.domain.reproduction.usecase.GetReproductionEventDetailUseCase
import com.github.rodrigotimoteo.animally.domain.reproduction.usecase.SaveReproductionEventUseCase
import com.github.rodrigotimoteo.animally.presentation.common.addEdit.BaseAddEditViewModel
import com.github.rodrigotimoteo.animally.presentation.navigation.AnimallyNavigator
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import org.koin.core.annotation.Named
import kotlin.time.Clock

/**
 * View model for the reproduction-event add/edit form.
 *
 * @param patientId The id of the patient this reproduction event belongs to.
 * @param reproductionEventId The id of the reproduction event being edited, or `null` when creating a new one.
 * @param getReproductionEventDetailUseCase Use case for loading an existing reproduction event.
 * @param saveReproductionEventUseCase Use case for persisting the reproduction event.
 * @param animallyNavigator The navigator to use for navigation.
 * @param ioDispatcher Dispatcher for blocking database work.
 */
class ReproductionEventEditViewModel(
    private val patientId: Long,
    private val reproductionEventId: Long?,
    private val getReproductionEventDetailUseCase: GetReproductionEventDetailUseCase,
    private val saveReproductionEventUseCase: SaveReproductionEventUseCase,
    animallyNavigator: AnimallyNavigator,
    @Named(IO_DISPATCHER) private val ioDispatcher: CoroutineDispatcher,
) : BaseAddEditViewModel<ReproductionEventFormState>(animallyNavigator) {
    init {
        if (reproductionEventId != null) {
            loadReproductionEvent(reproductionEventId)
        } else {
            updateForm(ReproductionEventFormState())
        }
    }

    private fun loadReproductionEvent(id: Long) {
        updateForm(ReproductionEventFormState(id = id, isLoading = true))
        viewModelScope.launch {
            runCatching { withContext(ioDispatcher) { getReproductionEventDetailUseCase(id) } }
                .onSuccess { event ->
                    if (event == null) {
                        updateForm(ReproductionEventFormState(id = id, eventTypeError = "Event not found"))
                    } else {
                        updateForm(
                            ReproductionEventFormState(
                                id = event.id,
                                eventType = event.eventType,
                                date = event.date.toString(),
                                details = event.details,
                                initialExamFindings = event.initialExamFindings,
                                stallionName = event.stallionName,
                                breedingType = event.breedingType,
                                vetName = event.vetName,
                                notes = event.notes,
                                createdAt = event.createdAt,
                            ),
                        )
                    }
                }.onFailure { error ->
                    updateForm(
                        ReproductionEventFormState(
                            id = id,
                            eventTypeError = error.message ?: "Failed to load reproduction event",
                        ),
                    )
                }
        }
    }

    /**
     * Updates the [ReproductionEventFormState.eventType].
     */
    fun onEventTypeChange(value: String) {
        formState.value?.let { updateForm(it.copy(eventType = value, eventTypeError = null)) }
    }

    /**
     * Updates the [ReproductionEventFormState.date].
     */
    fun onDateChange(date: String) {
        formState.value?.let { updateForm(it.copy(date = date, dateError = null)) }
    }

    /**
     * Updates the [ReproductionEventFormState.details].
     */
    fun onDetailsChange(value: String) {
        formState.value?.let { updateForm(it.copy(details = value.ifBlank { null })) }
    }

    /**
     * Updates the [ReproductionEventFormState.initialExamFindings].
     */
    fun onInitialExamFindingsChange(value: String) {
        formState.value?.let { updateForm(it.copy(initialExamFindings = value.ifBlank { null })) }
    }

    /**
     * Updates the [ReproductionEventFormState.stallionName].
     */
    fun onStallionNameChange(value: String) {
        formState.value?.let { updateForm(it.copy(stallionName = value.ifBlank { null })) }
    }

    /**
     * Updates the [ReproductionEventFormState.breedingType]
     * (`NATURAL_COVER` / `ARTIFICIAL_INSEMINATION` / `EMBRYO_RECIPIENT`).
     */
    fun onBreedingTypeChange(value: String) {
        formState.value?.let { updateForm(it.copy(breedingType = value.ifBlank { null })) }
    }

    /**
     * Updates the [ReproductionEventFormState.vetName].
     */
    fun onVetNameChange(value: String) {
        formState.value?.let { updateForm(it.copy(vetName = value.ifBlank { null })) }
    }

    /**
     * Updates the [ReproductionEventFormState.notes].
     */
    fun onNotesChange(value: String) {
        formState.value?.let { updateForm(it.copy(notes = value.ifBlank { null })) }
    }

    /**
     * Validates and persists the current form.
     */
    override fun save() {
        val form = formState.value ?: return
        if (form.eventType.isBlank()) {
            updateForm(form.copy(eventTypeError = "Event type is required"))
            return
        }
        val date = parseDateOrNull(form.date)
        if (date == null) {
            val message = if (form.date.isBlank()) "Date is required" else "Invalid date (YYYY-MM-DD)"
            updateForm(form.copy(dateError = message))
            return
        }
        viewModelScope.launch {
            updateForm(form.copy(isSaving = true))
            val now = Clock.System.now()
            val event =
                ReproductionEvent(
                    id = form.id ?: 0L,
                    patientId = patientId,
                    eventType = form.eventType.trim(),
                    date = date,
                    details = form.details,
                    initialExamFindings = form.initialExamFindings,
                    stallionName = form.stallionName,
                    breedingType = form.breedingType,
                    vetName = form.vetName,
                    notes = form.notes,
                    createdAt = form.createdAt ?: now,
                    updatedAt = now,
                )
            runCatching { withContext(ioDispatcher) { saveReproductionEventUseCase(event) } }
                .onSuccess {
                    formState.value?.let { updateForm(it.copy(isSaving = false)) }
                    emitSaved()
                }.onFailure { error ->
                    formState.value?.let {
                        updateForm(
                            it.copy(
                                isSaving = false,
                                dateError = error.message ?: "Failed to save reproduction event",
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
