package com.github.rodrigotimoteo.animally.domain.insights.model

/**
 * Current operational snapshot independent of the historical [InsightsFilter] range.
 *
 * Uses the single injected `today` captured for the load. Not constrained by
 * the historical period filter.
 *
 * @property activeGestations all active, unresolved gestations as of today.
 */
data class CurrentCareSnapshot(
    val activeGestations: List<CurrentGestationItem>,
) {
    fun dueSoon(days: Int): List<CurrentGestationItem> = activeGestations.filter { it.isDueSoon(days) }

    val dueSoon30: List<CurrentGestationItem> get() = dueSoon(DUE_SOON_30_DAYS)
    val dueSoon60: List<CurrentGestationItem> get() = dueSoon(DUE_SOON_60_DAYS)
    val dueSoon90: List<CurrentGestationItem> get() = dueSoon(DUE_SOON_90_DAYS)

    companion object {
        const val DUE_SOON_30_DAYS = 30
        const val DUE_SOON_60_DAYS = 60
        const val DUE_SOON_90_DAYS = 90
    }
}
