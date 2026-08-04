package com.github.rodrigotimoteo.animally.presentation.vaccination

import androidx.lifecycle.viewModelScope
import com.github.rodrigotimoteo.animally.di.dispatchers.IO_DISPATCHER
import com.github.rodrigotimoteo.animally.domain.vaccination.model.Vaccination
import com.github.rodrigotimoteo.animally.domain.vaccination.usecase.GetVaccinationDetailUseCase
import com.github.rodrigotimoteo.animally.domain.vaccination.usecase.SaveVaccinationUseCase
import com.github.rodrigotimoteo.animally.presentation.common.addEdit.BaseAddEditViewModel
import com.github.rodrigotimoteo.animally.presentation.navigation.AnimallyNavigator
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import org.koin.core.annotation.Named
import kotlin.time.Clock

/**
 * View model for the vaccination add/edit form.
 *
 * @param patientId The id of the patient this vaccination belongs to.
 * @param vaccinationId The id of the vaccination being edited, or `null` when creating a new one.
 * @param getVaccinationDetailUseCase Use case for loading an existing vaccination.
 * @param saveVaccinationUseCase Use case for persisting the vaccination.
 * @param animallyNavigator The navigator to use for navigation.
 * @param ioDispatcher Dispatcher for blocking database work.
 */
class VaccinationEditViewModel(
    private val patientId: Long,
    private val vaccinationId: Long?,
    private val getVaccinationDetailUseCase: GetVaccinationDetailUseCase,
    private val saveVaccinationUseCase: SaveVaccinationUseCase,
    animallyNavigator: AnimallyNavigator,
    @Named(IO_DISPATCHER) private val ioDispatcher: CoroutineDispatcher,
) : BaseAddEditViewModel<VaccinationFormState>(animallyNavigator) {
    init {
        if (vaccinationId != null) {
            loadVaccination(vaccinationId)
        } else {
            updateForm(VaccinationFormState())
        }
    }

    private fun loadVaccination(id: Long) {
        updateForm(VaccinationFormState(id = id, isLoading = true))
        viewModelScope.launch {
            runCatching { withContext(ioDispatcher) { getVaccinationDetailUseCase(id) } }
                .onSuccess { vaccination ->
                    if (vaccination == null) {
                        updateForm(VaccinationFormState(id = id, vaccineNameError = "Vaccination not found"))
                    } else {
                        updateForm(
                            VaccinationFormState(
                                id = vaccination.id,
                                vaccineName = vaccination.vaccineName,
                                dateAdministered = vaccination.dateAdministered.toString(),
                                vetName = vaccination.vetName,
                                batchNumber = vaccination.batchNumber,
                                site = vaccination.site,
                                notes = vaccination.notes,
                                nextDueDate = vaccination.nextDueDate?.toString(),
                                createdAt = vaccination.createdAt,
                            ),
                        )
                    }
                }.onFailure { error ->
                    updateForm(
                        VaccinationFormState(
                            id = id,
                            vaccineNameError = error.message ?: "Failed to load vaccination",
                        ),
                    )
                }
        }
    }

    /**
     * Updates the [VaccinationFormState.vaccineName].
     */
    fun onVaccineNameChange(value: String) {
        formState.value?.let { updateForm(it.copy(vaccineName = value, vaccineNameError = null)) }
    }

    /**
     * Updates the [VaccinationFormState.dateAdministered].
     */
    fun onDateAdministeredChange(value: String) {
        formState.value?.let { updateForm(it.copy(dateAdministered = value, dateError = null)) }
    }

    /**
     * Updates the [VaccinationFormState.vetName].
     */
    fun onVetNameChange(value: String) {
        formState.value?.let { updateForm(it.copy(vetName = value.ifBlank { null })) }
    }

    /**
     * Updates the [VaccinationFormState.batchNumber].
     */
    fun onBatchNumberChange(value: String) {
        formState.value?.let { updateForm(it.copy(batchNumber = value.ifBlank { null })) }
    }

    /**
     * Updates the [VaccinationFormState.site].
     */
    fun onSiteChange(value: String) {
        formState.value?.let { updateForm(it.copy(site = value.ifBlank { null })) }
    }

    /**
     * Updates the [VaccinationFormState.notes].
     */
    fun onNotesChange(value: String) {
        formState.value?.let { updateForm(it.copy(notes = value.ifBlank { null })) }
    }

    /**
     * Validates and persists the current form.
     */
    override fun save() {
        val form = formState.value ?: return
        if (form.vaccineName.isBlank()) {
            updateForm(form.copy(vaccineNameError = "Vaccine name is required"))
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
            val vaccination =
                Vaccination(
                    id = form.id ?: 0L,
                    patientId = patientId,
                    vaccineName = form.vaccineName.trim(),
                    dateAdministered = date,
                    nextDueDate = null,
                    vetName = form.vetName,
                    batchNumber = form.batchNumber,
                    site = form.site,
                    notes = form.notes,
                    createdAt = form.createdAt ?: now,
                    updatedAt = now,
                )
            runCatching { withContext(ioDispatcher) { saveVaccinationUseCase(vaccination) } }
                .onSuccess {
                    formState.value?.let { updateForm(it.copy(isSaving = false)) }
                    emitSaved()
                }.onFailure { error ->
                    formState.value?.let {
                        updateForm(
                            it.copy(
                                isSaving = false,
                                vaccineNameError = error.message ?: "Failed to save vaccination",
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
