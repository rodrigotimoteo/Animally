package com.github.rodrigotimoteo.animally.presentation.customreminder

import androidx.lifecycle.viewModelScope
import com.github.rodrigotimoteo.animally.di.dispatchers.IO_DISPATCHER
import com.github.rodrigotimoteo.animally.domain.customreminder.model.CustomReminder
import com.github.rodrigotimoteo.animally.domain.customreminder.usecase.GetCustomReminderDetailUseCase
import com.github.rodrigotimoteo.animally.domain.customreminder.usecase.SaveCustomReminderUseCase
import com.github.rodrigotimoteo.animally.presentation.common.addEdit.BaseAddEditViewModel
import com.github.rodrigotimoteo.animally.presentation.navigation.AnimallyNavigator
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import org.koin.core.annotation.Named
import kotlin.time.Clock

/**
 * View model for the custom reminder add/edit form.
 *
 * @param patientId The id of the patient this custom reminder belongs to.
 * @param reminderId The id of the custom reminder being edited, or `null` when creating a new one.
 * @param getCustomReminderDetailUseCase Use case for loading an existing custom reminder.
 * @param saveCustomReminderUseCase Use case for persisting the custom reminder.
 * @param animallyNavigator The navigator to use for navigation.
 * @param ioDispatcher Dispatcher for blocking database work.
 */
class CustomReminderEditViewModel(
    private val patientId: Long,
    private val reminderId: Long?,
    private val getCustomReminderDetailUseCase: GetCustomReminderDetailUseCase,
    private val saveCustomReminderUseCase: SaveCustomReminderUseCase,
    animallyNavigator: AnimallyNavigator,
    @Named(IO_DISPATCHER) private val ioDispatcher: CoroutineDispatcher,
) : BaseAddEditViewModel<CustomReminderFormState>(animallyNavigator) {
    init {
        if (reminderId != null) {
            loadCustomReminder(reminderId)
        } else {
            updateForm(CustomReminderFormState())
        }
    }

    private fun loadCustomReminder(id: Long) {
        updateForm(CustomReminderFormState(id = id, isLoading = true))
        viewModelScope.launch {
            runCatching { withContext(ioDispatcher) { getCustomReminderDetailUseCase(id) } }
                .onSuccess { reminder ->
                    if (reminder == null) {
                        updateForm(CustomReminderFormState(id = id, titleError = "Reminder not found"))
                    } else {
                        updateForm(
                            CustomReminderFormState(
                                id = reminder.id,
                                title = reminder.title,
                                dueDate = reminder.dueDate.toString(),
                                linkedRecordType = reminder.linkedRecordType,
                                linkedRecordId = reminder.linkedRecordId?.toString(),
                                notes = reminder.notes,
                                createdAt = reminder.createdAt,
                            ),
                        )
                    }
                }.onFailure { error ->
                    updateForm(
                        CustomReminderFormState(
                            id = id,
                            titleError = error.message ?: "Failed to load reminder",
                        ),
                    )
                }
        }
    }

    /**
     * Updates the [CustomReminderFormState.title].
     */
    fun onTitleChange(value: String) {
        formState.value?.let { updateForm(it.copy(title = value, titleError = null)) }
    }

    /**
     * Updates the [CustomReminderFormState.dueDate].
     */
    fun onDueDateChange(value: String) {
        formState.value?.let { updateForm(it.copy(dueDate = value, dueDateError = null)) }
    }

    /**
     * Updates the [CustomReminderFormState.linkedRecordType].
     */
    fun onLinkedRecordTypeChange(value: String) {
        formState.value?.let { updateForm(it.copy(linkedRecordType = value.ifBlank { null })) }
    }

    /**
     * Updates the [CustomReminderFormState.linkedRecordId].
     */
    fun onLinkedRecordIdChange(value: String) {
        formState.value?.let {
            updateForm(it.copy(linkedRecordId = value.ifBlank { null }, linkedRecordIdError = null))
        }
    }

    /**
     * Updates the [CustomReminderFormState.notes].
     */
    fun onNotesChange(value: String) {
        formState.value?.let { updateForm(it.copy(notes = value.ifBlank { null })) }
    }

    /**
     * Validates and persists the current form.
     */
    override fun save() {
        val form = formState.value ?: return
        val title = form.title.trim()
        val dueDate = parseDateOrNull(form.dueDate)
        val linkedRecordId = form.linkedRecordId?.toLongOrNull()
        if (!validateForm(form, title, dueDate, linkedRecordId)) return
        viewModelScope.launch {
            updateForm(form.copy(isSaving = true))
            val now = Clock.System.now()
            val reminder =
                CustomReminder(
                    id = form.id ?: 0L,
                    patientId = patientId,
                    title = title,
                    dueDate = requireNotNull(dueDate),
                    linkedRecordType = form.linkedRecordType,
                    linkedRecordId = linkedRecordId,
                    notes = form.notes,
                    createdAt = form.createdAt ?: now,
                    updatedAt = now,
                )
            runCatching { withContext(ioDispatcher) { saveCustomReminderUseCase(reminder) } }
                .onSuccess {
                    formState.value?.let { updateForm(it.copy(isSaving = false)) }
                    popBackStack()
                }.onFailure { error ->
                    formState.value?.let {
                        updateForm(
                            it.copy(
                                isSaving = false,
                                titleError = error.message ?: "Failed to save reminder",
                            ),
                        )
                    }
                }
        }
    }

    private fun validateForm(
        form: CustomReminderFormState,
        title: String,
        dueDate: LocalDate?,
        linkedRecordId: Long?,
    ): Boolean =
        when {
            title.isBlank() -> {
                updateForm(form.copy(titleError = "Title is required"))
                false
            }
            dueDate == null -> {
                val message = if (form.dueDate.isBlank()) "Due date is required" else "Invalid date (YYYY-MM-DD)"
                updateForm(form.copy(dueDateError = message))
                false
            }
            form.linkedRecordId != null && linkedRecordId == null -> {
                updateForm(form.copy(linkedRecordIdError = "Linked record id must be a number"))
                false
            }
            else -> true
        }

    private fun parseDateOrNull(value: String?): LocalDate? {
        if (value == null) return null
        return runCatching { LocalDate.parse(value) }.getOrNull()
    }
}
