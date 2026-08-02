package com.github.rodrigotimoteo.animally.presentation.repromedication

import androidx.lifecycle.viewModelScope
import com.github.rodrigotimoteo.animally.di.dispatchers.IO_DISPATCHER
import com.github.rodrigotimoteo.animally.domain.repromedication.model.ReproMedication
import com.github.rodrigotimoteo.animally.domain.repromedication.usecase.GetReproMedicationDetailUseCase
import com.github.rodrigotimoteo.animally.domain.repromedication.usecase.SaveReproMedicationUseCase
import com.github.rodrigotimoteo.animally.presentation.common.addEdit.BaseAddEditViewModel
import com.github.rodrigotimoteo.animally.presentation.navigation.AnimallyNavigator
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import org.koin.core.annotation.Named
import kotlin.time.Clock

/**
 * View model for the reproduction-medication add/edit form.
 *
 * @param patientId The id of the patient this reproduction medication belongs to.
 * @param reproMedId The id of the reproduction medication being edited, or `null` when creating a new one.
 * @param getReproMedicationDetailUseCase Use case for loading an existing reproduction medication.
 * @param saveReproMedicationUseCase Use case for persisting the reproduction medication.
 * @param animallyNavigator The navigator to use for navigation.
 * @param ioDispatcher Dispatcher for blocking database work.
 */
class ReproMedicationEditViewModel(
    private val patientId: Long,
    private val reproMedId: Long?,
    private val getReproMedicationDetailUseCase: GetReproMedicationDetailUseCase,
    private val saveReproMedicationUseCase: SaveReproMedicationUseCase,
    animallyNavigator: AnimallyNavigator,
    @Named(IO_DISPATCHER) private val ioDispatcher: CoroutineDispatcher,
) : BaseAddEditViewModel<ReproMedicationFormState>(animallyNavigator) {
    init {
        if (reproMedId != null) {
            loadReproMedication(reproMedId)
        } else {
            updateForm(ReproMedicationFormState())
        }
    }

    private fun loadReproMedication(id: Long) {
        updateForm(ReproMedicationFormState(id = id, isLoading = true))
        viewModelScope.launch {
            runCatching { withContext(ioDispatcher) { getReproMedicationDetailUseCase(id) } }
                .onSuccess { medication ->
                    if (medication == null) {
                        updateForm(ReproMedicationFormState(id = id, medicationError = "Medication not found"))
                    } else {
                        updateForm(
                            ReproMedicationFormState(
                                id = medication.id,
                                medication = medication.medication,
                                dateAdministered = medication.dateAdministered.toString(),
                                dosage = medication.dosage,
                                purpose = medication.purpose,
                                vetName = medication.vetName,
                                notes = medication.notes,
                                createdAt = medication.createdAt,
                            ),
                        )
                    }
                }.onFailure { error ->
                    updateForm(
                        ReproMedicationFormState(
                            id = id,
                            medicationError = error.message ?: "Failed to load reproduction medication",
                        ),
                    )
                }
        }
    }

    /**
     * Updates the [ReproMedicationFormState.medication].
     */
    fun onMedicationChange(value: String) {
        formState.value?.let { updateForm(it.copy(medication = value, medicationError = null)) }
    }

    /**
     * Updates the [ReproMedicationFormState.dateAdministered].
     */
    fun onDateAdministeredChange(value: String) {
        formState.value?.let { updateForm(it.copy(dateAdministered = value, dateError = null)) }
    }

    /**
     * Updates the [ReproMedicationFormState.dosage].
     */
    fun onDosageChange(value: String) {
        formState.value?.let { updateForm(it.copy(dosage = value.ifBlank { null })) }
    }

    /**
     * Updates the [ReproMedicationFormState.purpose].
     */
    fun onPurposeChange(value: String) {
        formState.value?.let { updateForm(it.copy(purpose = value.ifBlank { null })) }
    }

    /**
     * Updates the [ReproMedicationFormState.vetName].
     */
    fun onVetNameChange(value: String) {
        formState.value?.let { updateForm(it.copy(vetName = value.ifBlank { null })) }
    }

    /**
     * Updates the [ReproMedicationFormState.notes].
     */
    fun onNotesChange(value: String) {
        formState.value?.let { updateForm(it.copy(notes = value.ifBlank { null })) }
    }

    /**
     * Validates and persists the current form.
     */
    override fun save() {
        val form = formState.value ?: return
        if (form.medication.isBlank()) {
            updateForm(form.copy(medicationError = "Medication is required"))
            return
        }
        val date = parseDateOrNull(form.dateAdministered)
        if (date == null) {
            val message = if (form.dateAdministered.isBlank()) "Date is required" else "Invalid date (YYYY-MM-DD)"
            updateForm(form.copy(dateError = message))
            return
        }
        viewModelScope.launch {
            updateForm(form.copy(isSaving = true))
            val now = Clock.System.now()
            val reproMedication =
                ReproMedication(
                    id = form.id ?: 0L,
                    patientId = patientId,
                    medication = form.medication.trim(),
                    dateAdministered = date,
                    dosage = form.dosage,
                    purpose = form.purpose,
                    vetName = form.vetName,
                    notes = form.notes,
                    createdAt = form.createdAt ?: now,
                    updatedAt = now,
                )
            runCatching { withContext(ioDispatcher) { saveReproMedicationUseCase(reproMedication) } }
                .onSuccess {
                    formState.value?.let { updateForm(it.copy(isSaving = false)) }
                    popBackStack()
                }.onFailure { error ->
                    formState.value?.let {
                        updateForm(
                            it.copy(
                                isSaving = false,
                                medicationError = error.message ?: "Failed to save reproduction medication",
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
