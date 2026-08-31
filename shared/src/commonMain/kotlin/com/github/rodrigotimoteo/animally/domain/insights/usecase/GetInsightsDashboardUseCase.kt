package com.github.rodrigotimoteo.animally.domain.insights.usecase

import com.github.rodrigotimoteo.animally.domain.insights.IInsightsRepository
import com.github.rodrigotimoteo.animally.domain.insights.model.CurrentCareSnapshot
import com.github.rodrigotimoteo.animally.domain.insights.model.InsightsActivityBucket
import com.github.rodrigotimoteo.animally.domain.insights.model.InsightsFilter
import com.github.rodrigotimoteo.animally.domain.insights.model.InsightsSnapshot
import com.github.rodrigotimoteo.animally.domain.insights.model.MetricComparison
import com.github.rodrigotimoteo.animally.domain.insights.model.OverviewMetrics
import com.github.rodrigotimoteo.animally.domain.insights.model.RecordTypeCount
import com.github.rodrigotimoteo.animally.domain.insights.model.ReproductionMetrics
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single
import kotlin.time.Clock

/**
 * Deterministic dashboard use case for internship overview.
 *
 * Calculates patients seen, recorded activities, case-days, active days, safe averages,
 * record mix shares, activity trend buckets, reproduction period facts and the current
 * gestation snapshot from repository facts. All date ranges are inclusive; comparison
 * is the N days ending at `from - 1`. All-time resolves via earliest activity through
 * injected [todayProvider]; empty DB produces an empty snapshot without inventing a
 * range. Zero denominators yield `null`. Reproduction metrics report canonical event
 * counts (unknown -> Other), embryo/ICSI sums and averages with explicit denominators,
 * and ultrasound counts. No success or outcome rate. Current gestation is captured
 * from [today] independently of the historical range, recalculating gestation day
 * from breedingDate while respecting persisted expectedDueDate for due-soon
 * 30/60/90-day groups inclusive.
 *
 * @param repository aggregate facts via `WITH activity_rows AS (UNION ALL ...)` over 17 dated tables
 * and sargable reproduction aggregates plus readiness checks.
 * @param todayProvider injected today for deterministic all-time and tests.
 */
@Single
class GetInsightsDashboardUseCase(
    @Provided private val repository: IInsightsRepository,
    private val todayProvider: () -> LocalDate =
        { Clock.System.todayIn(TimeZone.currentSystemDefault()) },
) {
    /**
     * Builds snapshot for validated finite [filter] with comparison.
     *
     * @param filter validated inclusive range and optional patient scope.
     * @param today captured today for gestation snapshot; defaults to [todayProvider] for callers not
     * managing midnight boundary (VM passes its single capture).
     * @return deterministic snapshot.
     */
    operator fun invoke(
        filter: InsightsFilter,
        today: LocalDate = todayProvider(),
    ): InsightsSnapshot = buildSnapshot(filter, isAllTime = false, today = today)

    /**
     * Builds all-time snapshot for optional [patientId] scope.
     *
     * Resolves earliest activity through captured today. Empty DB returns empty
     * overview but still includes the current gestation snapshot (independent of
     * period). Comparison always `null` for all-time.
     *
     * @param patientId optional scope; null means global.
     * @param today captured today for current-care and all-time range; defaults to [todayProvider].
     * @return all-time snapshot.
     */
    fun getAllTimeSnapshot(
        patientId: Long?,
        today: LocalDate = todayProvider(),
    ): InsightsSnapshot {
        val currentCare = repository.getCurrentCareSnapshot(patientId, today)
        val earliest = repository.getEarliestActivityDate(patientId)
        if (earliest == null) return emptySnapshot(currentCare)
        // Future-dated rows are outside an all-time range ending today. Avoid
        // constructing an invalid filter when they are the only persisted activity.
        if (earliest > today) return emptySnapshot(currentCare)
        val filter = InsightsFilter(from = earliest, to = today, patientId = patientId)
        return buildSnapshot(filter, isAllTime = true, today = today, preFetchedCare = currentCare)
    }

    private fun buildSnapshot(
        filter: InsightsFilter,
        isAllTime: Boolean,
        today: LocalDate,
        preFetchedCare: CurrentCareSnapshot? = null,
    ): InsightsSnapshot {
        val currentBuckets = repository.getActivityBuckets(filter)
        val overview = buildOverview(currentBuckets, filter, isAllTime)
        val recordMix = buildRecordMix(currentBuckets)
        val activitySeries = InsightsBucketing.buildSeries(currentBuckets, filter.from, filter.to)
        val reproduction = repository.getReproductionMetrics(filter)
        val currentCare = preFetchedCare ?: repository.getCurrentCareSnapshot(filter.patientId, today)
        val dataIssues = repository.getDataIssueCounts(filter)
        return InsightsSnapshot(
            overview = overview,
            activitySeries = activitySeries,
            recordMix = recordMix,
            reproduction = reproduction,
            currentCare = currentCare,
            dataIssues = dataIssues,
            appliedFilter = filter,
        )
    }

    private fun buildOverview(
        currentBuckets: List<InsightsActivityBucket>,
        filter: InsightsFilter,
        isAllTime: Boolean,
    ): OverviewMetrics {
        val activityCount = currentBuckets.sumOf { it.count }
        val patientCount = currentBuckets.map { it.patientId }.toSet().size
        val caseDayCount = currentBuckets.map { it.patientId to it.date }.toSet().size
        val activeDayCount = currentBuckets.map { it.date }.toSet().size
        val (perActive, perCase) = OverviewMetrics.averages(activityCount, caseDayCount, activeDayCount)
        val comparison: MetricComparison? =
            if (isAllTime) {
                null
            } else {
                val previousFilter = filter.comparisonRange()
                val previousBuckets = repository.getActivityBuckets(previousFilter)
                val previousCount = previousBuckets.sumOf { it.count }
                MetricComparison.from(current = activityCount, previous = previousCount)
            }
        return OverviewMetrics(
            patientCount = patientCount,
            activityCount = activityCount,
            caseDayCount = caseDayCount,
            activeDayCount = activeDayCount,
            averagePerActiveDay = perActive,
            averagePerCaseDay = perCase,
            comparison = comparison,
        )
    }

    private fun buildRecordMix(buckets: List<InsightsActivityBucket>): List<RecordTypeCount> {
        val total = buckets.sumOf { it.count }
        if (total == 0) return emptyList()
        return buckets
            .groupBy { it.recordType }
            .map { (type, list) ->
                val cnt = list.sumOf { it.count }
                RecordTypeCount(type = type, count = cnt, share = RecordTypeCount.share(cnt, total))
            }.sortedWith(compareByDescending<RecordTypeCount> { it.count }.thenBy { it.type.wireName })
    }

    private fun emptyReproduction(): ReproductionMetrics =
        ReproductionMetrics(
            eventCounts = emptyList(),
            embryoCollections = 0,
            embryosCollected = 0,
            averageEmbryosPerCollection = null,
            icsiSessions = 0,
            folliclesRecovered = 0,
            averageFolliclesPerIcsi = null,
            ultrasoundCount = 0,
        )

    private fun emptySnapshot(currentCare: CurrentCareSnapshot = CurrentCareSnapshot(activeGestations = emptyList())): InsightsSnapshot =
        InsightsSnapshot(
            overview =
                OverviewMetrics(
                    patientCount = 0,
                    activityCount = 0,
                    caseDayCount = 0,
                    activeDayCount = 0,
                    averagePerActiveDay = null,
                    averagePerCaseDay = null,
                    comparison = null,
                ),
            activitySeries = emptyList(),
            recordMix = emptyList(),
            reproduction = emptyReproduction(),
            currentCare = currentCare,
            dataIssues = emptyList(),
            appliedFilter = null,
        )
}
