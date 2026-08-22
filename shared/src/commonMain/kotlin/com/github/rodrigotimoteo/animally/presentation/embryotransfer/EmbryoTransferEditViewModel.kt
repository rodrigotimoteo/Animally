package com.github.rodrigotimoteo.animally.presentation.embryotransfer

import androidx.lifecycle.viewModelScope
import com.github.rodrigotimoteo.animally.di.dispatchers.IO_DISPATCHER
import com.github.rodrigotimoteo.animally.domain.embryotransfer.model.EmbryoTransfer
import com.github.rodrigotimoteo.animally.domain.embryotransfer.usecase.GetEmbryoTransferDetailUseCase
import com.github.rodrigotimoteo.animally.domain.embryotransfer.usecase.SaveEmbryoTransferUseCase
import com.github.rodrigotimoteo.animally.presentation.common.addEdit.BaseAddEditViewModel
import com.github.rodrigotimoteo.animally.presentation.navigation.AnimallyNavigator
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import org.koin.core.annotation.Named
import kotlin.time.Clock

/**
 * View model for the embryo transfer add/edit form.
 *
 * @param patientId The id of the patient this record belongs to.
 * @param embryoTransferId The id of the record being edited, or `null` when creating.
 * @param getEmbryoTransferDetailUseCase Use case for loading an existing record.
 * @param saveEmbryoTransferUseCase Use case for persisting the record.
 * @param animallyNavigator The navigator to use for navigation.
 * @param ioDispatcher Dispatcher for blocking database work.
 */
class EmbryoTransferEditViewModel(
    private val patientId: Long,
    private val embryoTransferId: Long?,
    private val getEmbryoTransferDetailUseCase: GetEmbryoTransferDetailUseCase,
    private val saveEmbryoTransferUseCase: SaveEmbryoTransferUseCase,
    animallyNavigator: AnimallyNavigator,
    @Named(IO_DISPATCHER) private val ioDispatcher: CoroutineDispatcher,
) : BaseAddEditViewModel<EmbryoTransferFormState>(animallyNavigator) {
    init {
        if (embryoTransferId != null) {
            loadRecord(embryoTransferId)
        } else {
            updateForm(EmbryoTransferFormState())
        }
    }

    private fun loadRecord(id: Long) {
        updateForm(EmbryoTransferFormState(id = id, isLoading = true))
        viewModelScope.launch {
            runCatching { withContext(ioDispatcher) { getEmbryoTransferDetailUseCase(id) } }
                .onSuccess { record ->
                    if (record == null) {
                        updateForm(EmbryoTransferFormState(id = id, dateError = "Record not found"))
                    } else {
                        updateForm(
                            EmbryoTransferFormState(
                                id = record.id,
                                date = record.date.toString(),
                                embryoCount = record.embryoCount.toString(),
                                recipientMares = record.recipientMares,
                                vetName = record.vetName,
                                notes = record.notes,
                                createdAt = record.createdAt,
                            ),
                        )
                    }
                }.onFailure { error ->
                    updateForm(
                        EmbryoTransferFormState(
                            id = id,
                            dateError = error.message ?: "Failed to load record",
                        ),
                    )
                }
        }
    }

    /**
     * Updates the [EmbryoTransferFormState.date].
     */
    fun onDateChange(value: String) {
        formState.value?.let { updateForm(it.copy(date = value, dateError = null)) }
    }

    /**
     * Updates the [EmbryoTransferFormState.embryoCount].
     */
    fun onEmbryoCountChange(value: String) {
        formState.value?.let { updateForm(it.copy(embryoCount = value.filter { c -> c.isDigit() })) }
    }

    /**
     * Updates the [EmbryoTransferFormState.recipientMares].
     */
    fun onRecipientMaresChange(value: String) {
        formState.value?.let { updateForm(it.copy(recipientMares = value.ifBlank { null })) }
    }

    /**
     * Updates the [EmbryoTransferFormState.vetName].
     */
    fun onVetNameChange(value: String) {
        formState.value?.let { updateForm(it.copy(vetName = value.ifBlank { null })) }
    }

    /**
     * Updates the [EmbryoTransferFormState.notes].
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
                    saveEmbryoTransferUseCase(
                        EmbryoTransfer(
                            id = current.id ?: 0L,
                            patientId = patientId,
                            date = date,
                            embryoCount = current.embryoCount.toIntOrNull() ?: 0,
                            recipientMares = current.recipientMares,
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
