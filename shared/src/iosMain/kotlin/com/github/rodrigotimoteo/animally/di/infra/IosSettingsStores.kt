@file:OptIn(ExperimentalObjCName::class)

package com.github.rodrigotimoteo.animally.di.infra

import com.github.rodrigotimoteo.animally.presentation.assistant.AssistantViewModel
import com.github.rodrigotimoteo.animally.presentation.coggins.CogginsViewModel
import com.github.rodrigotimoteo.animally.presentation.ios.AssistantStore
import com.github.rodrigotimoteo.animally.presentation.ios.CogginsStore
import com.github.rodrigotimoteo.animally.presentation.ios.ReminderSettingsStore
import com.github.rodrigotimoteo.animally.presentation.ios.SearchStore
import com.github.rodrigotimoteo.animally.presentation.ios.SettingsStore
import com.github.rodrigotimoteo.animally.presentation.ios.TimelineStore
import com.github.rodrigotimoteo.animally.presentation.reminder.ReminderSettingsViewModel
import com.github.rodrigotimoteo.animally.presentation.search.SearchViewModel
import com.github.rodrigotimoteo.animally.presentation.settings.SettingsViewModel
import com.github.rodrigotimoteo.animally.presentation.settings.createPlatformThemePreferenceStore
import com.github.rodrigotimoteo.animally.presentation.timeline.TimelineViewModel
import org.koin.core.parameter.parametersOf
import kotlin.experimental.ExperimentalObjCName
import kotlin.native.ObjCName

/**
 * Swift-facing factories for the settings-related stores. Split from
 * [IosAppBridge] to stay within the detekt function-count budget.
 */
@ObjCName("IosSettingsStores")
object IosSettingsStores {
    /**
     * Returns a store exposing the settings screen.
     *
     * [SettingsViewModel] is constructed directly because its [ThemePreferenceStore]
     * dependency is provided by [ThemeModule], which is not part of the Koin
     * component scan.
     */
    fun settingsStore(): SettingsStore {
        val viewModel =
            SettingsViewModel(
                exportCsvUseCase = IosAppBridge.koin.get(),
                exportBackupUseCase = IosAppBridge.koin.get(),
                restoreBackupUseCase = IosAppBridge.koin.get(),
                exportReportUseCase = IosAppBridge.koin.get(),
                patientRepository = IosAppBridge.koin.get(),
                themePreferenceStore = createPlatformThemePreferenceStore(),
                animallyNavigator = IosAppBridge.koin.get(),
            )
        return SettingsStore(viewModel)
    }

    /** Returns a store exposing the global search screen. */
    fun searchStore(): SearchStore {
        val viewModel: SearchViewModel = IosAppBridge.koin.get()
        return SearchStore(viewModel)
    }

    /**
     * Returns a store exposing the AI assistant screen. The view model is constructed
     * directly because [com.github.rodrigotimoteo.animally.llm.llmModule] provides its
     * dependencies outside the Koin component scan.
     */
    fun assistantStore(): AssistantStore {
        val viewModel =
            AssistantViewModel(
                generateRagResponse = IosAppBridge.koin.get(),
                llmEngine = IosAppBridge.koin.get(),
            )
        return AssistantStore(viewModel)
    }

    /**
     * Returns a store exposing the timeline for the patient with [patientId],
     * or the global timeline when `null`.
     */
    fun timelineStore(patientId: Long?): TimelineStore {
        val viewModel: TimelineViewModel = IosAppBridge.koin.get { parametersOf(patientId) }
        return TimelineStore(viewModel)
    }

    /** Returns a store exposing the reminders section of the settings screen. */
    fun reminderSettingsStore(): ReminderSettingsStore {
        val viewModel: ReminderSettingsViewModel = IosAppBridge.koin.get()
        return ReminderSettingsStore(viewModel)
    }

    /** Returns a store exposing the Coggins alerts section of the settings screen. */
    fun cogginsStore(): CogginsStore {
        val viewModel: CogginsViewModel = IosAppBridge.koin.get()
        return CogginsStore(viewModel)
    }
}
