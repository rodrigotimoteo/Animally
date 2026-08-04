package com.github.rodrigotimoteo.animally.presentation.gestation

import androidx.lifecycle.viewModelScope
import com.github.rodrigotimoteo.animally.di.dispatchers.IO_DISPATCHER
import com.github.rodrigotimoteo.animally.domain.gestation.model.Gestation
import com.github.rodrigotimoteo.animally.domain.gestation.usecase.CalculateGestationUseCase
import com.github.rodrigotimoteo.animally.domain.gestation.usecase.GetGestationDetailUseCase
import com.github.rodrigotimoteo.animally.domain.gestation.usecase.SaveGestationUseCase
import com.github.rodrigotimoteo.animally.presentation.common.addEdit.BaseAddEditViewModel
import com.github.rodrigotimoteo.animally.presentation.navigation.AnimallyNavigator
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import org.koin.core.annotation.Named
import kotlin.time.Clock

/**
 * View model for the gestation add/edit form.
 *
 * @param patientId The id of the patient this gestation record belongs to.
 * @param gestationId The id of the gestation record being edited, or `null` when creating a new one.
 * @param getGestationDetailUseCase Use case for loading an existing gestation record.
 * @param saveGestationUseCase Use case for persisting the gestation record.
 * @param animallyNavigator The navigator to use for navigation.
 * @param ioDispatcher Dispatcher for blocking database work.
 */
class GestationEditViewModel(
    private val patientId: Long,
    private val gestationId: Long?,
    private val getGestationDetailUseCase: GetGestationDetailUseCase,
    private val saveGestationUseCase: SaveGestationUseCase,
    animallyNavigator: AnimallyNavigator,
    @Named(IO_DISPATCHER) private val ioDispatcher: CoroutineDispatcher,
) : BaseAddEditViewModel<GestationFormState>(animallyNavigator) {
    private val calculateGestationUseCase = CalculateGestationUseCase()

    init {
        if (gestationId != null) {
            loadGestation(gestationId)
        } else {
            updateForm(GestationFormState())
        }
    }

    private fun loadGestation(id: Long) {
        updateForm(GestationFormState(id = id, isLoading = true))
        viewModelScope.launch {
            runCatching { withContext(ioDispatcher) { getGestationDetailUseCase(id) } }
                .onSuccess { gestation ->
                    if (gestation == null) {
                        updateForm(GestationFormState(id = id, breedingDateError = "Gestation not found"))
                    } else {
                        updateForm(
                            GestationFormState(
                                id = gestation.id,
                                breedingDate = gestation.breedingDate.toString(),
                                status = gestation.status,
                                fetalCount = gestation.fetalCount?.toString(),
                                lastCheckDate = gestation.lastCheckDate?.toString(),
                                notes = gestation.notes,
                                createdAt = gestation.createdAt,
                            ),
                        )
                    }
                }.onFailure { error ->
                    updateForm(
                        GestationFormState(
                            id = id,
                            breedingDateError = error.message ?: "Failed to load gestation record",
                        ),
                    )
                }
        }
    }

    /**
     * Updates the [GestationFormState.breedingDate].
     */
    fun onBreedingDateChange(value: String) {
        formState.value?.let { updateForm(it.copy(breedingDate = value, breedingDateError = null)) }
    }

    /**
     * Updates the [GestationFormState.status].
     */
    fun onStatusChange(value: String) {
        formState.value?.let { updateForm(it.copy(status = value, statusError = null)) }
    }

    /**
     * Updates the [GestationFormState.fetalCount].
     */
    fun onFetalCountChange(value: String) {
        formState.value?.let { updateForm(it.copy(fetalCount = value.ifBlank { null }, fetalCountError = null)) }
    }

    /**
     * Updates the [GestationFormState.lastCheckDate].
     */
    fun onLastCheckDateChange(value: String) {
        formState.value?.let { updateForm(it.copy(lastCheckDate = value.ifBlank { null }, lastCheckDateError = null)) }
    }

    /**
     * Updates the [GestationFormState.notes].
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

    private fun validate(form: GestationFormState): GestationFormState? {
        var invalid: GestationFormState? = null
        val breedingDate = parseDateOrNull(form.breedingDate)
        if (breedingDate == null) {
            val message =
                if (form.breedingDate.isBlank()) "Breeding date is required" else "Invalid date (YYYY-MM-DD)"
            invalid = form.copy(breedingDateError = message)
        } else if (form.status.isBlank()) {
            invalid = form.copy(statusError = "Status is required")
        } else {
            val fetalCount = form.fetalCount?.trim()?.toIntOrNull()
            if (!form.fetalCount.isNullOrBlank() && fetalCount == null) {
                invalid = form.copy(fetalCountError = "Fetal count must be a whole number")
            } else {
                val lastCheckDate = parseDateOrNull(form.lastCheckDate)
                if (form.lastCheckDate != null && lastCheckDate == null) {
                    invalid = form.copy(lastCheckDateError = "Invalid date (YYYY-MM-DD)")
                }
            }
        }
        return invalid
    }

    private fun persist(form: GestationFormState) {
        val breedingDate = parseDateOrNull(form.breedingDate) ?: return
        val lastCheckDate = parseDateOrNull(form.lastCheckDate)
        val fetalCount = form.fetalCount?.trim()?.toIntOrNull()
        viewModelScope.launch {
            updateForm(form.copy(isSaving = true))
            val now = Clock.System.now()
            val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
            val progress = calculateGestationUseCase(breedingDate, today)
            val gestation =
                Gestation(
                    id = form.id ?: 0L,
                    patientId = patientId,
                    breedingDate = breedingDate,
                    expectedDueDate = progress.expectedDueDate,
                    gestationDays = progress.gestationDays,
                    status = form.status.trim(),
                    fetalCount = fetalCount,
                    lastCheckDate = lastCheckDate,
                    notes = form.notes,
                    createdAt = form.createdAt ?: now,
                    updatedAt = now,
                )
            runCatching { withContext(ioDispatcher) { saveGestationUseCase(gestation, today) } }
                .onSuccess {
                    formState.value?.let { updateForm(it.copy(isSaving = false)) }
                    emitSaved()
                }.onFailure { error ->
                    formState.value?.let {
                        updateForm(
                            it.copy(
                                isSaving = false,
                                breedingDateError = error.message ?: "Failed to save gestation record",
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
