package com.github.rodrigotimoteo.animally.presentation.coggins

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.rodrigotimoteo.animally.di.dispatchers.IO_DISPATCHER
import com.github.rodrigotimoteo.animally.domain.notification.NotificationScheduler
import com.github.rodrigotimoteo.animally.domain.patient.usecase.CogginsAlert
import com.github.rodrigotimoteo.animally.domain.patient.usecase.GetCogginsStatusUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import org.koin.core.annotation.KoinViewModel
import org.koin.core.annotation.Named
import kotlin.time.Clock

/**
 * View model for the Coggins alerts section of the settings screen.
 *
 * @param getCogginsStatusUseCase Use case for loading the Coggins alerts.
 * @param ioDispatcher Dispatcher for blocking database work.
 */
@KoinViewModel
class CogginsViewModel(
    private val getCogginsStatusUseCase: GetCogginsStatusUseCase,
    @Named(IO_DISPATCHER) private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {
    private val _uiState = MutableStateFlow(CogginsUiState())
    val uiState: StateFlow<CogginsUiState> = _uiState.asStateFlow()

    private val notificationScheduler = NotificationScheduler()

    init {
        load()
    }

    /**
     * Reloads the Coggins alerts and hands them to the platform notification scheduler.
     */
    fun load() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            runCatching {
                withContext(ioDispatcher) {
                    getCogginsStatusUseCase(today = Clock.System.todayIn(TimeZone.currentSystemDefault()))
                }
            }.onSuccess { alerts ->
                notificationScheduler.scheduleCogginsNotifications(alerts)
                _uiState.update { it.copy(alerts = alerts, isLoading = false) }
            }.onFailure { error ->
                _uiState.update { it.copy(isLoading = false, errorMessage = error.message) }
            }
        }
    }

    /**
     * Dismisses any error surfaced by the screen.
     */
    fun onDismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

/**
 * UI state for the Coggins alerts section.
 *
 * @param alerts The currently loaded Coggins alerts.
 * @param isLoading Whether the alerts are being loaded.
 * @param errorMessage Message of the last error, or `null` when none.
 */
data class CogginsUiState(
    val alerts: List<CogginsAlert> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)
