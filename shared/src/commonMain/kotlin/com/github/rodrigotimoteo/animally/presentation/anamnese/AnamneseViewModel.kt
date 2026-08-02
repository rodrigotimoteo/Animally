package com.github.rodrigotimoteo.animally.presentation.anamnese

import androidx.lifecycle.viewModelScope
import com.github.rodrigotimoteo.animally.di.dispatchers.IO_DISPATCHER
import com.github.rodrigotimoteo.animally.domain.anamnese.model.Anamnese
import com.github.rodrigotimoteo.animally.domain.anamnese.usecase.GetAnamneseByPatientUseCase
import com.github.rodrigotimoteo.animally.domain.anamnese.usecase.SaveAnamneseUseCase
import com.github.rodrigotimoteo.animally.presentation.common.addEdit.BaseAddEditViewModel
import com.github.rodrigotimoteo.animally.presentation.navigation.AnimallyNavigator
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Named
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * View model for the patient anamnese form.
 *
 * Anamnese is 1:1 with a patient, so the record is always looked up by [patientId]:
 * an explicit [anamneseId] (edit entry) resolves to the same single record.
 *
 * @param patientId The id of the patient this anamnese belongs to.
 * @param anamneseId The id of the record being edited, or `null` when unknown.
 * @param getAnamneseByPatientUseCase Use case for loading the patient's anamnese.
 * @param saveAnamneseUseCase Use case for persisting the anamnese.
 * @param animallyNavigator The navigator to use for navigation.
 * @param ioDispatcher Dispatcher for blocking database work.
 */
class AnamneseViewModel(
    private val patientId: Long,
    private val anamneseId: Long?,
    private val getAnamneseByPatientUseCase: GetAnamneseByPatientUseCase,
    private val saveAnamneseUseCase: SaveAnamneseUseCase,
    animallyNavigator: AnimallyNavigator,
    @Named(IO_DISPATCHER) private val ioDispatcher: CoroutineDispatcher,
) : BaseAddEditViewModel<AnamneseFormState>(animallyNavigator) {
    init {
        load()
    }

    private fun load() {
        updateForm(AnamneseFormState(isLoading = true))
        viewModelScope.launch {
            runCatching { withContext(ioDispatcher) { getAnamneseByPatientUseCase(patientId) } }
                .onSuccess { anamnese ->
                    if (anamnese == null) {
                        updateForm(AnamneseFormState(id = anamneseId))
                    } else {
                        updateForm(
                            AnamneseFormState(
                                id = anamnese.id,
                                generalHistory = anamnese.generalHistory,
                                chronicConditions = anamnese.chronicConditions,
                                allergies = anamnese.allergies,
                                createdAt = anamnese.createdAt,
                            ),
                        )
                    }
                }.onFailure {
                    updateForm(AnamneseFormState())
                }
        }
    }

    /**
     * Updates the [AnamneseFormState.generalHistory].
     */
    fun onGeneralHistoryChange(value: String) {
        formState.value?.let { updateForm(it.copy(generalHistory = value)) }
    }

    /**
     * Updates the [AnamneseFormState.chronicConditions].
     */
    fun onChronicConditionsChange(value: String) {
        formState.value?.let { updateForm(it.copy(chronicConditions = value)) }
    }

    /**
     * Updates the [AnamneseFormState.allergies].
     */
    fun onAllergiesChange(value: String) {
        formState.value?.let { updateForm(it.copy(allergies = value)) }
    }

    /**
     * Validates and persists the current form.
     */
    override fun save() {
        val form = formState.value ?: return
        viewModelScope.launch {
            updateForm(form.copy(isSaving = true))
            val now = Clock.System.now()
            val anamnese =
                Anamnese(
                    id = form.id ?: 0L,
                    patientId = patientId,
                    generalHistory = form.generalHistory,
                    chronicConditions = form.chronicConditions,
                    allergies = form.allergies,
                    createdAt = form.createdAt ?: now,
                    updatedAt = now,
                )
            runCatching { withContext(ioDispatcher) { saveAnamneseUseCase(anamnese) } }
                .onSuccess {
                    formState.value?.let { updateForm(it.copy(isSaving = false)) }
                    popBackStack()
                }.onFailure {
                    formState.value?.let { updateForm(it.copy(isSaving = false)) }
                }
        }
    }
}

/**
 * UI state for the anamnese form.
 *
 * @param id The persisted record id; `null` when no record exists yet.
 * @param generalHistory Free-form general medical history.
 * @param chronicConditions Free-form chronic conditions.
 * @param allergies Free-form allergies.
 * @param createdAt The original creation timestamp, preserved when editing.
 * @param isLoading Whether the form is still loading the existing record.
 * @param isSaving Whether a save is currently in progress.
 */
data class AnamneseFormState(
    val id: Long? = null,
    val generalHistory: String = "",
    val chronicConditions: String = "",
    val allergies: String = "",
    val createdAt: Instant? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
) {
    val isEditing: Boolean get() = id != null
}
