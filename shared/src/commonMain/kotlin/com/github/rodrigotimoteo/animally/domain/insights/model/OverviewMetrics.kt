package com.github.rodrigotimoteo.animally.domain.insights.model

/**
 * Internship overview metrics for a validated [InsightsFilter] period.
 *
 * Averages are `null` when denominators are zero, making empty periods
 * explicit. No formatting is applied here; Swift formats for display.
 *
 * @property patientCount distinct patients with at least one activity.
 * @property activityCount total recorded activities in the period.
 * @property caseDayCount distinct (patientId, date) pairs with activity.
 * @property activeDayCount distinct dates with activity.
 * @property averagePerActiveDay [activityCount] / [activeDayCount] or null.
 * @property averagePerCaseDay [activityCount] / [caseDayCount] or null.
 * @property comparison period-over-period delta for activity count, or null when disabled (all-time).
 */
data class OverviewMetrics(
    val patientCount: Int,
    val activityCount: Int,
    val caseDayCount: Int,
    val activeDayCount: Int,
    val averagePerActiveDay: Double?,
    val averagePerCaseDay: Double?,
    val comparison: MetricComparison?,
)
