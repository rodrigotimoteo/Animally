package com.github.rodrigotimoteo.animally.presentation.common.layout

import androidx.compose.runtime.Immutable

/**
 * Coarse window size buckets used for responsive layout decisions.
 *
 * Thresholds follow the Material 3 window size class guidance:
 * - [Compact] — width < 600dp (most phones portrait).
 * - [Medium] — 600dp <= width < 840dp (tablets portrait, large foldables).
 * - [Expanded] — width >= 840dp (tablets landscape, desktop).
 */
@Immutable
enum class WindowSizeClass {
    Compact,
    Medium,
    Expanded,
}
