package com.github.rodrigotimoteo.animally.presentation.weight

import androidx.lifecycle.viewModelScope
import com.github.rodrigotimoteo.animally.di.dispatchers.IO_DISPATCHER
import com.github.rodrigotimoteo.animally.domain.weight.model.Weight
import com.github.rodrigotimoteo.animally.domain.weight.usecase.GetWeightDetailUseCase
import com.github.rodrigotimoteo.animally.domain.weight.usecase.SaveWeightUseCase
import com.github.rodrigotimoteo.animally.presentation.common.addEdit.BaseAddEditViewModel
import com.github.rodrigotimoteo.animally.presentation.navigation.AnimallyNavigator
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import org.koin.core.annotation.Named
import kotlin.time.Clock

/**
 * View model for the weight add/edit form.
 *
 * @param patientId The id of the patient this weight entry belongs to.
 * @param weightId The id of the weight entry being edited, or `null` when creating a new one.
 * @param getWeightDetailUseCase Use case for loading an existing weight entry.
 * @param saveWeightUseCase Use case for persisting the weight entry.
 * @param animallyNavigator The navigator to use for navigation.
 * @param ioDispatcher Dispatcher for blocking database work.
 */
class WeightEditViewModel(
    private val patientId: Long,
    private val weightId: Long?,
    private val getWeightDetailUseCase: GetWeightDetailUseCase,
    private val saveWeightUseCase: SaveWeightUseCase,
    animallyNavigator: AnimallyNavigator,
    @Named(IO_DISPATCHER) private val ioDispatcher: CoroutineDispatcher,
) : BaseAddEditViewModel<WeightFormState>(animallyNavigator) {
    init {
        if (weightId != null) {
            loadWeight(weightId)
        } else {
            updateForm(WeightFormState())
        }
    }

    private fun loadWeight(id: Long) {
        updateForm(WeightFormState(id = id, isLoading = true))
        viewModelScope.launch {
            runCatching { withContext(ioDispatcher) { getWeightDetailUseCase(id) } }
                .onSuccess { weight ->
                    if (weight == null) {
                        updateForm(WeightFormState(id = id, dateError = "Weight not found"))
                    } else {
                        updateForm(
                            WeightFormState(
                                id = weight.id,
                                weightKg = weight.weightKg.toString(),
                                date = weight.date.toString(),
                                notes = weight.notes,
                                createdAt = weight.createdAt,
                            ),
                        )
                    }
                }.onFailure { error ->
                    updateForm(
                        WeightFormState(
                            id = id,
                            dateError = error.message ?: "Failed to load weight",
                        ),
                    )
                }
        }
    }

    /**
     * Updates the [WeightFormState.weightKg].
     */
    fun onWeightKgChange(value: String) {
        formState.value?.let { updateForm(it.copy(weightKg = value, weightError = null)) }
    }

    /**
     * Updates the [WeightFormState.date].
     */
    fun onDateChange(value: String) {
        formState.value?.let { updateForm(it.copy(date = value, dateError = null)) }
    }

    /**
     * Updates the [WeightFormState.notes].
     */
    fun onNotesChange(value: String) {
        formState.value?.let { updateForm(it.copy(notes = value)) }
    }

    /**
     * Validates and persists the current form.
     */
    override fun save() {
        val form = formState.value ?: return
        val invalid = validate(form)
        if (invalid != null) {
            updateForm(invalid)
            return
        }
        persist(form)
    }

    private fun validate(form: WeightFormState): WeightFormState? {
        var invalid: WeightFormState? = null
        val weightKg = form.weightKg.toDoubleOrNull()
        if (weightKg == null) {
            val message = if (form.weightKg.isBlank()) "Weight is required" else "Invalid weight"
            invalid = form.copy(weightError = message)
        } else if (weightKg <= 0) {
            invalid = form.copy(weightError = "Weight must be greater than 0")
        } else {
            val date = parseDateOrNull(form.date)
            if (date == null) {
                val message = if (form.date.isBlank()) "Date is required" else "Invalid date (YYYY-MM-DD)"
                invalid = form.copy(dateError = message)
            }
        }
        return invalid
    }

    private fun persist(form: WeightFormState) {
        val weightKg = form.weightKg.toDoubleOrNull() ?: return
        val date = parseDateOrNull(form.date) ?: return
        viewModelScope.launch {
            updateForm(form.copy(isSaving = true))
            val now = Clock.System.now()
            val weight =
                Weight(
                    id = form.id ?: 0L,
                    patientId = patientId,
                    weightKg = weightKg,
                    date = date,
                    notes = form.notes,
                    createdAt = form.createdAt ?: now,
                    updatedAt = now,
                )
            runCatching { withContext(ioDispatcher) { saveWeightUseCase(weight) } }
                .onSuccess {
                    formState.value?.let { updateForm(it.copy(isSaving = false)) }
                    emitSaved()
                }.onFailure { error ->
                    formState.value?.let {
                        updateForm(
                            it.copy(
                                isSaving = false,
                                weightError = error.message ?: "Failed to save weight",
                            ),
                        )
                    }
                }
        }
    }

    private fun parseDateOrNull(value: String): LocalDate? = runCatching { LocalDate.parse(value) }.getOrNull()
}
