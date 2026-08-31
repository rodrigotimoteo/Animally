package com.github.rodrigotimoteo.animally.domain.insights

import com.github.rodrigotimoteo.animally.domain.insights.model.CurrentCareSnapshot
import com.github.rodrigotimoteo.animally.domain.insights.model.InsightsActivityBucket
import com.github.rodrigotimoteo.animally.domain.insights.model.InsightsDataIssueCount
import com.github.rodrigotimoteo.animally.domain.insights.model.InsightsDataIssueType
import com.github.rodrigotimoteo.animally.domain.insights.model.InsightsDrillDown
import com.github.rodrigotimoteo.animally.domain.insights.model.InsightsFilter
import com.github.rodrigotimoteo.animally.domain.insights.model.InsightsRecordRef
import com.github.rodrigotimoteo.animally.domain.insights.model.ReproductionMetrics
import kotlinx.datetime.LocalDate

/**
 * Repository contract for Insights aggregate facts.
 *
 * Implementations aggregate via SQLDelight `UNION ALL` over active dated tables,
 * applying identical inclusive date and patient filters to every branch and
 * requiring `Patient.isActive = 1` and `isActive = 1` on source rows.
 * Medication rows without a `startDate` never contribute.
 *
 * All range parameters are validated before reaching this interface:
 * [InsightsFilter] and [InsightsDrillDown] throw [IllegalArgumentException] for
 * invalid ranges, so invalid periods cannot reach the database.
 *
 * No formatting strings are returned; callers compute rates, shares and
 * percentages deterministically, representing unavailable values as `null`.
 */
interface IInsightsRepository {
    /**
     * Earliest activity date for the given scope, or `null` when empty.
     *
     * Used to resolve the all-time range from the earliest activity through the
     * captured `today`. An empty database produces an empty state, not an invented range.
     *
     * @param patientId optional patient scope; null means global.
     * @return earliest date or `null`.
     */
    fun getEarliestActivityDate(patientId: Long?): LocalDate?

    /**
     * Grouped activity rows for the validated [filter].
     *
     * Each row is counted exactly once per included source type; excluded types
     * (gestation longitudinal rows, anamnese, custom reminders, follicle children)
     * never contribute.
     *
     * @param filter validated inclusive range and optional patient scope.
     * @return grouped rows (date, patient, type, count).
     */
    fun getActivityBuckets(filter: InsightsFilter): List<InsightsActivityBucket>

    /**
     * Source record references matching the validated [drillDown].
     *
     * @param drillDown validated inclusive range, optional patient and record-type filter.
     * @return underlying record refs ordered by date descending.
     */
    fun getRecordRefs(drillDown: InsightsDrillDown): List<InsightsRecordRef>

    /**
     * Reproduction period facts for validated [filter].
     *
     * Canonicalises [com.github.rodrigotimoteo.animally.domain.reproduction.model.ReproductionEventType]
     * (unknown -> Other), excludes soft-deleted/out-of-range/inactive-patient rows,
     * returns explicit sample counts and null averages when denominators zero.
     * No clinical outcome or success rate is derived.
     *
     * @param filter validated inclusive range and optional patient scope.
     * @return deterministic reproduction metrics for the period.
     */
    fun getReproductionMetrics(filter: InsightsFilter): ReproductionMetrics

    /**
     * Current operational gestation snapshot as of [today].
     *
     * Independent from the historical [InsightsFilter] range; filtered only by
     * optional patient scope. Excludes inactive rows, inactive patients and
     * resolved statuses (Completed/Failed/Foaled, case-insensitive). Gestation
     * day and due date are recalculated from breedingDate with a 340-day period,
     * not trusted from stored values. `daysUntilDue` is negative for overdue
     * rows. Due-soon groups are 0..30/0..60/0..90 inclusive, sorted by due date.
     *
     * @param patientId optional scope; null means global.
     * @param today captured today for deterministic day/due calculation.
     * @return operational snapshot with active and due-soon groups.
     */
    fun getCurrentCareSnapshot(
        patientId: Long?,
        today: LocalDate,
    ): CurrentCareSnapshot

    /**
     * Research-readiness issue counts for validated [filter].
     *
     * Each [InsightsDataIssueType] maps to a concrete validation rule with explicit
     * definition (see [InsightsDataIssueType.definition]) and drills to affected rows.
     * Counts respect inclusive date range, patient scope, soft-delete (`isActive = 1`)
     * and active patient (`Patient.isActive = 1`). Language surfaces as
     * "missing for analysis", not clinically wrong. No combined quality score.
     *
     * @param filter validated inclusive range and optional patient scope.
     * @return counts per issue type (zero counts may be omitted or zero).
     */
    fun getDataIssueCounts(filter: InsightsFilter): List<InsightsDataIssueCount>

    /**
     * Drill-down refs for a specific readiness issue within [filter].
     *
     * @param filter validated inclusive range and optional patient scope.
     * @param issueType readiness rule to filter.
     * @return affected record refs ordered by date descending, each navigable via `RecordDetailKey`.
     */
    fun getDataIssueRecordRefs(
        filter: InsightsFilter,
        issueType: InsightsDataIssueType,
    ): List<InsightsRecordRef>
}
