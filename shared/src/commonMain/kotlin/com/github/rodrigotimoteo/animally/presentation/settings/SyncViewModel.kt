package com.github.rodrigotimoteo.animally.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.rodrigotimoteo.animally.di.dispatchers.IO_DISPATCHER
import com.github.rodrigotimoteo.animally.domain.sync.SyncMetadataRepository
import com.github.rodrigotimoteo.animally.domain.sync.SyncResult
import com.github.rodrigotimoteo.animally.domain.sync.SyncUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.annotation.KoinViewModel
import org.koin.core.annotation.Named
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * View model for the cloud sync section of the settings screen.
 *
 * Drives a manual sync cycle on demand and surfaces the last-synced timestamp
 * alongside any failure message from the most recent run.
 *
 * @param syncUseCase Entry point for a manual sync cycle.
 * @param syncMetadataRepository Repository for the last-synced marker and device id.
 * @param ioDispatcher Dispatcher for blocking sync work.
 */
@KoinViewModel
class SyncViewModel(
    private val syncUseCase: SyncUseCase,
    private val syncMetadataRepository: SyncMetadataRepository,
    @Named(IO_DISPATCHER) private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SyncUiState())
    val uiState: StateFlow<SyncUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val lastSync =
                withContext(ioDispatcher) {
                    val deviceId = syncMetadataRepository.getDeviceId()
                    syncMetadataRepository.getOrCreateLastSyncAt(deviceId)
                }
            _uiState.update {
                it.copy(
                    lastSyncAt = if (lastSync == Instant.DISTANT_PAST) null else lastSync,
                )
            }
        }
    }

    /**
     * Runs a sync cycle, updating the last-synced marker on success and surfacing
     * any failure message on error.
     */
    fun syncNow() {
        if (_uiState.value.isSyncing) return
        _uiState.update { it.copy(isSyncing = true, errorMessage = null) }
        viewModelScope.launch {
            val result =
                withContext(ioDispatcher) {
                    runCatching { syncUseCase() }
                }
            result
                .onSuccess { syncResult ->
                    val newLastSync =
                        if (syncResult.success) {
                            syncResult.serverTimestamp ?: Clock.System.now()
                        } else {
                            null
                        }
                    _uiState.update {
                        it.copy(
                            isSyncing = false,
                            lastSyncAt = newLastSync ?: it.lastSyncAt,
                            lastResult = syncResult,
                            errorMessage = syncResult.errorMessage,
                        )
                    }
                }.onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isSyncing = false,
                            errorMessage = error.message ?: "Sync failed unexpectedly",
                        )
                    }
                }
        }
    }

    /**
     * Dismisses the error message surfaced by the most recent sync failure.
     */
    fun onDismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

/**
 * UI state for the cloud sync section of the settings screen.
 *
 * @param isSyncing Whether a sync cycle is currently in flight.
 * @param lastSyncAt Timestamp of the last successful sync, or `null` when never synced.
 * @param lastResult Outcome of the most recent sync run, or `null` before the first run.
 * @param errorMessage Message of the last error, or `null` when none.
 */
data class SyncUiState(
    val isSyncing: Boolean = false,
    val lastSyncAt: Instant? = null,
    val lastResult: SyncResult? = null,
    val errorMessage: String? = null,
)
