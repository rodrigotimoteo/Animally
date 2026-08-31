package com.github.rodrigotimoteo.animally.presentation.insights

import com.github.rodrigotimoteo.animally.domain.insights.model.InsightsDrillDown
import com.github.rodrigotimoteo.animally.domain.insights.model.InsightsRecordRef

/**
 * UI state for the filtered record-reference list.
 *
 * @property drillDown active drill-down filter (inclusive range + optional type/patient).
 * @property refs matching record references ordered by date descending.
 * @property isLoading true while a repository load is in flight.
 * @property errorMessage last load failure message.
 */
data class InsightsRecordsUiState(
    val drillDown: InsightsDrillDown,
    val refs: List<InsightsRecordRef> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)
