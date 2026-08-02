package com.github.rodrigotimoteo.animally.presentation.substance

import androidx.lifecycle.viewModelScope
import com.github.rodrigotimoteo.animally.di.dispatchers.IO_DISPATCHER
import com.github.rodrigotimoteo.animally.domain.substance.model.ControlledSubstance
import com.github.rodrigotimoteo.animally.domain.substance.usecase.GetControlledSubstanceDetailUseCase
import com.github.rodrigotimoteo.animally.domain.substance.usecase.SaveControlledSubstanceUseCase
import com.github.rodrigotimoteo.animally.presentation.common.addEdit.BaseAddEditViewModel
import com.github.rodrigotimoteo.animally.presentation.navigation.AnimallyNavigator
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import org.koin.core.annotation.Named
import kotlin.time.Clock

/**
 * View model for the controlled-substance add/edit form.
 *
 * @param patientId The id of the patient this controlled-substance record belongs to.
 * @param substanceId The id of the record being edited, or `null` when creating a new one.
 * @param getControlledSubstanceDetailUseCase Use case for loading an existing record.
 * @param saveControlledSubstanceUseCase Use case for persisting the record.
 * @param animallyNavigator The navigator to use for navigation.
 * @param ioDispatcher Dispatcher for blocking database work.
 */
class ControlledSubstanceEditViewModel(
    private val patientId: Long,
    private val substanceId: Long?,
    private val getControlledSubstanceDetailUseCase: GetControlledSubstanceDetailUseCase,
    private val saveControlledSubstanceUseCase: SaveControlledSubstanceUseCase,
    animallyNavigator: AnimallyNavigator,
    @Named(IO_DISPATCHER) private val ioDispatcher: CoroutineDispatcher,
) : BaseAddEditViewModel<ControlledSubstanceFormState>(animallyNavigator) {
    init {
        if (substanceId != null) {
            loadSubstance(substanceId)
        } else {
            updateForm(ControlledSubstanceFormState())
        }
    }

    private fun loadSubstance(id: Long) {
        updateForm(ControlledSubstanceFormState(id = id, isLoading = true))
        viewModelScope.launch {
            runCatching { withContext(ioDispatcher) { getControlledSubstanceDetailUseCase(id) } }
                .onSuccess { substance ->
                    if (substance == null) {
                        updateForm(ControlledSubstanceFormState(id = id, drugNameError = "Record not found"))
                    } else {
                        updateForm(
                            ControlledSubstanceFormState(
                                id = substance.id,
                                drugName = substance.drugName,
                                dose = substance.dose,
                                unit = substance.unit,
                                route = substance.route,
                                administeredBy = substance.administeredBy,
                                witness = substance.witness,
                                date = substance.date.toString(),
                                reason = substance.reason,
                                notes = substance.notes,
                                createdAt = substance.createdAt,
                            ),
                        )
                    }
                }.onFailure { error ->
                    updateForm(
                        ControlledSubstanceFormState(
                            id = id,
                            drugNameError = error.message ?: "Failed to load record",
                        ),
                    )
                }
        }
    }

    /**
     * Updates the [ControlledSubstanceFormState.drugName].
     */
    fun onDrugNameChange(value: String) {
        formState.value?.let { updateForm(it.copy(drugName = value, drugNameError = null)) }
    }

    /**
     * Updates the [ControlledSubstanceFormState.dose].
     */
    fun onDoseChange(value: String) {
        formState.value?.let { updateForm(it.copy(dose = value, doseError = null)) }
    }

    /**
     * Updates the [ControlledSubstanceFormState.unit].
     */
    fun onUnitChange(value: String) {
        formState.value?.let { updateForm(it.copy(unit = value.ifBlank { null })) }
    }

    /**
     * Updates the [ControlledSubstanceFormState.route].
     */
    fun onRouteChange(value: String) {
        formState.value?.let { updateForm(it.copy(route = value.ifBlank { null })) }
    }

    /**
     * Updates the [ControlledSubstanceFormState.administeredBy].
     */
    fun onAdministeredByChange(value: String) {
        formState.value?.let { updateForm(it.copy(administeredBy = value.ifBlank { null })) }
    }

    /**
     * Updates the [ControlledSubstanceFormState.witness].
     */
    fun onWitnessChange(value: String) {
        formState.value?.let { updateForm(it.copy(witness = value.ifBlank { null })) }
    }

    /**
     * Updates the [ControlledSubstanceFormState.date].
     */
    fun onDateChange(value: String) {
        formState.value?.let { updateForm(it.copy(date = value, dateError = null)) }
    }

    /**
     * Updates the [ControlledSubstanceFormState.reason].
     */
    fun onReasonChange(value: String) {
        formState.value?.let { updateForm(it.copy(reason = value.ifBlank { null })) }
    }

    /**
     * Updates the [ControlledSubstanceFormState.notes].
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

    private fun validate(form: ControlledSubstanceFormState): ControlledSubstanceFormState? {
        var invalid: ControlledSubstanceFormState? = null
        if (form.drugName.isBlank()) {
            invalid = form.copy(drugNameError = "Drug name is required")
        } else if (form.dose.isBlank()) {
            invalid = form.copy(doseError = "Dose is required")
        } else {
            val date = parseDateOrNull(form.date)
            if (date == null) {
                val message = if (form.date.isBlank()) "Date is required" else "Invalid date (YYYY-MM-DD)"
                invalid = form.copy(dateError = message)
            }
        }
        return invalid
    }

    private fun persist(form: ControlledSubstanceFormState) {
        val date = parseDateOrNull(form.date) ?: return
        viewModelScope.launch {
            updateForm(form.copy(isSaving = true))
            val now = Clock.System.now()
            val substance =
                ControlledSubstance(
                    id = form.id ?: 0L,
                    patientId = patientId,
                    drugName = form.drugName,
                    dose = form.dose,
                    unit = form.unit,
                    route = form.route,
                    administeredBy = form.administeredBy,
                    witness = form.witness,
                    date = date,
                    reason = form.reason,
                    notes = form.notes,
                    createdAt = form.createdAt ?: now,
                    updatedAt = now,
                )
            runCatching { withContext(ioDispatcher) { saveControlledSubstanceUseCase(substance) } }
                .onSuccess {
                    formState.value?.let { updateForm(it.copy(isSaving = false)) }
                    popBackStack()
                }.onFailure { error ->
                    formState.value?.let {
                        updateForm(
                            it.copy(
                                isSaving = false,
                                drugNameError = error.message ?: "Failed to save record",
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
