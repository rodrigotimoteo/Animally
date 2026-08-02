package com.github.rodrigotimoteo.animally.presentation.lameness

import androidx.lifecycle.viewModelScope
import com.github.rodrigotimoteo.animally.di.dispatchers.IO_DISPATCHER
import com.github.rodrigotimoteo.animally.domain.lameness.model.Lameness
import com.github.rodrigotimoteo.animally.domain.lameness.usecase.GetLamenessDetailUseCase
import com.github.rodrigotimoteo.animally.domain.lameness.usecase.SaveLamenessUseCase
import com.github.rodrigotimoteo.animally.presentation.common.addEdit.BaseAddEditViewModel
import com.github.rodrigotimoteo.animally.presentation.navigation.AnimallyNavigator
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import org.koin.core.annotation.Named
import kotlin.time.Clock

/**
 * View model for the lameness add/edit form.
 *
 * @param patientId The id of the patient this lameness evaluation belongs to.
 * @param lamenessId The id of the lameness evaluation being edited, or `null` when creating a new one.
 * @param getLamenessDetailUseCase Use case for loading an existing lameness evaluation.
 * @param saveLamenessUseCase Use case for persisting the lameness evaluation.
 * @param animallyNavigator The navigator to use for navigation.
 * @param ioDispatcher Dispatcher for blocking database work.
 */
class LamenessEditViewModel(
    private val patientId: Long,
    private val lamenessId: Long?,
    private val getLamenessDetailUseCase: GetLamenessDetailUseCase,
    private val saveLamenessUseCase: SaveLamenessUseCase,
    animallyNavigator: AnimallyNavigator,
    @Named(IO_DISPATCHER) private val ioDispatcher: CoroutineDispatcher,
) : BaseAddEditViewModel<LamenessFormState>(animallyNavigator) {
    init {
        if (lamenessId != null) {
            loadLameness(lamenessId)
        } else {
            updateForm(LamenessFormState())
        }
    }

    private fun loadLameness(id: Long) {
        updateForm(LamenessFormState(id = id, isLoading = true))
        viewModelScope.launch {
            runCatching { withContext(ioDispatcher) { getLamenessDetailUseCase(id) } }
                .onSuccess { lameness ->
                    if (lameness == null) {
                        updateForm(LamenessFormState(id = id, dateError = "Lameness not found"))
                    } else {
                        updateForm(
                            LamenessFormState(
                                id = lameness.id,
                                date = lameness.date.toString(),
                                gradeAAEP = lameness.gradeAAEP.toString(),
                                limbLocation = lameness.limbLocation,
                                flexionTest = lameness.flexionTest,
                                diagnosis = lameness.diagnosis,
                                treatment = lameness.treatment,
                                vetName = lameness.vetName,
                                notes = lameness.notes,
                                createdAt = lameness.createdAt,
                            ),
                        )
                    }
                }.onFailure { error ->
                    updateForm(
                        LamenessFormState(
                            id = id,
                            dateError = error.message ?: "Failed to load lameness",
                        ),
                    )
                }
        }
    }

    /**
     * Updates the [LamenessFormState.date].
     */
    fun onDateChange(date: String) {
        formState.value?.let { updateForm(it.copy(date = date, dateError = null)) }
    }

    /**
     * Updates the [LamenessFormState.gradeAAEP].
     */
    fun onGradeAAEPChange(value: String) {
        formState.value?.let { updateForm(it.copy(gradeAAEP = value, gradeError = null)) }
    }

    /**
     * Updates the [LamenessFormState.limbLocation].
     */
    fun onLimbLocationChange(value: String) {
        formState.value?.let { updateForm(it.copy(limbLocation = value.ifBlank { null })) }
    }

    /**
     * Updates the [LamenessFormState.flexionTest].
     */
    fun onFlexionTestChange(value: String) {
        formState.value?.let { updateForm(it.copy(flexionTest = value.ifBlank { null })) }
    }

    /**
     * Updates the [LamenessFormState.diagnosis].
     */
    fun onDiagnosisChange(value: String) {
        formState.value?.let { updateForm(it.copy(diagnosis = value.ifBlank { null })) }
    }

    /**
     * Updates the [LamenessFormState.treatment].
     */
    fun onTreatmentChange(value: String) {
        formState.value?.let { updateForm(it.copy(treatment = value.ifBlank { null })) }
    }

    /**
     * Updates the [LamenessFormState.vetName].
     */
    fun onVetNameChange(value: String) {
        formState.value?.let { updateForm(it.copy(vetName = value.ifBlank { null })) }
    }

    /**
     * Updates the [LamenessFormState.notes].
     */
    fun onNotesChange(value: String) {
        formState.value?.let { updateForm(it.copy(notes = value.ifBlank { null })) }
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
        val grade = form.gradeAAEP.toIntOrNull()
        if (grade == null || grade !in 1..5) {
            updateForm(form.copy(gradeError = "Grade must be 1-5"))
            return
        }
        viewModelScope.launch {
            updateForm(form.copy(isSaving = true))
            val now = Clock.System.now()
            val lameness =
                Lameness(
                    id = form.id ?: 0L,
                    patientId = patientId,
                    date = date,
                    gradeAAEP = grade,
                    limbLocation = form.limbLocation,
                    flexionTest = form.flexionTest,
                    diagnosis = form.diagnosis,
                    treatment = form.treatment,
                    vetName = form.vetName,
                    notes = form.notes,
                    createdAt = form.createdAt ?: now,
                    updatedAt = now,
                )
            runCatching { withContext(ioDispatcher) { saveLamenessUseCase(lameness) } }
                .onSuccess {
                    formState.value?.let { updateForm(it.copy(isSaving = false)) }
                    popBackStack()
                }.onFailure { error ->
                    formState.value?.let {
                        updateForm(
                            it.copy(
                                isSaving = false,
                                dateError = error.message ?: "Failed to save lameness",
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
