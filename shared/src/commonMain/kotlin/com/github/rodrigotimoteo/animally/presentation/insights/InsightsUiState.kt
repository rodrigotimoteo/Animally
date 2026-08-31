package com.github.rodrigotimoteo.animally.presentation.insights

import com.github.rodrigotimoteo.animally.domain.insights.model.InsightsSnapshot
import kotlinx.datetime.LocalDate

/**
 * Date preset for Insights.
 *
 * Each preset resolves to a validated [com.github.rodrigotimoteo.animally.domain.insights.model.InsightsFilter]
 * before repository access. `ALL_TIME` resolves via earliest activity through today; `CUSTOM`
 * requires validated inclusive bounds.
 */
enum class InsightsPreset {
    THIRTY_DAYS,
    NINETY_DAYS,
    ALL_TIME,
    CUSTOM,
}

/**
 * Ui state for the Insights dashboard.
 *
 * Holds the selected preset, patient scope, effective range, custom inputs that stay
 * visible on validation failure, snapshot, loading, error and validation feedback.
 * All derived empty/content states are explicit (no zero/NaN hiding).
 *
 * @property preset selected preset.
 * @property patientId null means all active patients, non-null scopes to one active patient.
 * @property from inclusive lower bound of the applied filter, null for all-time.
 * @property to inclusive upper bound of the applied filter, null for all-time.
 * @property customFrom user input for custom preset; stays visible even when invalid.
 * @property customTo user input for custom preset; stays visible even when invalid.
 * @property snapshot deterministic snapshot for the applied filter, null before first success.
 * @property isLoading true while a repository load is in flight.
 * @property errorMessage last load failure message, null when none.
 * @property validationError custom range validation message, null when valid.
 */
data class InsightsUiState(
    val preset: InsightsPreset = InsightsPreset.THIRTY_DAYS,
    val patientId: Long? = null,
    val from: LocalDate? = null,
    val to: LocalDate? = null,
    val customFrom: LocalDate? = null,
    val customTo: LocalDate? = null,
    val snapshot: InsightsSnapshot? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val validationError: String? = null,
) {
    /** True when a valid snapshot with zero activity is shown. */
    val isEmpty: Boolean
        get() =
            snapshot != null &&
                !isLoading &&
                errorMessage == null &&
                validationError == null &&
                snapshot.overview.activityCount == 0

    /** True when content is available. */
    val isContent: Boolean
        get() =
            snapshot != null &&
                !isLoading &&
                errorMessage == null &&
                validationError == null &&
                snapshot.overview.activityCount > 0

    /** True when the comparison range is disabled (all-time). */
    val isComparisonEnabled: Boolean
        get() = preset != InsightsPreset.ALL_TIME
}
