package com.github.rodrigotimoteo.animally.presentation.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Curated color palette for Animally — a veterinary equine app.
 *
 * Palette rationale:
 * - Primary (forest green): clinical trust, nature, equine heritage. Deep enough for strong contrast.
 * - Secondary (warm amber): hay, warmth, approachability. Balances the cool primary.
 * - Tertiary (sage): fresh, clean, medical freshness. Bridges primary and secondary.
 * - Semantic colors follow Material 3 conventions with veterinary-appropriate tinting.
 */

private val ForestGreen = Color(0xFF2D5F4E)
private val ForestGreenLight = Color(0xFF4A8B72)
private val ForestGreenDark = Color(0xFF1A3D31)
private val OnForestGreen = Color(0xFFFFFFFF)
private val OnForestGreenDark = Color(0xFFE0F2EA)

private val WarmAmber = Color(0xFFC17817)
private val WarmAmberLight = Color(0xFFE09B3D)
private val WarmAmberDark = Color(0xFF8A5410)
private val OnAmber = Color(0xFFFFFFFF)
private val OnAmberDark = Color(0xFFFFDCB8)

private val Sage = Color(0xFF6B9080)
private val SageLight = Color(0xFF94B8A8)
private val SageDark = Color(0xFF4A6B5C)
private val OnSage = Color(0xFFFFFFFF)
private val OnSageDark = Color(0xFFD4E8DC)

private val CreamBackground = Color(0xFFFAF9F6)
private val SurfaceLight = Color(0xFFFFFFFF)
private val OnSurfaceLight = Color(0xFF1A1C1A)
private val SurfaceVariantLight = Color(0xFFDDE5DD)
private val OnSurfaceVariantLight = Color(0xFF414941)
private val OutlineLight = Color(0xFF717971)

private val NightBackground = Color(0xFF111412)
private val SurfaceDark = Color(0xFF1A1C1A)
private val OnSurfaceDark = Color(0xFFE2E3DE)
private val SurfaceVariantDark = Color(0xFF414941)
private val OnSurfaceVariantDark = Color(0xFFC1C9C1)
private val OutlineDark = Color(0xFF8B938B)

private val ErrorRed = Color(0xFFBA1A1A)
private val ErrorRedDark = Color(0xFFFFB4AB)
private val OnError = Color(0xFFFFFFFF)
private val OnErrorDark = Color(0xFF690005)

private val SuccessGreen = Color(0xFF2E7D32)
private val SuccessGreenDark = Color(0xFF81C784)

private val WarningAmber = Color(0xFFF57C00)
private val WarningAmberDark = Color(0xFFFFB74D)

/** Light Material 3 color scheme for Animally. */
val animallyLightColorScheme =
    lightColorScheme(
        primary = ForestGreen,
        onPrimary = OnForestGreen,
        primaryContainer = ForestGreenLight,
        onPrimaryContainer = OnForestGreen,
        secondary = WarmAmber,
        onSecondary = OnAmber,
        secondaryContainer = WarmAmberLight,
        onSecondaryContainer = OnAmber,
        tertiary = Sage,
        onTertiary = OnSage,
        tertiaryContainer = SageLight,
        onTertiaryContainer = OnSage,
        error = ErrorRed,
        onError = OnError,
        errorContainer = Color(0xFFFFDAD6),
        onErrorContainer = OnErrorDark,
        background = CreamBackground,
        onBackground = OnSurfaceLight,
        surface = SurfaceLight,
        onSurface = OnSurfaceLight,
        surfaceVariant = SurfaceVariantLight,
        onSurfaceVariant = OnSurfaceVariantLight,
        outline = OutlineLight,
        outlineVariant = Color(0xFFBFC7BF),
        inverseSurface = SurfaceDark,
        inverseOnSurface = OnSurfaceDark,
        inversePrimary = ForestGreenLight,
        surfaceTint = ForestGreen,
    )

/** Dark Material 3 color scheme for Animally. */
val animallyDarkColorScheme =
    darkColorScheme(
        primary = ForestGreenLight,
        onPrimary = ForestGreenDark,
        primaryContainer = ForestGreen,
        onPrimaryContainer = OnForestGreenDark,
        secondary = WarmAmberLight,
        onSecondary = WarmAmberDark,
        secondaryContainer = WarmAmber,
        onSecondaryContainer = OnAmberDark,
        tertiary = SageLight,
        onTertiary = SageDark,
        tertiaryContainer = Sage,
        onTertiaryContainer = OnSageDark,
        error = ErrorRedDark,
        onError = OnErrorDark,
        errorContainer = ErrorRed,
        onErrorContainer = Color(0xFFFFDAD6),
        background = NightBackground,
        onBackground = OnSurfaceDark,
        surface = SurfaceDark,
        onSurface = OnSurfaceDark,
        surfaceVariant = SurfaceVariantDark,
        onSurfaceVariant = OnSurfaceVariantDark,
        outline = OutlineDark,
        outlineVariant = Color(0xFF414941),
        inverseSurface = CreamBackground,
        inverseOnSurface = OnSurfaceLight,
        inversePrimary = ForestGreen,
        surfaceTint = ForestGreenLight,
    )

/** Semantic success color — not part of Material 3 but used across the app. */
val successColorLight: Color = SuccessGreen

/** Semantic success color for dark theme. */
val successColorDark: Color = SuccessGreenDark

/** Semantic warning color — not part of Material 3 but used across the app. */
val warningColorLight: Color = WarningAmber

/** Semantic warning color for dark theme. */
val warningColorDark: Color = WarningAmberDark

/** Primary colors for a selectable brand accent. */
internal data class AccentPalette(
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val inversePrimary: Color,
)

private val oceanDarkAccentPalette =
    AccentPalette(
        primary = Color(0xFF8CCBEE),
        onPrimary = Color(0xFF00344D),
        primaryContainer = Color(0xFF145B83),
        onPrimaryContainer = Color(0xFFD0EEFF),
        inversePrimary = Color(0xFF1769AA),
    )

private val oceanLightAccentPalette =
    AccentPalette(
        primary = Color(0xFF1769AA),
        onPrimary = OnForestGreen,
        primaryContainer = Color(0xFFA9D8F5),
        onPrimaryContainer = Color(0xFF00344D),
        inversePrimary = Color(0xFF5C9BC7),
    )

private val plumDarkAccentPalette =
    AccentPalette(
        primary = Color(0xFFEAB3E1),
        onPrimary = Color(0xFF4D1448),
        primaryContainer = Color(0xFF71366A),
        onPrimaryContainer = Color(0xFFFFD7F6),
        inversePrimary = Color(0xFF9B5B91),
    )

private val plumLightAccentPalette =
    AccentPalette(
        primary = Color(0xFF7A3E72),
        onPrimary = OnForestGreen,
        primaryContainer = Color(0xFFE9B6DE),
        onPrimaryContainer = Color(0xFF330B2F),
        inversePrimary = Color(0xFFA15A95),
    )

private val terracottaDarkAccentPalette =
    AccentPalette(
        primary = Color(0xFFFFB59D),
        onPrimary = Color(0xFF541F10),
        primaryContainer = Color(0xFF7B3523),
        onPrimaryContainer = Color(0xFFFFDBD0),
        inversePrimary = Color(0xFFB9684F),
    )

private val terracottaLightAccentPalette =
    AccentPalette(
        primary = Color(0xFF9B4D32),
        onPrimary = OnForestGreen,
        primaryContainer = Color(0xFFFFB59D),
        onPrimaryContainer = Color(0xFF3B0B00),
        inversePrimary = Color(0xFFC8785D),
    )

private val slateDarkAccentPalette =
    AccentPalette(
        primary = Color(0xFFB6C8E5),
        onPrimary = Color(0xFF1F3047),
        primaryContainer = Color(0xFF3A4D68),
        onPrimaryContainer = Color(0xFFD8E2FF),
        inversePrimary = Color(0xFF7890B0),
    )

private val slateLightAccentPalette =
    AccentPalette(
        primary = Color(0xFF4E6078),
        onPrimary = OnForestGreen,
        primaryContainer = Color(0xFFB8C9E3),
        onPrimaryContainer = Color(0xFF0A1B30),
        inversePrimary = Color(0xFF7B91B0),
    )

/**
 * Applies only the brand-facing Material roles, preserving dynamic/system
 * surfaces and semantic error colors supplied by the base scheme.
 */
internal fun ColorScheme.withAccent(
    accent: AccentColor,
    darkTheme: Boolean,
): ColorScheme {
    val palette = accentPalette(accent, darkTheme)
    return copy(
        primary = palette.primary,
        onPrimary = palette.onPrimary,
        primaryContainer = palette.primaryContainer,
        onPrimaryContainer = palette.onPrimaryContainer,
        inversePrimary = palette.inversePrimary,
        surfaceTint = palette.primary,
    )
}

private fun accentPalette(
    accent: AccentColor,
    darkTheme: Boolean,
): AccentPalette =
    when (accent) {
        AccentColor.FOREST ->
            if (darkTheme) {
                AccentPalette(
                    primary = ForestGreenLight,
                    onPrimary = ForestGreenDark,
                    primaryContainer = ForestGreen,
                    onPrimaryContainer = OnForestGreenDark,
                    inversePrimary = ForestGreen,
                )
            } else {
                AccentPalette(
                    primary = ForestGreen,
                    onPrimary = OnForestGreen,
                    primaryContainer = ForestGreenLight,
                    onPrimaryContainer = OnForestGreen,
                    inversePrimary = ForestGreenLight,
                )
            }
        AccentColor.OCEAN ->
            if (darkTheme) oceanDarkAccentPalette else oceanLightAccentPalette
        AccentColor.PLUM ->
            if (darkTheme) plumDarkAccentPalette else plumLightAccentPalette
        AccentColor.TERRACOTTA ->
            if (darkTheme) terracottaDarkAccentPalette else terracottaLightAccentPalette
        AccentColor.SLATE ->
            if (darkTheme) slateDarkAccentPalette else slateLightAccentPalette
    }
