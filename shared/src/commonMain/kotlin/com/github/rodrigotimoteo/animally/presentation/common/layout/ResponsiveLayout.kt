package com.github.rodrigotimoteo.animally.presentation.common.layout

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp

/**
 * Classifies a width in dp into a [WindowSizeClass].
 *
 * Pure function exposed for unit testing — the Compose helper [rememberWindowSizeClass]
 * delegates to this for classification.
 *
 * @param widthDp Width in density-independent pixels.
 */
fun windowSizeClass(widthDp: Int): WindowSizeClass =
    when {
        widthDp < COMPACT_MAX_WIDTH_DP -> WindowSizeClass.Compact
        widthDp < MEDIUM_MAX_WIDTH_DP -> WindowSizeClass.Medium
        else -> WindowSizeClass.Expanded
    }

/**
 * Returns the current [WindowSizeClass] based on the parent's max width constraint.
 *
 * Uses [BoxWithConstraints] to read the available width and classify it. Intended to be called
 * near the top of a screen composable so child composables can branch on size class.
 *
 * @param content Composable receiving the resolved size class.
 */
@Composable
fun <T> withWindowSizeClass(content: @Composable (WindowSizeClass) -> T): T {
    var result: T? = null
    BoxWithConstraints {
        val widthDp = maxWidth.value.toInt()
        result = content(windowSizeClass(widthDp))
    }
    @Suppress("UNCHECKED_CAST")
    return result as T
}

/**
 * Returns the max width constraint as a [Dp].
 *
 * Convenience wrapper around [BoxWithConstraints] for callers that need the raw width
 * rather than a classified bucket.
 */
@Composable
fun withMaxWidth(content: @Composable (Dp) -> Unit) {
    BoxWithConstraints {
        content(maxWidth)
    }
}

private const val COMPACT_MAX_WIDTH_DP = 600
private const val MEDIUM_MAX_WIDTH_DP = 840
