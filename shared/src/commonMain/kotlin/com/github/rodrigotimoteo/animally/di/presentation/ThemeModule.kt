package com.github.rodrigotimoteo.animally.di.presentation

import com.github.rodrigotimoteo.animally.presentation.settings.ThemePreferenceStore
import com.github.rodrigotimoteo.animally.presentation.settings.createPlatformThemePreferenceStore
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

/**
 * Koin module providing the [ThemePreferenceStore] singleton.
 *
 * The platform-specific implementation is created via [createPlatformThemePreferenceStore].
 */
@Module
class ThemeModule {
    @Single
    fun provideThemePreferenceStore(): ThemePreferenceStore = createPlatformThemePreferenceStore()
}
