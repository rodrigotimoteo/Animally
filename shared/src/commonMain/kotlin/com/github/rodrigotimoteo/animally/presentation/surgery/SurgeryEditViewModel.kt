package com.github.rodrigotimoteo.animally.presentation.surgery

import androidx.lifecycle.viewModelScope
import com.github.rodrigotimoteo.animally.di.dispatchers.IO_DISPATCHER
import com.github.rodrigotimoteo.animally.domain.surgery.model.Surgery
import com.github.rodrigotimoteo.animally.domain.surgery.usecase.GetSurgeryDetailUseCase
import com.github.rodrigotimoteo.animally.domain.surgery.usecase.SaveSurgeryUseCase
import com.github.rodrigotimoteo.animally.presentation.common.addEdit.BaseAddEditViewModel
import com.github.rodrigotimoteo.animally.presentation.navigation.AnimallyNavigator
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import org.koin.core.annotation.Named
import kotlin.time.Clock

/**
 * View model for the surgery add/edit form.
 *
 * @param patientId The id of the patient this surgery belongs to.
 * @param surgeryId The id of the surgery being edited, or `null` when creating a new one.
 * @param getSurgeryDetailUseCase Use case for loading an existing surgery.
 * @param saveSurgeryUseCase Use case for persisting the surgery.
 * @param animallyNavigator The navigator to use for navigation.
 * @param ioDispatcher Dispatcher for blocking database work.
 */
class SurgeryEditViewModel(
    private val patientId: Long,
    private val surgeryId: Long?,
    private val getSurgeryDetailUseCase: GetSurgeryDetailUseCase,
    private val saveSurgeryUseCase: SaveSurgeryUseCase,
    animallyNavigator: AnimallyNavigator,
    @Named(IO_DISPATCHER) private val ioDispatcher: CoroutineDispatcher,
) : BaseAddEditViewModel<SurgeryFormState>(animallyNavigator) {
    init {
        if (surgeryId != null) {
            loadSurgery(surgeryId)
        } else {
            updateForm(SurgeryFormState())
        }
    }

    private fun loadSurgery(id: Long) {
        updateForm(SurgeryFormState(id = id, isLoading = true))
        viewModelScope.launch {
            runCatching { withContext(ioDispatcher) { getSurgeryDetailUseCase(id) } }
                .onSuccess { surgery ->
                    if (surgery == null) {
                        updateForm(SurgeryFormState(id = id, dateError = "Surgery not found"))
                    } else {
                        updateForm(
                            SurgeryFormState(
                                id = surgery.id,
                                date = surgery.date.toString(),
                                type = surgery.type,
                                description = surgery.description,
                                outcome = surgery.outcome,
                                surgeon = surgery.surgeon,
                                anesthesia = surgery.anesthesia,
                                analgesia = surgery.analgesia,
                                complications = surgery.complications,
                                recoveryNotes = surgery.recoveryNotes,
                                createdAt = surgery.createdAt,
                            ),
                        )
                    }
                }.onFailure { error ->
                    updateForm(
                        SurgeryFormState(
                            id = id,
                            dateError = error.message ?: "Failed to load surgery",
                        ),
                    )
                }
        }
    }

    /**
     * Updates the [SurgeryFormState.date].
     */
    fun onDateChange(date: String) {
        formState.value?.let { updateForm(it.copy(date = date, dateError = null)) }
    }

    /**
     * Updates the [SurgeryFormState.type].
     */
    fun onTypeChange(value: String) {
        formState.value?.let { updateForm(it.copy(type = value.ifBlank { null })) }
    }

    /**
     * Updates the [SurgeryFormState.description].
     */
    fun onDescriptionChange(value: String) {
        formState.value?.let { updateForm(it.copy(description = value.ifBlank { null })) }
    }

    /**
     * Updates the [SurgeryFormState.outcome].
     */
    fun onOutcomeChange(value: String) {
        formState.value?.let { updateForm(it.copy(outcome = value.ifBlank { null })) }
    }

    /**
     * Updates the [SurgeryFormState.surgeon].
     */
    fun onSurgeonChange(value: String) {
        formState.value?.let { updateForm(it.copy(surgeon = value.ifBlank { null })) }
    }

    /**
     * Updates the [SurgeryFormState.anesthesia].
     */
    fun onAnesthesiaChange(value: String) {
        formState.value?.let { updateForm(it.copy(anesthesia = value.ifBlank { null })) }
    }

    /**
     * Updates the [SurgeryFormState.analgesia].
     */
    fun onAnalgesiaChange(value: String) {
        formState.value?.let { updateForm(it.copy(analgesia = value.ifBlank { null })) }
    }

    /**
     * Updates the [SurgeryFormState.complications].
     */
    fun onComplicationsChange(value: String) {
        formState.value?.let { updateForm(it.copy(complications = value.ifBlank { null })) }
    }

    /**
     * Updates the [SurgeryFormState.recoveryNotes].
     */
    fun onRecoveryNotesChange(value: String) {
        formState.value?.let { updateForm(it.copy(recoveryNotes = value.ifBlank { null })) }
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
        viewModelScope.launch {
            updateForm(form.copy(isSaving = true))
            val now = Clock.System.now()
            val surgery =
                Surgery(
                    id = form.id ?: 0L,
                    patientId = patientId,
                    date = date,
                    type = form.type,
                    description = form.description,
                    outcome = form.outcome,
                    surgeon = form.surgeon,
                    anesthesia = form.anesthesia,
                    analgesia = form.analgesia,
                    complications = form.complications,
                    recoveryNotes = form.recoveryNotes,
                    createdAt = form.createdAt ?: now,
                    updatedAt = now,
                )
            runCatching { withContext(ioDispatcher) { saveSurgeryUseCase(surgery) } }
                .onSuccess {
                    formState.value?.let { updateForm(it.copy(isSaving = false)) }
                    emitSaved()
                }.onFailure { error ->
                    formState.value?.let {
                        updateForm(
                            it.copy(
                                isSaving = false,
                                dateError = error.message ?: "Failed to save surgery",
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
