package com.github.rodrigotimoteo.animally.domain.insights.model

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.minus

/**
 * Validated inclusive date range for Insights queries.
 *
 * @property from inclusive lower bound.
 * @property to inclusive upper bound, never before [from].
 * @property patientId optional scope; null means all active patients.
 */
data class InsightsFilter(
    val from: LocalDate,
    val to: LocalDate,
    val patientId: Long? = null,
) {
    init {
        require(from <= to) {
            "InsightsFilter requires from <= to, got from=$from to=$to"
        }
    }

    fun comparisonRange(): InsightsFilter {
        val daysInclusive = from.daysUntil(to) + 1
        val comparisonTo = from.minus(DatePeriod(days = 1))
        val comparisonFrom = comparisonTo.minus(DatePeriod(days = daysInclusive - 1))
        return InsightsFilter(
            from = comparisonFrom,
            to = comparisonTo,
            patientId = patientId,
        )
    }
}
