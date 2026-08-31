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
import com.github.rodrigotimoteo.animally.domain.insights.model.avgOrNull
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single
import kotlin.time.Clock

@Single
class GetInsightsDashboardUseCase(
    @Provided private val repository: IInsightsRepository,
    private val todayProvider: () -> LocalDate = { Clock.System.todayIn(TimeZone.currentSystemDefault()) },
) {
    operator fun invoke(
        filter: InsightsFilter,
        today: LocalDate = todayProvider(),
    ): InsightsSnapshot = buildSnapshot(filter, isAllTime = false, today = today)

    fun getAllTimeSnapshot(
        patientId: Long?,
        today: LocalDate = todayProvider(),
    ): InsightsSnapshot {
        val currentCare = repository.getCurrentCareSnapshot(patientId, today)
        val earliest = repository.getEarliestActivityDate(patientId)
        if (earliest == null || earliest > today) {
            return InsightsSnapshot(
                OverviewMetrics(0, 0, 0, 0, null, null, null),
                emptyList(),
                emptyList(),
                ReproductionMetrics(emptyList(), 0, 0, null, 0, 0, null, 0),
                currentCare,
                emptyList(),
                null,
            )
        }
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
        return InsightsSnapshot(overview, activitySeries, recordMix, reproduction, currentCare, dataIssues, filter)
    }

    private fun buildOverview(
        currentBuckets: List<InsightsActivityBucket>,
        filter: InsightsFilter,
        isAllTime: Boolean,
    ): OverviewMetrics {
        var activityCount = 0
        val patients = mutableSetOf<Long>()
        val caseDays = mutableSetOf<Pair<Long, LocalDate>>()
        val activeDays = mutableSetOf<LocalDate>()
        for (b in currentBuckets) {
            activityCount += b.count
            patients.add(b.patientId)
            caseDays.add(b.patientId to b.date)
            activeDays.add(b.date)
        }
        val perActive = avgOrNull(activityCount, activeDays.size)
        val perCase = avgOrNull(activityCount, caseDays.size)
        val comparison =
            if (isAllTime) {
                null
            } else {
                val previousFilter = filter.comparisonRange()
                val previousCount = repository.getActivityBuckets(previousFilter).sumOf { it.count }
                MetricComparison.from(current = activityCount, previous = previousCount)
            }
        return OverviewMetrics(patients.size, activityCount, caseDays.size, activeDays.size, perActive, perCase, comparison)
    }

    private fun buildRecordMix(buckets: List<InsightsActivityBucket>): List<RecordTypeCount> {
        val total = buckets.sumOf { it.count }
        if (total == 0) return emptyList()
        return buckets
            .groupBy { it.recordType }
            .map { (type, list) ->
                val cnt = list.sumOf { it.count }
                RecordTypeCount(type, cnt, avgOrNull(cnt, total))
            }.sortedWith(compareByDescending<RecordTypeCount> { it.count }.thenBy { it.type.wireName })
    }
}
