package com.github.rodrigotimoteo.animally.llm

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus

/**
 * Inclusive date window resolved from a relative question. The end is always
 * bounded by the supplied [today], so future-dated records cannot answer a
 * question about the current period.
 */
data class RagDateRange(
    val from: LocalDate,
    val to: LocalDate,
) {
    fun contains(date: LocalDate): Boolean = date >= from && date <= to
}

/** Resolves the small set of relative periods understood by the assistant. */
object RagDateRangeIntent {
    private val currentMonthRegex =
        Regex("\\b(this|current)\\s+month\\b|\\b(este|neste)\\s+m[eê]s\\b|\\bm[eê]s\\s+atual\\b")
    private val previousMonthRegex =
        Regex("\\b(last|previous)\\s+month\\b|\\b(m[eê]s)\\s+passado\\b")
    private val currentWeekRegex =
        Regex("\\b(this|current)\\s+week\\b|\\b(esta|nesta)\\s+semana\\b|\\bsemana\\s+atual\\b")
    private val todayRegex = Regex("\\btoday\\b|\\bhoje\\b")
    private val yesterdayRegex = Regex("\\byesterday\\b|\\bontem\\b")
    private val currentYearRegex =
        Regex("\\b(this|current)\\s+year\\b|\\b(este|neste)\\s+ano\\b|\\bano\\s+atual\\b")

    /** Returns an inclusive range, or null when the query has no supported period. */
    fun resolve(
        query: String,
        today: LocalDate,
    ): RagDateRange? {
        val normalized = query.lowercase()
        return when {
            currentMonthRegex.containsMatchIn(normalized) ->
                RagDateRange(startOfMonth(today), today)
            previousMonthRegex.containsMatchIn(normalized) -> {
                val currentMonth = startOfMonth(today)
                val previousMonthEnd = currentMonth.minus(DatePeriod(days = 1))
                RagDateRange(startOfMonth(previousMonthEnd), previousMonthEnd)
            }
            currentWeekRegex.containsMatchIn(normalized) ->
                RagDateRange(
                    today.minus(DatePeriod(days = today.dayOfWeek.isoDayNumber - 1)),
                    today,
                )
            todayRegex.containsMatchIn(normalized) -> RagDateRange(today, today)
            yesterdayRegex.containsMatchIn(normalized) -> {
                val yesterday = today.minus(DatePeriod(days = 1))
                RagDateRange(yesterday, yesterday)
            }
            currentYearRegex.containsMatchIn(normalized) ->
                RagDateRange(LocalDate(today.year, 1, 1), today)
            else -> null
        }
    }

    private fun startOfMonth(date: LocalDate): LocalDate = LocalDate(date.year, date.month.ordinal + 1, 1)
}

/** The unambiguous “what happened in a period?” shape is answered from rows. */
object RecentActivityIntent {
    private val activityRegex =
        Regex(
            "\\b(what\\s+(happened|occurred|was\\s+recorded)|recent\\s+activity|activity\\s+log)\\b|" +
                "\\b(o\\s+que\\s+(aconteceu|ocorreu)|atividade\\s+recente|registos?\\s+recentes?)\\b",
        )

    fun matches(
        query: String,
        dateRange: RagDateRange?,
    ): Boolean = dateRange != null && activityRegex.containsMatchIn(query.lowercase())
}
