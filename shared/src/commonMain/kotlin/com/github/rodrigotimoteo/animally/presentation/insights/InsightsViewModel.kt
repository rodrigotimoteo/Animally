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

/**
 * Shared presentation ViewModel for the Insights dashboard.
 *
 * Calculates no metrics itself; delegates to [GetInsightsDashboardUseCase] which
 * aggregates deterministically from persisted records. This ViewModel owns
 * preset resolution, patient scope, custom-range validation, cancellable reloads
 * and immutable StateFlow exposure.
 *
 * Rapid filter changes cancel the previous [Job] so an older repository result
 * cannot publish over a newer one. All heavy work runs on [ioDispatcher].
 * The first load defaults to the locked trailing 30-day inclusive range.
 * Invalid custom input stays visible with validation feedback and does not
 * trigger a repository load.
 *
 * Exposes immutable [StateFlow] for Swift/Compose observation.
 *
 * @param getInsightsDashboardUseCase deterministic snapshot use case.
 * @param ioDispatcher dispatcher for database work.
 * @param todayProvider injected today for deterministic tests and 30-day default.
 * @param initialPatientId optional scope; null means all active patients.
 */
class InsightsViewModel(
    private val getInsightsDashboardUseCase: GetInsightsDashboardUseCase,
    @Named(IO_DISPATCHER) private val ioDispatcher: CoroutineDispatcher,
    private val todayProvider: () -> LocalDate = {
        Clock.System.todayIn(TimeZone.currentSystemDefault())
    },
    private val initialPatientId: Long? = null,
    private val logger: InsightsLogger = NoOpInsightsLogger,
) : ViewModel() {
    private fun defaultRange(today: LocalDate): Pair<LocalDate, LocalDate> {
        val from = today.minus(DatePeriod(days = THIRTY_DAYS_INCLUSIVE_OFFSET))
        return from to today
    }

    // Single today capture reused for _uiState init and initial load — avoids midnight delta between two todayProvider() calls.
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

    /** Immutable StateFlow for Swift/Compose. */
    val uiState: StateFlow<InsightsUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        // Trigger initial load with same today as _uiState init — prevents midnight divergence (S3 oracle).
        val state = _uiState.value
        loadWithPreset(state.preset, state.patientId, state.customFrom, state.customTo, today = initialToday)
    }

    /**
     * Reloads using the current preset, patient scope and custom inputs.
     *
     * Cancels any in-flight load so rapid changes cannot publish stale results.
     */
    fun reload() {
        val state = _uiState.value
        loadWithPreset(state.preset, state.patientId, state.customFrom, state.customTo)
    }

    /**
     * Selects a preset and triggers a reload.
     *
     * @param preset target preset.
     */
    fun selectPreset(preset: InsightsPreset) {
        // Clear validation when leaving custom; keep custom inputs visible for return.
        _uiState.update { it.copy(preset = preset, validationError = null, errorMessage = null) }
        loadWithPreset(preset, _uiState.value.patientId, _uiState.value.customFrom, _uiState.value.customTo)
    }

    /**
     * Applies a custom inclusive range.
     *
     * The inputs stay visible even when invalid; validation feedback is set
     * and no repository call is made until the range is valid and not empty.
     *
     * @param from inclusive lower bound or null.
     * @param to inclusive upper bound or null.
     */
    fun setCustomRange(
        from: LocalDate?,
        to: LocalDate?,
    ) {
        _uiState.update { it.copy(customFrom = from, customTo = to, preset = InsightsPreset.CUSTOM) }
        val error = validateCustomRange(from, to)
        if (error != null) {
            _uiState.update { it.copy(validationError = error, isLoading = false) }
            return
        }
        _uiState.update { it.copy(validationError = null, errorMessage = null) }
        loadWithPreset(InsightsPreset.CUSTOM, _uiState.value.patientId, from, to)
    }

    /** Updates only the custom from input, keeping the other end visible. */
    fun setCustomFrom(from: LocalDate?) = setCustomRange(from, _uiState.value.customTo)

    /** Updates only the custom to input, keeping the other end visible. */
    fun setCustomTo(to: LocalDate?) = setCustomRange(_uiState.value.customFrom, to)

    /**
     * Updates patient scope and reloads.
     *
     * Clears validationError consistently; range may still be invalid but
     * loadWithPreset will re-validate for CUSTOM.
     *
     * @param patientId null for all active patients, or a single active patient id.
     */
    fun setPatientScope(patientId: Long?) {
        _uiState.update { it.copy(patientId = patientId, errorMessage = null, validationError = null) }
        loadWithPreset(_uiState.value.preset, patientId, _uiState.value.customFrom, _uiState.value.customTo)
    }

    /**
     * Clears the last error message.
     */
    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    /**
     * Clears custom-range validation feedback.
     */
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

    private fun loadWithPreset(
        preset: InsightsPreset,
        patientId: Long?,
        customFrom: LocalDate?,
        customTo: LocalDate?,
        today: LocalDate = todayProvider(),
    ) {
        // Single capture for this load — filter bounds and UC today must match to avoid midnight divergence.
        // Default todayProvider() gives fresh today for subsequent reloads; init passes initialToday explicitly.
        when (preset) {
            InsightsPreset.THIRTY_DAYS -> {
                val (from, to) = defaultRange(today)
                _uiState.update { it.copy(from = from, to = to, validationError = null) }
                launchLoad(
                    patientId = patientId,
                    filter = InsightsFilter(from = from, to = to, patientId = patientId),
                    isAllTime = false,
                    today = today,
                )
            }
            InsightsPreset.NINETY_DAYS -> {
                val from = today.minus(DatePeriod(days = NINETY_DAYS_INCLUSIVE_OFFSET))
                val to = today
                _uiState.update { it.copy(from = from, to = to, validationError = null) }
                launchLoad(
                    patientId = patientId,
                    filter = InsightsFilter(from = from, to = to, patientId = patientId),
                    isAllTime = false,
                    today = today,
                )
            }
            InsightsPreset.ALL_TIME -> {
                _uiState.update { it.copy(from = null, to = null, validationError = null) }
                launchLoad(patientId = patientId, filter = null, isAllTime = true, today = today)
            }
            InsightsPreset.CUSTOM -> {
                val error = validateCustomRange(customFrom, customTo)
                if (error != null) {
                    _uiState.update { it.copy(validationError = error, isLoading = false) }
                    return
                }
                val from = requireNotNull(customFrom)
                val to = requireNotNull(customTo)
                _uiState.update { it.copy(from = from, to = to, validationError = null) }
                launchLoad(
                    patientId = patientId,
                    filter = InsightsFilter(from = from, to = to, patientId = patientId),
                    isAllTime = false,
                    today = today,
                )
            }
        }
    }

    private fun launchLoad(
        patientId: Long?,
        filter: InsightsFilter?,
        isAllTime: Boolean,
        today: LocalDate,
    ) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        loadJob?.cancel()
        loadJob =
            viewModelScope.launch {
                try {
                    val snapshot =
                        withContext(ioDispatcher) {
                            if (isAllTime) {
                                getInsightsDashboardUseCase.getAllTimeSnapshot(patientId, today)
                            } else {
                                getInsightsDashboardUseCase.invoke(requireNotNull(filter), today)
                            }
                        }
                    _uiState.update {
                        it.copy(
                            snapshot = snapshot,
                            isLoading = false,
                            errorMessage = null,
                        )
                    }
                } catch (ce: CancellationException) {
                    throw ce
                } catch (t: Exception) {
                    logger.e(t, "Insights load failed: ${t.message}")
                    val userMessage = InsightsErrorMapper.map(t, FALLBACK_ERROR)
                    _uiState.update { it.copy(isLoading = false, errorMessage = userMessage) }
                }
            }
    }

    private companion object {
        const val THIRTY_DAYS_INCLUSIVE_OFFSET = 29
        const val NINETY_DAYS_INCLUSIVE_OFFSET = 89
        const val VALIDATION_BOTH_REQUIRED = "Select both start and end dates"
        const val VALIDATION_START_AFTER_END = "Start date must be on or before end date"
        const val FALLBACK_ERROR = "Failed to load insights"
    }
}
