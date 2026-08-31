package com.github.rodrigotimoteo.animally.presentation.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.rodrigotimoteo.animally.di.dispatchers.IO_DISPATCHER
import com.github.rodrigotimoteo.animally.domain.insights.IInsightsRepository
import com.github.rodrigotimoteo.animally.domain.insights.model.InsightsDrillDown
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Named

class InsightsRecordsViewModel(
    private val repository: IInsightsRepository,
    @Named(IO_DISPATCHER) private val ioDispatcher: CoroutineDispatcher,
    private val drillDown: InsightsDrillDown,
    private val logError: (Throwable, String) -> Unit = { _, _ -> },
) : ViewModel() {
    private val _uiState = MutableStateFlow(InsightsRecordsUiState(drillDown = drillDown, isLoading = true))
    val uiState: StateFlow<InsightsRecordsUiState> = _uiState.asStateFlow()
    private var loadJob: Job? = null

    init {
        reload()
    }

    fun reload() {
        loadJob?.cancel()
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        loadJob =
            viewModelScope.launch {
                try {
                    val refs = withContext(ioDispatcher) { repository.getRecordRefs(drillDown) }
                    _uiState.update { it.copy(refs = refs, isLoading = false, errorMessage = null) }
                } catch (ce: CancellationException) {
                    throw ce
                } catch (t: Exception) {
                    logError(t, "Insights records load failed: ${t.message}")
                    _uiState.update { it.copy(isLoading = false, errorMessage = mapError(t, FALLBACK_ERROR)) }
                }
            }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun mapError(
        t: Throwable,
        fallback: String,
    ): String {
        val raw = t.message?.trim()
        if (raw.isNullOrBlank()) return fallback
        val lower = raw.lowercase()
        if (lower.contains("sql") || lower.contains("sqlite") || lower.contains("driver")) return fallback
        return raw
    }

    private companion object {
        const val FALLBACK_ERROR = "Failed to load records"
    }
}
