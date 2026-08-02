package com.github.rodrigotimoteo.animally.presentation.medication

import androidx.lifecycle.viewModelScope
import com.github.rodrigotimoteo.animally.di.dispatchers.IO_DISPATCHER
import com.github.rodrigotimoteo.animally.domain.medication.model.Medication
import com.github.rodrigotimoteo.animally.domain.medication.usecase.GetMedicationDetailUseCase
import com.github.rodrigotimoteo.animally.domain.medication.usecase.SaveMedicationUseCase
import com.github.rodrigotimoteo.animally.presentation.common.addEdit.BaseAddEditViewModel
import com.github.rodrigotimoteo.animally.presentation.navigation.AnimallyNavigator
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import org.koin.core.annotation.Named
import kotlin.time.Clock

/**
 * View model for the medication add/edit form.
 *
 * @param patientId The id of the patient this medication belongs to.
 * @param medicationId The id of the medication being edited, or `null` when creating a new one.
 * @param getMedicationDetailUseCase Use case for loading an existing medication.
 * @param saveMedicationUseCase Use case for persisting the medication.
 * @param animallyNavigator The navigator to use for navigation.
 * @param ioDispatcher Dispatcher for blocking database work.
 */
class MedicationEditViewModel(
    private val patientId: Long,
    private val medicationId: Long?,
    private val getMedicationDetailUseCase: GetMedicationDetailUseCase,
    private val saveMedicationUseCase: SaveMedicationUseCase,
    animallyNavigator: AnimallyNavigator,
    @Named(IO_DISPATCHER) private val ioDispatcher: CoroutineDispatcher,
) : BaseAddEditViewModel<MedicationFormState>(animallyNavigator) {
    init {
        if (medicationId != null) {
            loadMedication(medicationId)
        } else {
            updateForm(MedicationFormState())
        }
    }

    private fun loadMedication(id: Long) {
        updateForm(MedicationFormState(id = id, isLoading = true))
        viewModelScope.launch {
            runCatching { withContext(ioDispatcher) { getMedicationDetailUseCase(id) } }
                .onSuccess { medication ->
                    if (medication == null) {
                        updateForm(MedicationFormState(id = id, nameError = "Medication not found"))
                    } else {
                        updateForm(
                            MedicationFormState(
                                id = medication.id,
                                name = medication.name,
                                dosage = medication.dosage,
                                route = medication.route,
                                frequency = medication.frequency,
                                startDate = medication.startDate?.toString(),
                                endDate = medication.endDate?.toString(),
                                prescribedBy = medication.prescribedBy,
                                notes = medication.notes,
                                createdAt = medication.createdAt,
                            ),
                        )
                    }
                }.onFailure { error ->
                    updateForm(
                        MedicationFormState(
                            id = id,
                            nameError = error.message ?: "Failed to load medication",
                        ),
                    )
                }
        }
    }

    /**
     * Updates the [MedicationFormState.name].
     */
    fun onNameChange(value: String) {
        formState.value?.let { updateForm(it.copy(name = value, nameError = null)) }
    }

    /**
     * Updates the [MedicationFormState.dosage].
     */
    fun onDosageChange(value: String) {
        formState.value?.let { updateForm(it.copy(dosage = value, dosageError = null)) }
    }

    /**
     * Updates the [MedicationFormState.route].
     */
    fun onRouteChange(value: String) {
        formState.value?.let { updateForm(it.copy(route = value.ifBlank { null })) }
    }

    /**
     * Updates the [MedicationFormState.frequency].
     */
    fun onFrequencyChange(value: String) {
        formState.value?.let { updateForm(it.copy(frequency = value.ifBlank { null })) }
    }

    /**
     * Updates the [MedicationFormState.startDate].
     */
    fun onStartDateChange(value: String) {
        formState.value?.let { updateForm(it.copy(startDate = value.ifBlank { null }, startDateError = null)) }
    }

    /**
     * Updates the [MedicationFormState.endDate].
     */
    fun onEndDateChange(value: String) {
        formState.value?.let { updateForm(it.copy(endDate = value.ifBlank { null }, endDateError = null)) }
    }

    /**
     * Updates the [MedicationFormState.prescribedBy].
     */
    fun onPrescribedByChange(value: String) {
        formState.value?.let { updateForm(it.copy(prescribedBy = value.ifBlank { null })) }
    }

    /**
     * Updates the [MedicationFormState.notes].
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

    private fun validate(form: MedicationFormState): MedicationFormState? {
        var invalid: MedicationFormState? = null
        if (form.name.isBlank()) {
            invalid = form.copy(nameError = "Name is required")
        } else if (form.dosage.isBlank()) {
            invalid = form.copy(dosageError = "Dosage is required")
        } else {
            val startDate = parseDateOrNull(form.startDate)
            if (form.startDate != null && startDate == null) {
                invalid = form.copy(startDateError = "Invalid date (YYYY-MM-DD)")
            } else {
                val endDate = parseDateOrNull(form.endDate)
                if (form.endDate != null && endDate == null) {
                    invalid = form.copy(endDateError = "Invalid date (YYYY-MM-DD)")
                }
            }
        }
        return invalid
    }

    private fun persist(form: MedicationFormState) {
        val startDate = parseDateOrNull(form.startDate)
        val endDate = parseDateOrNull(form.endDate)
        viewModelScope.launch {
            updateForm(form.copy(isSaving = true))
            val now = Clock.System.now()
            val medication =
                Medication(
                    id = form.id ?: 0L,
                    patientId = patientId,
                    name = form.name,
                    dosage = form.dosage,
                    route = form.route,
                    frequency = form.frequency,
                    startDate = startDate,
                    endDate = endDate,
                    prescribedBy = form.prescribedBy,
                    notes = form.notes,
                    createdAt = form.createdAt ?: now,
                    updatedAt = now,
                )
            runCatching { withContext(ioDispatcher) { saveMedicationUseCase(medication) } }
                .onSuccess {
                    formState.value?.let { updateForm(it.copy(isSaving = false)) }
                    popBackStack()
                }.onFailure { error ->
                    formState.value?.let {
                        updateForm(
                            it.copy(
                                isSaving = false,
                                nameError = error.message ?: "Failed to save medication",
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
