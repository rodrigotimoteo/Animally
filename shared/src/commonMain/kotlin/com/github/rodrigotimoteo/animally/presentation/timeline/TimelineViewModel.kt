package com.github.rodrigotimoteo.animally.presentation.timeline

import androidx.lifecycle.viewModelScope
import com.github.rodrigotimoteo.animally.di.dispatchers.IO_DISPATCHER
import com.github.rodrigotimoteo.animally.domain.timeline.model.TimelineGroup
import com.github.rodrigotimoteo.animally.domain.timeline.usecase.GetTimelineUseCase
import com.github.rodrigotimoteo.animally.presentation.navigation.AnimallyNavigationViewModel
import com.github.rodrigotimoteo.animally.presentation.navigation.AnimallyNavigator
import com.github.rodrigotimoteo.animally.presentation.navigation.Route
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Named

/**
 * View model for the timeline screen.
 *
 * @param patientId The id of the patient, or null for global timeline.
 * @param getTimelineUseCase Use case for loading the timeline.
 * @param animallyNavigator The navigator to use for navigation.
 * @param ioDispatcher Dispatcher for blocking database work.
 */
class TimelineViewModel(
    private val patientId: Long?,
    private val getTimelineUseCase: GetTimelineUseCase,
    animallyNavigator: AnimallyNavigator,
    @Named(IO_DISPATCHER) private val ioDispatcher: CoroutineDispatcher,
) : AnimallyNavigationViewModel(animallyNavigator) {
    private val _uiState = MutableStateFlow(TimelineUiState(patientId = patientId))
    val uiState: StateFlow<TimelineUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    /**
     * Reloads the timeline feed.
     */
    fun load() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            runCatching {
                withContext(ioDispatcher) {
                    if (patientId != null) {
                        getTimelineUseCase(patientId)
                    } else {
                        getTimelineUseCase()
                    }
                }
            }.onSuccess { feed ->
                _uiState.update {
                    it.copy(
                        patientName = feed.patientName,
                        groups = feed.groups,
                        isLoading = false,
                    )
                }
            }.onFailure { error ->
                _uiState.update { it.copy(isLoading = false, errorMessage = error.message) }
            }
        }
    }

    /**
     * Navigates back to the previous destination.
     */
    fun onBack() = popBackStack()

    /**
     * Navigates to the source record for the given timeline entry.
     */
    fun onEntryClick(
        recordType: String,
        patientId: Long,
        recordId: Long,
    ) {
        navigateTo(routeForRecordType(recordType, patientId, recordId))
    }

    private fun routeForRecordType(
        recordType: String,
        patientId: Long,
        recordId: Long,
    ): Route {
        val routeMap =
            mapOf(
                "Vaccination" to Route.AddEditVaccination(patientId, recordId),
                "Consultation" to Route.AddEditConsultation(patientId, recordId),
                "Deworming" to Route.AddEditDeworming(patientId, recordId),
                "Dentistry" to Route.AddEditDentistry(patientId, recordId),
                "Weight" to Route.AddEditWeight(patientId, recordId),
                "Lameness" to Route.AddEditLameness(patientId, recordId),
                "Surgery" to Route.AddEditSurgery(patientId, recordId),
                "Medication" to Route.AddEditMedication(patientId, recordId),
                "Lab Result" to Route.AddEditLabResult(patientId, recordId),
                "Imaging" to Route.AddEditImaging(patientId, recordId),
                "Farrier" to Route.AddEditFarrierVisit(patientId, recordId),
                "Reproduction" to Route.AddEditReproductionEvent(patientId, recordId),
                "Ultrasound" to Route.AddEditUltrasound(patientId, recordId),
                "Gestation" to Route.AddEditGestation(patientId, recordId),
                "Repro Medication" to Route.AddEditReproMed(patientId, recordId),
                "Controlled Substance" to Route.AddEditControlledSubstance(patientId, recordId),
            )
        return routeMap[recordType] ?: Route.PatientDetail(patientId)
    }

    /**
     * Clears the current error message.
     */
    fun onDismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

/**
 * UI state for the timeline screen.
 *
 * @param patientId The id of the patient, or null for global timeline.
 * @param patientName The name of the patient, or null for global timeline.
 * @param groups The timeline groups, sorted by date descending.
 * @param isLoading Whether the timeline is being loaded.
 * @param errorMessage Message of the last error, or null when none.
 */
data class TimelineUiState(
    val patientId: Long? = null,
    val patientName: String? = null,
    val groups: List<TimelineGroup> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)
