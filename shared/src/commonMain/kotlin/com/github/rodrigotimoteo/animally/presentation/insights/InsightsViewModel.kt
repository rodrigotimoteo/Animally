@file:Suppress("Wrapping", "MaximumLineLength", "MaxLineLength")

package com.github.rodrigotimoteo.animally.presentation.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.rodrigotimoteo.animally.di.dispatchers.IO_DISPATCHER
import com.github.rodrigotimoteo.animally.domain.insights.model.InsightsFilter
import com.github.rodrigotimoteo.animally.domain.insights.usecase.GetInsightsDashboardUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.todayIn
import org.koin.core.annotation.Named
import kotlin.time.Clock

class InsightsViewModel(
    private val getInsightsDashboardUseCase: GetInsightsDashboardUseCase,
    @Named(IO_DISPATCHER) private val ioDispatcher: CoroutineDispatcher,
    private val todayProvider: () -> LocalDate = { Clock.System.todayIn(TimeZone.currentSystemDefault()) },
    private val initialPatientId: Long? = null,
    private val logError: (Throwable, String) -> Unit = { _, _ -> },
) : ViewModel() {
    private fun defaultRange(today: LocalDate): Pair<LocalDate, LocalDate> {
        val from = today.minus(DatePeriod(days = THIRTY_DAYS_INCLUSIVE_OFFSET))
        return from to today
    }

    private val initialToday: LocalDate = todayProvider()

    private val _uiState: MutableStateFlow<InsightsUiState> =
        run {
            val (from, to) = defaultRange(initialToday)
            MutableStateFlow(
                InsightsUiState(
                    preset = InsightsPreset.THIRTY_DAYS,
                    patientId = initialPatientId,
                    from = from,
                    to = to,
                    customFrom = from,
                    customTo = to,
                    isLoading = false,
                    errorMessage = null,
                    validationError = null,
                ),
            )
        }

    val uiState: StateFlow<InsightsUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        val state = _uiState.value
        loadWithPreset(state.preset, state.patientId, state.customFrom, state.customTo, today = initialToday)
    }

    fun reload() {
        val state = _uiState.value
        loadWithPreset(state.preset, state.patientId, state.customFrom, state.customTo)
    }

    fun selectPreset(preset: InsightsPreset) {
        _uiState.update { it.copy(preset = preset, validationError = null, errorMessage = null) }
        loadWithPreset(preset, _uiState.value.patientId, _uiState.value.customFrom, _uiState.value.customTo)
    }

    fun setCustomRange(
        from: LocalDate?,
        to: LocalDate?,
    ) {
        _uiState.update { it.copy(customFrom = from, customTo = to, preset = InsightsPreset.CUSTOM) }
        loadWithPreset(InsightsPreset.CUSTOM, _uiState.value.patientId, from, to)
    }

    fun setCustomFrom(from: LocalDate?) = setCustomRange(from, _uiState.value.customTo)

    fun setCustomTo(to: LocalDate?) = setCustomRange(_uiState.value.customFrom, to)

    fun setPatientScope(patientId: Long?) {
        _uiState.update { it.copy(patientId = patientId, errorMessage = null, validationError = null) }
        loadWithPreset(_uiState.value.preset, patientId, _uiState.value.customFrom, _uiState.value.customTo)
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun dismissValidationError() {
        _uiState.update { it.copy(validationError = null) }
    }

    private fun validateCustomRange(
        from: LocalDate?,
        to: LocalDate?,
    ): String? {
        if (from == null || to == null) return VALIDATION_BOTH_REQUIRED
        if (from > to) return VALIDATION_START_AFTER_END
        return null
    }

    sealed interface Resolved {
        data class Finite(
            val filter: InsightsFilter,
        ) : Resolved

        data class AllTime(
            val patientId: Long?,
        ) : Resolved

        data class Invalid(
            val error: String,
            val from: LocalDate?,
            val to: LocalDate?,
        ) : Resolved
    }

    private fun resolve(
        preset: InsightsPreset,
        patientId: Long?,
        customFrom: LocalDate?,
        customTo: LocalDate?,
        today: LocalDate,
    ): Resolved =
        when (preset) {
            InsightsPreset.THIRTY_DAYS -> {
                val (from, to) = defaultRange(today)
                Resolved.Finite(InsightsFilter(from, to, patientId))
            }
            InsightsPreset.NINETY_DAYS -> {
                val from = today.minus(DatePeriod(days = NINETY_DAYS_INCLUSIVE_OFFSET))
                Resolved.Finite(InsightsFilter(from, today, patientId))
            }
            InsightsPreset.ALL_TIME -> Resolved.AllTime(patientId)
            InsightsPreset.CUSTOM -> {
                val err = validateCustomRange(customFrom, customTo)
                if (err !=
                    null
                ) {
                    Resolved.Invalid(err, customFrom, customTo)
                } else {
                    Resolved.Finite(InsightsFilter(requireNotNull(customFrom), requireNotNull(customTo), patientId))
                }
            }
        }

    private fun loadWithPreset(
        preset: InsightsPreset,
        patientId: Long?,
        customFrom: LocalDate?,
        customTo: LocalDate?,
        today: LocalDate = todayProvider(),
    ) {
        when (val r = resolve(preset, patientId, customFrom, customTo, today)) {
            is Resolved.Invalid -> _uiState.update { it.copy(validationError = r.error, from = r.from, to = r.to, isLoading = false) }
            is Resolved.Finite -> {
                _uiState.update { it.copy(from = r.filter.from, to = r.filter.to, validationError = null) }
                launchLoad(r, today)
            }
            is Resolved.AllTime -> {
                _uiState.update { it.copy(from = null, to = null, validationError = null) }
                launchLoad(r, today)
            }
        }
    }

    private fun launchLoad(
        resolved: Resolved,
        today: LocalDate,
    ) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        loadJob?.cancel()
        loadJob =
            viewModelScope.launch {
                try {
                    val snapshot =
                        withContext(ioDispatcher) {
                            when (resolved) {
                                is Resolved.Finite -> getInsightsDashboardUseCase.invoke(resolved.filter, today)
                                is Resolved.AllTime -> getInsightsDashboardUseCase.getAllTimeSnapshot(resolved.patientId, today)
                                is Resolved.Invalid -> error("Invalid should not reach launchLoad")
                            }
                        }
                    _uiState.update { it.copy(snapshot = snapshot, isLoading = false, errorMessage = null) }
                } catch (ce: CancellationException) {
                    throw ce
                } catch (t: Exception) {
                    logError(t, "Insights load failed: ${t.message}")
                    _uiState.update { it.copy(isLoading = false, errorMessage = mapError(t, FALLBACK_ERROR)) }
                }
            }
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
        const val THIRTY_DAYS_INCLUSIVE_OFFSET = 29
        const val NINETY_DAYS_INCLUSIVE_OFFSET = 89
        const val VALIDATION_BOTH_REQUIRED = "Select both start and end dates"
        const val VALIDATION_START_AFTER_END = "Start date must be on or before end date"
        const val FALLBACK_ERROR = "Failed to load insights"
    }
}
