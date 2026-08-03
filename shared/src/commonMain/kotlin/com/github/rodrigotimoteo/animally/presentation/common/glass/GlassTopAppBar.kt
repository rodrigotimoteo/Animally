package com.github.rodrigotimoteo.animally.presentation.common.glass

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeColorEffect
import dev.chrisbanes.haze.blur.blurEffect
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource

/**
 * Remembers a shared [HazeState] for the current composition.
 *
 * The returned state should be passed to both [GlassTopAppBar] (the effect surface) and
 * to the content below via [Modifier.hazeSourceFrom] so the app bar can blur what sits
 * underneath it.
 */
@Composable
fun rememberHazeState(): HazeState = remember { HazeState() }

/**
 * CompositionLocal carrying the shared [HazeState] for the current screen.
 *
 * Provided at the screen root next to [rememberHazeState] and consumed by content
 * composables that need to register as a haze source. Keeping the state in a local
 * avoids threading [HazeState] through every composable's parameter list.
 */
val LocalHazeState =
    staticCompositionLocalOf<HazeState> {
        error("LocalHazeState not provided — call rememberHazeState() at the screen root")
    }

/**
 * Returns a modifier that registers this composable as a haze source for the given [state].
 *
 * Apply this to the scrolling content behind the app bar so the glass effect has pixels
 * to blur.
 */
fun Modifier.hazeSourceFrom(state: HazeState): Modifier = this.hazeSource(state = state)

/**
 * A translucent top app bar with a glass-blur haze effect.
 *
 * Renders a standard Material 3 [TopAppBar] and overlays a haze effect that blurs whatever
 * is registered as a source via [hazeSourceFrom]. The tint uses the current theme's surface
 * color at low alpha so the bar reads as frosted glass over the content.
 *
 * @param title Title content, forwarded to [TopAppBar].
 * @param hazeState State shared with the content below via [hazeSourceFrom].
 * @param modifier Optional modifier.
 * @param navigationIcon Leading icon slot.
 * @param actions Trailing actions slot.
 * @param scrollBehavior Optional scroll behavior forwarded to [TopAppBar].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlassTopAppBar(
    title: @Composable () -> Unit,
    hazeState: HazeState,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable () -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    val surfaceColor = MaterialTheme.colorScheme.surface
    val tintAlpha = if (isDarkTheme()) GLASS_TINT_ALPHA_DARK else GLASS_TINT_ALPHA_LIGHT

    val appBarModifier =
        modifier
            .fillMaxWidth()
            .hazeEffect(state = hazeState) {
                blurEffect {
                    blurRadius = 20.dp
                    colorEffects = listOf(HazeColorEffect.tint(surfaceColor.copy(alpha = tintAlpha)))
                }
            }

    TopAppBar(
        title = title,
        navigationIcon = { navigationIcon() },
        actions = { actions() },
        scrollBehavior = scrollBehavior,
        modifier = appBarModifier,
    )
}

@Composable
private fun isDarkTheme(): Boolean {
    val background = MaterialTheme.colorScheme.background
    return background.luminance() < LUMINANCE_THRESHOLD
}

private fun Color.luminance(): Float {
    val r = red.toFloat()
    val g = green.toFloat()
    val b = blue.toFloat()
    return LUMINANCE_RED_COEFFICIENT * r + LUMINANCE_GREEN_COEFFICIENT * g + LUMINANCE_BLUE_COEFFICIENT * b
}

private const val GLASS_TINT_ALPHA_DARK = 0.72f
private const val GLASS_TINT_ALPHA_LIGHT = 0.78f
private const val LUMINANCE_THRESHOLD = 0.5f
private const val LUMINANCE_RED_COEFFICIENT = 0.299f
private const val LUMINANCE_GREEN_COEFFICIENT = 0.587f
private const val LUMINANCE_BLUE_COEFFICIENT = 0.114f
