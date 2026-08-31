package com.github.rodrigotimoteo.animally.domain.insights.model

/**
 * Deterministic dashboard snapshot for a validated [InsightsFilter].
 *
 * All values are computed in shared Kotlin from persisted records; Swift only
 * renders. Every displayed metric can drill down to [InsightsRecordRef] rows.
 * Current-state cards in [currentCare] use the single injected `today` captured
 * for the load and are not constrained by the historical range.
 *
 * @property overview internship overview metrics and period-over-period comparison.
 * @property activitySeries activity trend buckets (daily through 45 days, weekly 46..180, monthly >180).
 * @property recordMix count and share by [com.github.rodrigotimoteo.animally.domain.common.RecordType].
 * @property reproduction breeding, embryo, ICSI and ultrasound totals for the period.
 * @property currentCare operational gestation snapshot as of today.
 * @property dataIssues explicit counts for research-readiness rules.
 * @property appliedFilter exact persisted-record range used to build this snapshot,
 *   including the resolved earliest date for all-time loads; null only when an
 *   all-time scope has no activity rows.
 */
data class InsightsSnapshot(
    val overview: OverviewMetrics,
    val activitySeries: List<ActivityPoint>,
    val recordMix: List<RecordTypeCount>,
    val reproduction: ReproductionMetrics,
    val currentCare: CurrentCareSnapshot,
    val dataIssues: List<InsightsDataIssueCount>,
    val appliedFilter: InsightsFilter? = null,
) {
    /**
     * Total affected rows across all readiness rules.
     * Not a quality score; explicit sum of individual issue counts for research-readiness.
     */
    val totalIssues: Int get() = dataIssues.sumOf { it.count }

    /**
     * Counts indexed by issue type for drill-down.
     * Missing types are omitted (count 0).
     */
    val issuesByType: Map<InsightsDataIssueType, Int> get() = dataIssues.associate { it.type to it.count }

    /**
     * Alias for [dataIssues] using the plan's preferred naming `issueCounts`.
     */
    val issueCounts: List<InsightsDataIssueCount> get() = dataIssues
}
