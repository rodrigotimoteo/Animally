package com.github.rodrigotimoteo.animally.domain.insights.model

import kotlinx.datetime.LocalDate

/**
 * One bucket in the activity trend series.
 *
 * @property periodStart inclusive start of the bucket (day, week Monday, or month first day).
 * @property count activity count in the bucket.
 */
data class ActivityPoint(
    val periodStart: LocalDate,
    val count: Int,
)
