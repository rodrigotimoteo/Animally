package com.github.rodrigotimoteo.animally.domain.insights.model

import com.github.rodrigotimoteo.animally.domain.common.RecordType

/**
 * Count and share for one [RecordType] within a period.
 *
 * @property type the record type.
 * @property count number of activities of this type.
 * @property share fraction of total activities, or `null` when total is zero.
 */
data class RecordTypeCount(
    val type: RecordType,
    val count: Int,
    val share: Double?,
)
