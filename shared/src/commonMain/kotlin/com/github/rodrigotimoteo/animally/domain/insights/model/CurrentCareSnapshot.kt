package com.github.rodrigotimoteo.animally.domain.insights.model

/**
 * Current operational snapshot independent of the historical [InsightsFilter] range.
 *
 * Uses the single injected `today` captured for the load. Not constrained by
 * the historical period filter.
 *
 * @property activeGestations all active, unresolved gestations as of today.
 * @property dueSoon30 gestations due in 0..30 days inclusive.
 * @property dueSoon60 gestations due in 0..60 days inclusive.
 * @property dueSoon90 gestations due in 0..90 days inclusive.
 */
data class CurrentCareSnapshot(
    val activeGestations: List<CurrentGestationItem>,
    val dueSoon30: List<CurrentGestationItem> = emptyList(),
    val dueSoon60: List<CurrentGestationItem> = emptyList(),
    val dueSoon90: List<CurrentGestationItem> = emptyList(),
)
