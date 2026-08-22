package com.github.rodrigotimoteo.animally.presentation.icsi

import androidx.lifecycle.viewModelScope
import com.github.rodrigotimoteo.animally.di.dispatchers.IO_DISPATCHER
import com.github.rodrigotimoteo.animally.domain.icsi.model.Icsi
import com.github.rodrigotimoteo.animally.domain.icsi.usecase.GetIcsiDetailUseCase
import com.github.rodrigotimoteo.animally.domain.icsi.usecase.SaveIcsiUseCase
import com.github.rodrigotimoteo.animally.presentation.common.addEdit.BaseAddEditViewModel
import com.github.rodrigotimoteo.animally.presentation.navigation.AnimallyNavigator
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import org.koin.core.annotation.Named
import kotlin.time.Clock

/**
 * View model for the ICSI add/edit form.
 *
 * @param patientId The id of the patient this record belongs to.
 * @param icsiId The id of the record being edited, or `null` when creating.
 * @param getIcsiDetailUseCase Use case for loading an existing record.
 * @param saveIcsiUseCase Use case for persisting the record.
 * @param animallyNavigator The navigator to use for navigation.
 * @param ioDispatcher Dispatcher for blocking database work.
 */
class IcsiEditViewModel(
    private val patientId: Long,
    private val icsiId: Long?,
    private val getIcsiDetailUseCase: GetIcsiDetailUseCase,
    private val saveIcsiUseCase: SaveIcsiUseCase,
    animallyNavigator: AnimallyNavigator,
    @Named(IO_DISPATCHER) private val ioDispatcher: CoroutineDispatcher,
) : BaseAddEditViewModel<IcsiFormState>(animallyNavigator) {
    init {
        if (icsiId != null) {
            loadRecord(icsiId)
        } else {
            updateForm(IcsiFormState())
        }
    }

    private fun loadRecord(id: Long) {
        updateForm(IcsiFormState(id = id, isLoading = true))
        viewModelScope.launch {
            runCatching { withContext(ioDispatcher) { getIcsiDetailUseCase(id) } }
                .onSuccess { record ->
                    if (record == null) {
                        updateForm(IcsiFormState(id = id, dateError = "Record not found"))
                    } else {
                        updateForm(
                            IcsiFormState(
                                id = record.id,
                                date = record.date.toString(),
                                folliclesRecovered = record.folliclesRecovered.toString(),
                                vetName = record.vetName,
                                notes = record.notes,
                                createdAt = record.createdAt,
                            ),
                        )
                    }
                }.onFailure { error ->
                    updateForm(
                        IcsiFormState(
                            id = id,
                            dateError = error.message ?: "Failed to load record",
                        ),
                    )
                }
        }
    }

    /**
     * Updates the [IcsiFormState.date].
     */
    fun onDateChange(value: String) {
        formState.value?.let { updateForm(it.copy(date = value, dateError = null)) }
    }

    /**
     * Updates the [IcsiFormState.folliclesRecovered].
     */
    fun onFolliclesRecoveredChange(value: String) {
        formState.value?.let { updateForm(it.copy(folliclesRecovered = value.filter { c -> c.isDigit() })) }
    }

    /**
     * Updates the [IcsiFormState.vetName].
     */
    fun onVetNameChange(value: String) {
        formState.value?.let { updateForm(it.copy(vetName = value.ifBlank { null })) }
    }

    /**
     * Updates the [IcsiFormState.notes].
     */
    fun onNotesChange(value: String) {
        formState.value?.let { updateForm(it.copy(notes = value.ifBlank { null })) }
    }

    override fun save() {
        val current = formState.value ?: return
        val date = runCatching { LocalDate.parse(current.date) }.getOrNull()
        if (date == null) {
            updateForm(current.copy(dateError = "Date must be yyyy-MM-dd"))
            return
        }
        updateForm(current.copy(isSaving = true))
        viewModelScope.launch {
            runCatching {
                withContext(ioDispatcher) {
                    saveIcsiUseCase(
                        Icsi(
                            id = current.id ?: 0L,
                            patientId = patientId,
                            date = date,
                            folliclesRecovered = current.folliclesRecovered.toIntOrNull() ?: 0,
                            vetName = current.vetName,
                            notes = current.notes,
                            createdAt = current.createdAt ?: Clock.System.now(),
                            updatedAt = Clock.System.now(),
                        ),
                    )
                }
            }.onSuccess {
                emitSaved()
            }.onFailure { error ->
                updateForm(current.copy(isSaving = false, dateError = error.message))
            }
        }
    }
}
