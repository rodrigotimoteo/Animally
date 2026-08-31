package com.github.rodrigotimoteo.animally.domain.insights.model

import com.github.rodrigotimoteo.animally.domain.common.RecordType
import kotlinx.datetime.LocalDate

/**
 * One grouped row returned by the activity union before bucketing.
 *
 * Repository returns facts at this granularity; the use case groups them into
 * [ActivityPoint] buckets and computes [RecordTypeCount] shares.
 *
 * @property date record date (inclusive filter key).
 * @property patientId owning patient.
 * @property recordType source type of the counted row.
 * @property count number of rows in the group (always 1 after union grouping before sum, but kept as count for SQL SUM).
 */
data class InsightsActivityBucket(
    val date: LocalDate,
    val patientId: Long,
    val recordType: RecordType,
    val count: Int,
)
