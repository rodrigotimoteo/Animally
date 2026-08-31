package com.github.rodrigotimoteo.animally.domain.insights.model

import com.github.rodrigotimoteo.animally.domain.reproduction.model.ReproductionEventType

/**
 * Count for one canonical [ReproductionEventType] within a period.
 *
 * Unknown raw values are canonicalised to [ReproductionEventType.Other] before counting,
 * so the list always sums to the total reproduction-event activity count.
 *
 * @property type canonical event type.
 * @property count number of events of this type.
 */
data class ReproductionEventCount(
    val type: ReproductionEventType,
    val count: Int,
)
