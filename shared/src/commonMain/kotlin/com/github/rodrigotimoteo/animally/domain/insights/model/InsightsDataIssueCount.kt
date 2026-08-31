package com.github.rodrigotimoteo.animally.domain.insights.model

/**
 * Count for one [InsightsDataIssueType] in the current dataset.
 *
 * @property type issue category.
 * @property count number of affected rows.
 */
data class InsightsDataIssueCount(
    val type: InsightsDataIssueType,
    val count: Int,
)
