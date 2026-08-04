package com.github.rodrigotimoteo.animally.presentation.ui.settings

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.presentation.settings.DesktopThemePreferenceStore
import com.github.rodrigotimoteo.animally.presentation.settings.SettingsViewModel
import com.github.rodrigotimoteo.animally.presentation.settings.view.SettingsScreen
import com.github.rodrigotimoteo.animally.presentation.theme.ThemeMode
import com.github.rodrigotimoteo.animally.presentation.ui.ProvideTestLifecycle
import com.github.rodrigotimoteo.animally.presentation.ui.installMainDispatcher
import com.github.rodrigotimoteo.animally.presentation.ui.restoreMainDispatcher
import com.github.rodrigotimoteo.animally.presentation.ui.seedPatient
import com.github.rodrigotimoteo.animally.presentation.ui.uiTestKoinModules
import org.koin.compose.KoinContext
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import kotlin.test.AfterTest
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class SettingsScreenTest {
    @AfterTest
    fun tearDown() {
        stopKoin()
        restoreMainDispatcher()
    }

    private fun ComposeUiTest.showSettingsScreen(block: ComposeUiTest.() -> Unit) {
        DesktopThemePreferenceStore().setThemeMode(ThemeMode.SYSTEM)
        startKoin { modules(uiTestKoinModules()) }
        val viewModel: SettingsViewModel = GlobalContext.get().get(SettingsViewModel::class)
        setContent {
            KoinContext {
                ProvideTestLifecycle {
                    SettingsScreen(viewModel = viewModel)
                }
            }
        }
        waitUntil(timeoutMillis = 5_000) {
            onAllNodesWithText("Appearance").fetchSemanticsNodes().isNotEmpty()
        }
        block()
    }

    @Test
    fun `when settings open then all sections are visible`() =
        runComposeUiTest {
            installMainDispatcher()
            showSettingsScreen {
                onNodeWithText("Appearance").assertIsDisplayed()
                listOf("Export", "Backup & Restore", "PDF Export", "Cloud Sync", "Reminders").forEach { section ->
                    onNodeWithText(section).assertExists()
                }
                listOf("Light", "Dark", "System").forEach { mode ->
                    onNodeWithContentDescription("Theme $mode").assertExists()
                }
            }
        }

    @Test
    fun `when dark chip tapped then dark theme is selected`() =
        runComposeUiTest {
            installMainDispatcher()
            showSettingsScreen {
                onNodeWithContentDescription("Theme Dark").assertIsNotSelected()

                onNodeWithContentDescription("Theme Dark").performClick()
                waitForIdle()

                onNodeWithContentDescription("Theme Dark").assertIsSelected()
                onNodeWithContentDescription("Theme Light").assertIsNotSelected()
            }
        }

    @Test
    fun `when seeded patient exists then patient pdf button is shown`() =
        runComposeUiTest {
            installMainDispatcher()
            DesktopThemePreferenceStore().setThemeMode(ThemeMode.SYSTEM)
            startKoin { modules(uiTestKoinModules()) }
            val database: AnimallyDatabase = GlobalContext.get().get(AnimallyDatabase::class)
            database.seedPatient(name = "Midnight")
            val viewModel: SettingsViewModel = GlobalContext.get().get(SettingsViewModel::class)

            setContent {
                KoinContext {
                    ProvideTestLifecycle {
                        SettingsScreen(viewModel = viewModel)
                    }
                }
            }

            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText("Midnight").fetchSemanticsNodes().isNotEmpty()
            }
            onNodeWithText("Midnight").assertExists()
        }
}
