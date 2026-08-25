@file:OptIn(ExperimentalObjCName::class)

package com.github.rodrigotimoteo.animally.presentation.ios

import androidx.lifecycle.viewModelScope
import com.github.rodrigotimoteo.animally.bridge.NativeFlow
import com.github.rodrigotimoteo.animally.domain.patient.model.Patient
import com.github.rodrigotimoteo.animally.llm.cloud.CloudLlmProviderPreset
import com.github.rodrigotimoteo.animally.presentation.settings.SettingsViewModel
import com.github.rodrigotimoteo.animally.presentation.theme.ThemeMode
import kotlin.experimental.ExperimentalObjCName
import kotlin.native.ObjCName

/**
 * Swift-facing store wrapping [SettingsViewModel].
 *
 * Exposes only data actions; navigation is owned by SwiftUI.
 */
@ObjCName("SettingsStore")
class SettingsStore(
    private val viewModel: SettingsViewModel,
) {
    /** Observable theme mode preference. */
    val state: NativeFlow<ThemeMode> = NativeFlow(viewModel.themeMode, viewModel.viewModelScope)

    /** The patients available for the PDF export picker. */
    val patients: List<Patient>
        get() = viewModel.patients

    /** The patient selected for PDF export, or `null` when none is selected. */
    val selectedPatientId: Long?
        get() = viewModel.selectedPatientId

    /** The JSON payload pasted by the user for restore. */
    var restoreJson: String
        get() = viewModel.restoreJson
        set(value) {
            viewModel.restoreJson = value
        }

    /** Message from the last backup export, or `null`. */
    val backupStatus: String?
        get() = viewModel.backupStatus

    /** Message from the last restore, or `null`. */
    val restoreStatus: String?
        get() = viewModel.restoreStatus

    /** Message from the last PDF export, or `null`. */
    val pdfStatus: String?
        get() = viewModel.pdfStatus

    /** True while the database wipe is in flight. */
    val isWipingData: Boolean
        get() = viewModel.isWipingData

    /** True once a wipe completed successfully (done state: suggest relaunch). */
    val dataWiped: Boolean
        get() = viewModel.dataWiped

    /** Message from the last failed wipe, or `null`. */
    val wipeStatus: String?
        get() = viewModel.wipeStatus

    /** Exports every patient's records to a CSV file. */
    fun exportCsv() {
        viewModel.onExportClick()
    }

    /** Writes a full database backup and shares the JSON artifact. */
    fun exportBackup() {
        viewModel.onExportBackupClick()
    }

    /** Restores the database from the JSON in [restoreJson]. */
    fun restoreBackup() {
        viewModel.onRestoreBackupClick()
    }

    /** Selects the patient whose history the PDF export will include. */
    fun selectPatient(patientId: Long) {
        viewModel.onSelectPatient(patientId)
    }

    /** Renders the selected patient's history as a PDF and shares it. */
    fun exportPdf() {
        viewModel.onExportPdfClick()
    }

    /**
     * Erases every table and resets the search index. Call only after the
     * user confirmed in the UI — irreversible.
     */
    fun wipeAllData() {
        viewModel.onWipeAllDataClick()
    }

    /** Updates the theme mode, persisting the choice. */
    fun setThemeMode(mode: ThemeMode) {
        viewModel.onThemeModeChange(mode)
    }

    /** True when cloud AI routing is enabled. */
    val cloudAiEnabled: Boolean
        get() = viewModel.cloudAiEnabled

    /** The stored cloud API key (masked), or empty when none was entered. */
    val cloudApiKey: String
        get() = viewModel.cloudApiKey

    /** The chat-completions model name. */
    val cloudModel: String
        get() = viewModel.cloudModel

    /** The OpenAI-compatible endpoint URL. */
    val cloudBaseUrl: String
        get() = viewModel.cloudBaseUrl

    /** Toggles cloud AI routing, persisting the choice. */
    fun setCloudAiEnabled(enabled: Boolean) {
        viewModel.onCloudAiEnabledChange(enabled)
    }

    /** Persists the cloud API key (blank clears it). */
    fun setCloudApiKey(key: String) {
        viewModel.onCloudApiKeyChange(key)
    }

    /** Persists the cloud model name. */
    fun setCloudModel(model: String) {
        viewModel.onCloudModelChange(model)
    }

    /** Persists the cloud endpoint URL. */
    fun setCloudBaseUrl(url: String) {
        viewModel.onCloudBaseUrlChange(url)
    }

    /** All selectable provider presets, in display order. */
    val cloudProviderPresets: List<CloudLlmProviderPreset>
        get() = CloudLlmProviderPreset.entries.toList()

    /** The currently selected provider preset. */
    val cloudProviderPreset: CloudLlmProviderPreset
        get() = viewModel.cloudProviderPreset

    /** Selects a provider preset; non-custom presets fill the endpoint URL. */
    fun setCloudProviderPreset(preset: CloudLlmProviderPreset) {
        viewModel.onCloudProviderChange(preset)
    }

    /** Models from the last successful discovery (in-memory cache). */
    val cloudModelChoices: List<String>
        get() = viewModel.cloudModelChoices

    /** Status message from the last models fetch, or null when idle/successful. */
    val cloudModelsStatus: String?
        get() = viewModel.cloudModelsStatus

    /** True while a models fetch is in flight. */
    val isFetchingCloudModels: Boolean
        get() = viewModel.isFetchingCloudModels

    /** Fetches the model list from the configured endpoint; results land in [cloudModelChoices]. */
    suspend fun fetchCloudModels() {
        viewModel.fetchCloudModelsAwait()
    }
}
