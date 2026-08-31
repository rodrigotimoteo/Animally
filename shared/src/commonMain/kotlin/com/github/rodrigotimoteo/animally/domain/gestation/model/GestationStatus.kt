@file:Suppress("Wrapping")

package com.github.rodrigotimoteo.animally.domain.gestation.model

/**
 * Single source of truth for gestation lifecycle constants.
 *
 * Centralizes the equine gestation period and the resolved-status set so every
 * caller — [com.github.rodrigotimoteo.animally.domain.gestation.usecase.CalculateGestationUseCase],
 * [com.github.rodrigotimoteo.animally.presentation.ios.RecordDetailOpener],
 * insights and care use cases — agrees on what "active" means.
 */
object GestationStatus {
    /** Approximate equine gestation period in days. */
    const val GESTATION_PERIOD_DAYS = 340

    /** Statuses that mark a pregnancy as no longer active. Case-insensitive. */
    val RESOLVED_STATUSES: Set<String> = setOf("Completed", "Failed", "Foaled")
}

/**
 * Whether this status string denotes a resolved (ended) pregnancy.
 *
 * Comparison is case-insensitive so legacy capitalisations all match.
 */
fun String.isResolvedGestationStatus(): Boolean = GestationStatus.RESOLVED_STATUSES.any { equals(it, ignoreCase = true) }

/**
 * Whether this gestation is currently active (ongoing pregnancy).
 *
 * Mirrors [com.github.rodrigotimoteo.animally.presentation.ios.RecordDetailOpener.isGestationActive]
 * — the single source for Swift delegates. Inactive rows or rows whose status is
 * Completed/Failed/Foaled (any capitalisation) are not active.
 */
fun Gestation.isActiveGestation(): Boolean = isActive && !status.isResolvedGestationStatus()
