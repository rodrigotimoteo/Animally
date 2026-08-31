package com.github.rodrigotimoteo.animally.domain.insights.usecase

import com.github.rodrigotimoteo.animally.domain.insights.model.ActivityPoint
import com.github.rodrigotimoteo.animally.domain.insights.model.InsightsActivityBucket
import com.github.rodrigotimoteo.animally.domain.insights.model.InsightsFilter
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/**
 * Granularity for the activity trend series.
 *
 * Daily through 45 days inclusive, weekly for 46..180, monthly above 180.
 * Thresholds are inclusive on the upper bound so 45 → daily, 46 → weekly, 181 → monthly.
 */
enum class BucketGranularity {
    DAILY,
    WEEKLY,
    MONTHLY,
}

/**
 * Date-bucketing helper for Insights activity trends.
 *
 * All buckets are inclusive and based on calendar boundaries:
 * - Daily: the record date itself.
 * - Weekly: ISO Monday of the record's calendar week.
 * - Monthly: first day of the record's calendar month.
 */
object InsightsBucketing {
    private const val DAILY_MAX_DAYS = 45
    private const val WEEKLY_MAX_DAYS = 180

    /**
     * Resolves bucket granularity for an inclusive range.
     *
     * @param from inclusive start.
     * @param to inclusive end, must be >= [from].
     * @return granularity for the range.
     */
    fun granularity(
        from: LocalDate,
        to: LocalDate,
    ): BucketGranularity {
        val daysInclusive = from.daysUntil(to) + 1
        return when {
            daysInclusive <= DAILY_MAX_DAYS -> BucketGranularity.DAILY
            daysInclusive <= WEEKLY_MAX_DAYS -> BucketGranularity.WEEKLY
            else -> BucketGranularity.MONTHLY
        }
    }

    /**
     * Calendar-week bucket start (Monday) for [date].
     *
     * @param date record date.
     * @return Monday of the ISO week containing [date].
     */
    fun weekStart(date: LocalDate): LocalDate = date.minus(DatePeriod(days = date.dayOfWeek.isoDayNumber - 1))

    /**
     * Calendar-month bucket start for [date].
     *
     * @param date record date.
     * @return first day of the calendar month containing [date].
     */
    fun monthStart(date: LocalDate): LocalDate = LocalDate(date.year, date.month.ordinal + 1, 1)

    /**
     * Bucket start for [date] under [granularity].
     *
     * @param date record date.
     * @param granularity desired bucket size.
     * @return inclusive start of the bucket.
     */
    fun bucketStart(
        date: LocalDate,
        granularity: BucketGranularity,
    ): LocalDate =
        when (granularity) {
            BucketGranularity.DAILY -> date
            BucketGranularity.WEEKLY -> weekStart(date)
            BucketGranularity.MONTHLY -> monthStart(date)
        }

    /**
     * Builds the activity trend series for the given buckets.
     *
     * Buckets are grouped by their calendar bucket start and summed. The result is
     * sorted ascending by period start and contains only buckets with at least one
     * activity; empty periods are not emitted as zero entries. This keeps the series
     * deterministic and avoids inventing activity where no row exists.
     *
     * @param buckets grouped rows from the repository for the current filter.
     * @param from inclusive range start (used only for granularity selection).
     * @param to inclusive range end (used only for granularity selection).
     * @return activity points sorted by period start.
     */
    fun buildSeries(
        buckets: List<InsightsActivityBucket>,
        from: LocalDate,
        to: LocalDate,
    ): List<ActivityPoint> {
        if (buckets.isEmpty()) return emptyList()
        val gran = granularity(from, to)
        return buckets
            .groupBy { bucketStart(it.date, gran) }
            .map { (start, list) ->
                ActivityPoint(
                    periodStart = start,
                    count = list.sumOf { it.count },
                )
            }.sortedBy { it.periodStart }
    }

    /**
     * Convenience overload taking a validated filter.
     *
     * @param filter validated inclusive range defining granularity.
     * @param buckets repository rows.
     * @return series.
     */
    fun buildSeries(
        filter: InsightsFilter,
        buckets: List<InsightsActivityBucket>,
    ): List<ActivityPoint> = buildSeries(buckets, filter.from, filter.to)

    /**
     * Inclusive day count for [filter].
     *
     * @param filter validated inclusive range.
     * @return number of days inclusive.
     */
    fun inclusiveDayCount(filter: InsightsFilter): Int = filter.from.daysUntil(filter.to) + 1

    /**
     * Comparison filter for [filter] using inclusive semantics.
     *
     * @param filter current period.
     * @return preceding equal-length inclusive filter.
     */
    fun comparisonFilter(filter: InsightsFilter): InsightsFilter = filter.comparisonRange()
}

/**
 * Computes the comparison filter for an inclusive range.
 *
 * For an inclusive range of N days [from]..[to], the comparison range is the
 * N days ending on [from] minus one day, so the two ranges are exact,
 * non-overlapping and inclusive.
 *
 * @return filter for the immediately preceding equal-length period with same patient scope.
 */
fun com.github.rodrigotimoteo.animally.domain.insights.model.InsightsFilter.comparisonRange():
    com.github.rodrigotimoteo.animally.domain.insights.model.InsightsFilter {
    val daysInclusive = from.daysUntil(to) + 1
    val comparisonTo = from.minus(DatePeriod(days = 1))
    val comparisonFrom = comparisonTo.minus(DatePeriod(days = daysInclusive - 1))
    return com.github.rodrigotimoteo.animally.domain.insights.model.InsightsFilter(
        from = comparisonFrom,
        to = comparisonTo,
        patientId = patientId,
    )
}
