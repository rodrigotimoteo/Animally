package com.github.rodrigotimoteo.animally.presentation.timeline

import androidx.lifecycle.viewModelScope
import com.github.rodrigotimoteo.animally.di.dispatchers.IO_DISPATCHER
import com.github.rodrigotimoteo.animally.domain.common.RecordType
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
        // Parse the wire string back to the enum, then look up the editor route.
        // Keys are enum constants, so an unknown display literal can never match;
        // unmapped types land on the patient detail instead of a wrong editor.
        return recordRoutes[RecordType.fromDisplayName(recordType)]
            ?.invoke(patientId, recordId)
            ?: Route.PatientDetail(patientId)
    }

    private companion object {
        /** Editor route factories keyed by [RecordType]; types without editors are absent. */
        val recordRoutes: Map<RecordType, (patientId: Long, recordId: Long) -> Route> =
            mapOf(
                RecordType.Vaccination to { patientId, recordId -> Route.AddEditVaccination(patientId, recordId) },
                RecordType.Consultation to { patientId, recordId -> Route.AddEditConsultation(patientId, recordId) },
                RecordType.Deworming to { patientId, recordId -> Route.AddEditDeworming(patientId, recordId) },
                RecordType.Dentistry to { patientId, recordId -> Route.AddEditDentistry(patientId, recordId) },
                RecordType.Weight to { patientId, recordId -> Route.AddEditWeight(patientId, recordId) },
                RecordType.Lameness to { patientId, recordId -> Route.AddEditLameness(patientId, recordId) },
                RecordType.Surgery to { patientId, recordId -> Route.AddEditSurgery(patientId, recordId) },
                RecordType.Medication to { patientId, recordId -> Route.AddEditMedication(patientId, recordId) },
                RecordType.LabResult to { patientId, recordId -> Route.AddEditLabResult(patientId, recordId) },
                RecordType.Imaging to { patientId, recordId -> Route.AddEditImaging(patientId, recordId) },
                RecordType.FarrierVisit to { patientId, recordId -> Route.AddEditFarrierVisit(patientId, recordId) },
                RecordType.ReproductionEvent to { patientId, recordId ->
                    Route.AddEditReproductionEvent(patientId, recordId)
                },
                RecordType.Ultrasound to { patientId, recordId -> Route.AddEditUltrasound(patientId, recordId) },
                RecordType.Gestation to { patientId, recordId -> Route.AddEditGestation(patientId, recordId) },
                RecordType.ReproMedication to { patientId, recordId -> Route.AddEditReproMed(patientId, recordId) },
                RecordType.ControlledSubstance to { patientId, recordId ->
                    Route.AddEditControlledSubstance(patientId, recordId)
                },
                RecordType.Anamnese to { patientId, recordId -> Route.AddEditAnamnese(patientId, recordId) },
                RecordType.CustomReminder to { patientId, recordId ->
                    Route.AddEditCustomReminder(patientId, recordId)
                },
            )
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
