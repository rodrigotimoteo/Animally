@file:Suppress("ktlint:standard:filename")

package com.github.rodrigotimoteo.animally.presentation.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable

/** Desktop has no dynamic color — always falls back to the static palette. */
@Composable
actual fun dynamicColorScheme(darkTheme: Boolean): ColorScheme? = null
