package com.github.rodrigotimoteo.animally.presentation.deworming

import androidx.lifecycle.viewModelScope
import com.github.rodrigotimoteo.animally.di.dispatchers.IO_DISPATCHER
import com.github.rodrigotimoteo.animally.domain.deworming.model.Deworming
import com.github.rodrigotimoteo.animally.domain.deworming.usecase.GetDewormingDetailUseCase
import com.github.rodrigotimoteo.animally.domain.deworming.usecase.SaveDewormingUseCase
import com.github.rodrigotimoteo.animally.presentation.common.addEdit.BaseAddEditViewModel
import com.github.rodrigotimoteo.animally.presentation.navigation.AnimallyNavigator
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import org.koin.core.annotation.Named
import kotlin.time.Clock

/**
 * View model for the deworming add/edit form.
 *
 * @param patientId The id of the patient this deworming belongs to.
 * @param dewormingId The id of the deworming being edited, or `null` when creating a new one.
 * @param getDewormingDetailUseCase Use case for loading an existing deworming.
 * @param saveDewormingUseCase Use case for persisting the deworming.
 * @param animallyNavigator The navigator to use for navigation.
 * @param ioDispatcher Dispatcher for blocking database work.
 */
class DewormingEditViewModel(
    private val patientId: Long,
    private val dewormingId: Long?,
    private val getDewormingDetailUseCase: GetDewormingDetailUseCase,
    private val saveDewormingUseCase: SaveDewormingUseCase,
    animallyNavigator: AnimallyNavigator,
    @Named(IO_DISPATCHER) private val ioDispatcher: CoroutineDispatcher,
) : BaseAddEditViewModel<DewormingFormState>(animallyNavigator) {
    init {
        if (dewormingId != null) {
            loadDeworming(dewormingId)
        } else {
            updateForm(DewormingFormState())
        }
    }

    private fun loadDeworming(id: Long) {
        updateForm(DewormingFormState(id = id, isLoading = true))
        viewModelScope.launch {
            runCatching { withContext(ioDispatcher) { getDewormingDetailUseCase(id) } }
                .onSuccess { deworming ->
                    if (deworming == null) {
                        updateForm(DewormingFormState(id = id, dateError = "Deworming not found"))
                    } else {
                        updateForm(
                            DewormingFormState(
                                id = deworming.id,
                                product = deworming.product,
                                dateAdministered = deworming.dateAdministered.toString(),
                                nextDueDate = deworming.nextDueDate?.toString(),
                                dose = deworming.dose,
                                vetName = deworming.vetName,
                                notes = deworming.notes,
                                createdAt = deworming.createdAt,
                            ),
                        )
                    }
                }.onFailure { error ->
                    updateForm(
                        DewormingFormState(
                            id = id,
                            dateError = error.message ?: "Failed to load deworming",
                        ),
                    )
                }
        }
    }

    /**
     * Updates the [DewormingFormState.product].
     */
    fun onProductChange(value: String) {
        formState.value?.let { updateForm(it.copy(product = value, productError = null)) }
    }

    /**
     * Updates the [DewormingFormState.dateAdministered].
     */
    fun onDateAdministeredChange(value: String) {
        formState.value?.let { updateForm(it.copy(dateAdministered = value, dateError = null)) }
    }

    /**
     * Updates the [DewormingFormState.nextDueDate].
     */
    fun onNextDueDateChange(value: String) {
        formState.value?.let { updateForm(it.copy(nextDueDate = value.ifBlank { null }, nextDueDateError = null)) }
    }

    /**
     * Updates the [DewormingFormState.dose].
     */
    fun onDoseChange(value: String) {
        formState.value?.let { updateForm(it.copy(dose = value.ifBlank { null })) }
    }

    /**
     * Updates the [DewormingFormState.vetName].
     */
    fun onVetNameChange(value: String) {
        formState.value?.let { updateForm(it.copy(vetName = value.ifBlank { null })) }
    }

    /**
     * Updates the [DewormingFormState.notes].
     */
    fun onNotesChange(value: String) {
        formState.value?.let { updateForm(it.copy(notes = value.ifBlank { null })) }
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

    private fun validate(form: DewormingFormState): DewormingFormState? {
        var invalid: DewormingFormState? = null
        if (form.product.isBlank()) {
            invalid = form.copy(productError = "Product is required")
        } else {
            val dateAdministered = parseDateOrNull(form.dateAdministered)
            if (dateAdministered == null) {
                val message =
                    if (form.dateAdministered.isBlank()) "Date is required" else "Invalid date (YYYY-MM-DD)"
                invalid = form.copy(dateError = message)
            } else {
                val nextDueDate = parseDateOrNull(form.nextDueDate)
                if (form.nextDueDate != null && nextDueDate == null) {
                    invalid = form.copy(nextDueDateError = "Invalid date (YYYY-MM-DD)")
                }
            }
        }
        return invalid
    }

    private fun persist(form: DewormingFormState) {
        val dateAdministered = parseDateOrNull(form.dateAdministered) ?: return
        val nextDueDate = parseDateOrNull(form.nextDueDate)
        viewModelScope.launch {
            updateForm(form.copy(isSaving = true))
            val now = Clock.System.now()
            val deworming =
                Deworming(
                    id = form.id ?: 0L,
                    patientId = patientId,
                    product = form.product,
                    dateAdministered = dateAdministered,
                    nextDueDate = nextDueDate,
                    dose = form.dose,
                    vetName = form.vetName,
                    notes = form.notes,
                    createdAt = form.createdAt ?: now,
                    updatedAt = now,
                )
            runCatching { withContext(ioDispatcher) { saveDewormingUseCase(deworming) } }
                .onSuccess {
                    formState.value?.let { updateForm(it.copy(isSaving = false)) }
                    emitSaved()
                }.onFailure { error ->
                    formState.value?.let {
                        updateForm(
                            it.copy(
                                isSaving = false,
                                dateError = error.message ?: "Failed to save deworming",
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
