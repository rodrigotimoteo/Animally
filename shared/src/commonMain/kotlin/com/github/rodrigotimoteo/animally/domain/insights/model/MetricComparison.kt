package com.github.rodrigotimoteo.animally.domain.insights.model

/**
 * Period-over-period comparison for a single integer metric.
 *
 * Share/percentage unavailable states are represented as `null`, never as
 * zero, infinity or NaN, so empty periods are explicit.
 *
 * @property current value in the current period.
 * @property previous value in the immediately preceding equal-length period.
 * @property absoluteDelta [current] minus [previous].
 * @property percentageDelta percentage change over [previous], or `null` when [previous] is zero.
 */
data class MetricComparison(
    val current: Int,
    val previous: Int,
    val absoluteDelta: Int,
    val percentageDelta: Double?,
) {
    companion object {
        private const val PERCENT_SCALE = 100.0

        /**
         * Builds a comparison from two raw counts.
         *
         * @param current value in the current period.
         * @param previous value in the preceding period.
         * @return comparison with `percentageDelta` null when [previous] == 0.
         */
        fun from(
            current: Int,
            previous: Int,
        ): MetricComparison {
            val delta = current - previous
            val pct = if (previous == 0) null else delta.toDouble() / previous.toDouble() * PERCENT_SCALE
            return MetricComparison(
                current = current,
                previous = previous,
                absoluteDelta = delta,
                percentageDelta = pct,
            )
        }
    }
}
