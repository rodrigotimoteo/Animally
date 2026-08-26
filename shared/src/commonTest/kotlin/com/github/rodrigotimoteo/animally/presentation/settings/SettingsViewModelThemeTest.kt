package com.github.rodrigotimoteo.animally.presentation.settings

import com.github.rodrigotimoteo.animally.presentation.theme.AccentColor
import com.github.rodrigotimoteo.animally.presentation.theme.ThemeMode
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for [ThemePreferenceStore] behavior and theme mode persistence.
 *
 * Uses an in-memory fake to verify the contract without platform dependencies.
 */
class SettingsViewModelThemeTest {
    private val store = FakeThemePreferenceStore()

    @Test
    fun `default theme mode is SYSTEM when store is empty`() {
        assertEquals(ThemeMode.SYSTEM, store.getThemeMode())
    }

    @Test
    fun `setThemeMode persists the chosen mode`() {
        store.setThemeMode(ThemeMode.DARK)
        assertEquals(ThemeMode.DARK, store.getThemeMode())
    }

    @Test
    fun `setThemeMode can transition between all modes`() {
        store.setThemeMode(ThemeMode.LIGHT)
        assertEquals(ThemeMode.LIGHT, store.getThemeMode())

        store.setThemeMode(ThemeMode.DARK)
        assertEquals(ThemeMode.DARK, store.getThemeMode())

        store.setThemeMode(ThemeMode.SYSTEM)
        assertEquals(ThemeMode.SYSTEM, store.getThemeMode())
    }

    @Test
    fun `setThemeMode with same value is idempotent`() {
        store.setThemeMode(ThemeMode.LIGHT)
        store.setThemeMode(ThemeMode.LIGHT)
        assertEquals(ThemeMode.LIGHT, store.getThemeMode())
    }

    @Test
    fun `default accent is FOREST when store is empty`() {
        assertEquals(AccentColor.FOREST, store.getAccentColor())
    }

    @Test
    fun `setAccentColor persists the chosen stable id`() {
        store.setAccentColor(AccentColor.PLUM)
        assertEquals(AccentColor.PLUM, store.getAccentColor())
    }

    @Test
    fun `ThemeMode entries have correct labels`() {
        assertEquals("Light", ThemeMode.LIGHT.label)
        assertEquals("Dark", ThemeMode.DARK.label)
        assertEquals("System", ThemeMode.SYSTEM.label)
    }
}

/**
 * In-memory [ThemePreferenceStore] for tests.
 */
private class FakeThemePreferenceStore : ThemePreferenceStore {
    private var storedMode: ThemeMode = ThemeMode.SYSTEM
    private var storedAccent: AccentColor = AccentColor.FOREST

    override fun getThemeMode(): ThemeMode = storedMode

    override fun setThemeMode(mode: ThemeMode) {
        storedMode = mode
    }

    override fun getAccentColor(): AccentColor = storedAccent

    override fun setAccentColor(accent: AccentColor) {
        storedAccent = accent
    }
}
