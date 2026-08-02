package com.github.rodrigotimoteo.animally.presentation.labresult

import androidx.lifecycle.viewModelScope
import com.github.rodrigotimoteo.animally.di.dispatchers.IO_DISPATCHER
import com.github.rodrigotimoteo.animally.domain.labresult.model.LabResult
import com.github.rodrigotimoteo.animally.domain.labresult.usecase.GetLabResultDetailUseCase
import com.github.rodrigotimoteo.animally.domain.labresult.usecase.SaveLabResultUseCase
import com.github.rodrigotimoteo.animally.presentation.common.addEdit.BaseAddEditViewModel
import com.github.rodrigotimoteo.animally.presentation.navigation.AnimallyNavigator
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import org.koin.core.annotation.Named
import kotlin.time.Clock

/**
 * View model for the lab result add/edit form.
 *
 * @param patientId The id of the patient this lab result belongs to.
 * @param labResultId The id of the lab result being edited, or `null` when creating a new one.
 * @param getLabResultDetailUseCase Use case for loading an existing lab result.
 * @param saveLabResultUseCase Use case for persisting the lab result.
 * @param animallyNavigator The navigator to use for navigation.
 * @param ioDispatcher Dispatcher for blocking database work.
 */
class LabResultEditViewModel(
    private val patientId: Long,
    private val labResultId: Long?,
    private val getLabResultDetailUseCase: GetLabResultDetailUseCase,
    private val saveLabResultUseCase: SaveLabResultUseCase,
    animallyNavigator: AnimallyNavigator,
    @Named(IO_DISPATCHER) private val ioDispatcher: CoroutineDispatcher,
) : BaseAddEditViewModel<LabResultFormState>(animallyNavigator) {
    init {
        if (labResultId != null) {
            loadLabResult(labResultId)
        } else {
            updateForm(LabResultFormState())
        }
    }

    private fun loadLabResult(id: Long) {
        updateForm(LabResultFormState(id = id, isLoading = true))
        viewModelScope.launch {
            runCatching { withContext(ioDispatcher) { getLabResultDetailUseCase(id) } }
                .onSuccess { labResult ->
                    if (labResult == null) {
                        updateForm(LabResultFormState(id = id, dateError = "Lab result not found"))
                    } else {
                        updateForm(
                            LabResultFormState(
                                id = labResult.id,
                                testType = labResult.testType,
                                date = labResult.date.toString(),
                                results = labResult.results,
                                normalRange = labResult.normalRange,
                                vetName = labResult.vetName,
                                notes = labResult.notes,
                                createdAt = labResult.createdAt,
                            ),
                        )
                    }
                }.onFailure { error ->
                    updateForm(
                        LabResultFormState(
                            id = id,
                            dateError = error.message ?: "Failed to load lab result",
                        ),
                    )
                }
        }
    }

    /**
     * Updates the [LabResultFormState.testType].
     */
    fun onTestTypeChange(value: String) {
        formState.value?.let { updateForm(it.copy(testType = value, testTypeError = null)) }
    }

    /**
     * Updates the [LabResultFormState.date].
     */
    fun onDateChange(value: String) {
        formState.value?.let { updateForm(it.copy(date = value, dateError = null)) }
    }

    /**
     * Updates the [LabResultFormState.results].
     */
    fun onResultsChange(value: String) {
        formState.value?.let { updateForm(it.copy(results = value.ifBlank { null })) }
    }

    /**
     * Updates the [LabResultFormState.normalRange].
     */
    fun onNormalRangeChange(value: String) {
        formState.value?.let { updateForm(it.copy(normalRange = value.ifBlank { null })) }
    }

    /**
     * Updates the [LabResultFormState.vetName].
     */
    fun onVetNameChange(value: String) {
        formState.value?.let { updateForm(it.copy(vetName = value.ifBlank { null })) }
    }

    /**
     * Updates the [LabResultFormState.notes].
     */
    fun onNotesChange(value: String) {
        formState.value?.let { updateForm(it.copy(notes = value.ifBlank { null })) }
    }

    /**
     * Validates and persists the current form.
     */
    override fun save() {
        val form = formState.value ?: return
        if (form.testType.isBlank()) {
            updateForm(form.copy(testTypeError = "Test type is required"))
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
            val labResult =
                LabResult(
                    id = form.id ?: 0L,
                    patientId = patientId,
                    testType = form.testType,
                    date = date,
                    results = form.results,
                    normalRange = form.normalRange,
                    vetName = form.vetName,
                    notes = form.notes,
                    createdAt = form.createdAt ?: now,
                    updatedAt = now,
                )
            runCatching { withContext(ioDispatcher) { saveLabResultUseCase(labResult) } }
                .onSuccess {
                    formState.value?.let { updateForm(it.copy(isSaving = false)) }
                    popBackStack()
                }.onFailure { error ->
                    formState.value?.let {
                        updateForm(
                            it.copy(
                                isSaving = false,
                                dateError = error.message ?: "Failed to save lab result",
                            ),
                        )
                    }
                }
        }
    }

    private fun parseDateOrNull(value: String): LocalDate? = runCatching { LocalDate.parse(value) }.getOrNull()
}
