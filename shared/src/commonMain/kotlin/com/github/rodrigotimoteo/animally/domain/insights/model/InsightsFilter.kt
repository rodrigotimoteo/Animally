package com.github.rodrigotimoteo.animally.domain.insights.model

import kotlinx.datetime.LocalDate

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
}
