package com.github.rodrigotimoteo.animally.presentation.care

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.rodrigotimoteo.animally.di.dispatchers.IO_DISPATCHER
import com.github.rodrigotimoteo.animally.domain.care.CareDueItem
import com.github.rodrigotimoteo.animally.domain.care.GetUpcomingRemindersUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import org.koin.core.annotation.Named
import kotlin.time.Clock

/**
 * View model for the Care Due panel on the patient Overview tab.
 *
 * @param patientId The id of the patient whose care items are listed.
 * @param getUpcomingRemindersUseCase Use case for aggregating due care.
 * @param ioDispatcher Dispatcher for blocking database work.
 */
class UpcomingCareViewModel(
    private val patientId: Long,
    private val getUpcomingRemindersUseCase: GetUpcomingRemindersUseCase,
    @Named(IO_DISPATCHER) private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {
    private val _uiState = MutableStateFlow(UpcomingCareUiState())
    val uiState: StateFlow<UpcomingCareUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    /**
     * Reloads the care-due list for the patient.
     */
    fun load() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            runCatching {
                withContext(ioDispatcher) {
                    getUpcomingRemindersUseCase(
                        patientId,
                        Clock.System.todayIn(TimeZone.currentSystemDefault()),
                    )
                }
            }.onSuccess { items ->
                _uiState.update { it.copy(items = items, isLoading = false) }
            }.onFailure {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
}

/**
 * UI state for the Care Due panel.
 *
 * @property items The due care items, soonest first. Empty while loading or when nothing is due.
 * @property isLoading Whether the list is being loaded.
 */
data class UpcomingCareUiState(
    val items: List<CareDueItem> = emptyList(),
    val isLoading: Boolean = false,
)
