package com.github.rodrigotimoteo.animally.presentation.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.github.rodrigotimoteo.animally.di.dispatchers.IO_DISPATCHER
import com.github.rodrigotimoteo.animally.domain.backup.ExportBackupUseCase
import com.github.rodrigotimoteo.animally.domain.backup.RestoreBackupUseCase
import com.github.rodrigotimoteo.animally.domain.export.ExportCsvUseCase
import com.github.rodrigotimoteo.animally.domain.export.pdf.ExportPatientReportUseCase
import com.github.rodrigotimoteo.animally.domain.export.pdf.PdfPalette
import com.github.rodrigotimoteo.animally.domain.export.pdf.generatePdf
import com.github.rodrigotimoteo.animally.domain.export.shareFile
import com.github.rodrigotimoteo.animally.domain.export.shareFileAt
import com.github.rodrigotimoteo.animally.domain.export.sharePdf
import com.github.rodrigotimoteo.animally.domain.patient.IPatientRepository
import com.github.rodrigotimoteo.animally.domain.patient.model.Patient
import com.github.rodrigotimoteo.animally.domain.settings.usecase.WipeAllDataUseCase
import com.github.rodrigotimoteo.animally.llm.cloud.CloudLlmProviderPreset
import com.github.rodrigotimoteo.animally.llm.cloud.CloudModelCatalog
import com.github.rodrigotimoteo.animally.llm.cloud.CloudModelsResult
import com.github.rodrigotimoteo.animally.presentation.navigation.AnimallyNavigationViewModel
import com.github.rodrigotimoteo.animally.presentation.navigation.AnimallyNavigator
import com.github.rodrigotimoteo.animally.presentation.theme.AccentColor
import com.github.rodrigotimoteo.animally.presentation.theme.ThemeMode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import org.koin.core.annotation.KoinViewModel
import org.koin.core.annotation.Named
import kotlin.time.Clock

@KoinViewModel
@Suppress("LongParameterList", "TooManyFunctions")
class SettingsViewModel(
    private val exportCsvUseCase: ExportCsvUseCase,
    private val exportBackupUseCase: ExportBackupUseCase,
    private val restoreBackupUseCase: RestoreBackupUseCase,
    private val exportReportUseCase: ExportPatientReportUseCase,
    private val wipeAllDataUseCase: WipeAllDataUseCase,
    private val patientRepository: IPatientRepository,
    private val themePreferenceStore: ThemePreferenceStore,
    private val cloudLlmSettings: CloudLlmSettingsStore,
    private val cloudModelCatalog: CloudModelCatalog,
    @Named(IO_DISPATCHER) private val ioDispatcher: CoroutineDispatcher,
    animallyNavigator: AnimallyNavigator,
) : AnimallyNavigationViewModel(animallyNavigator) {
    private val patientsState = mutableStateOf(patientRepository.getPatientList())
    val patients: List<Patient>
        get() = patientsState.value

    private val selectedPatientIdState = mutableStateOf<Long?>(null)
    var selectedPatientId: Long?
        get() = selectedPatientIdState.value
        set(value) {
            selectedPatientIdState.value = value
        }

    var restoreJson: String by mutableStateOf("")

    var backupStatus: String? by mutableStateOf(null)
    var restoreStatus: String? by mutableStateOf(null)
    var pdfStatus: String? by mutableStateOf(null)

    /** True while the all-patient CSV export is being built or shared. */
    var isExportingCsv: Boolean by mutableStateOf(false)
        private set

    /** True while the dual-format backup is being built or shared. */
    var isExportingBackup: Boolean by mutableStateOf(false)
        private set

    /** True while a pasted backup is being validated and restored. */
    var isRestoringBackup: Boolean by mutableStateOf(false)
        private set

    /** True while a patient PDF is being rendered or shared. */
    var isExportingPdf: Boolean by mutableStateOf(false)
        private set

    var csvStatus: String? by mutableStateOf(null)

    /** True while the database wipe is in flight. */
    var isWipingData: Boolean by mutableStateOf(false)
        private set

    /** True once a wipe completed successfully; drives the "relaunch" done state. */
    var dataWiped: Boolean by mutableStateOf(false)
        private set

    /** Message from the last failed wipe, or `null`. */
    var wipeStatus: String? by mutableStateOf(null)
        private set

    private val _themeMode = MutableStateFlow(themePreferenceStore.getThemeMode())
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _accentColor = MutableStateFlow(themePreferenceStore.getAccentColor())
    val accentColor: StateFlow<AccentColor> = _accentColor.asStateFlow()

    // Cloud AI settings: composed state backed by the platform store; the API key
    // round-trips through secure storage only (never plaintext preferences).
    private val cloudLlmSettingsState = mutableStateOf(cloudLlmSettings.snapshot())

    /** True when the user opted into routing assistant answers to a cloud model. */
    var cloudAiEnabled: Boolean
        get() = cloudLlmSettingsState.value.enabled
        private set(value) {
            cloudLlmSettingsState.value = cloudLlmSettingsState.value.copy(enabled = value)
        }

    /** The stored API key (masked input value), or empty when none was entered. */
    var cloudApiKey: String
        get() = cloudLlmSettingsState.value.apiKey.orEmpty()
        private set(value) {
            cloudLlmSettingsState.value = cloudLlmSettingsState.value.copy(apiKey = value)
        }

    /** The chat-completions model name (e.g. gpt-4o-mini). */
    var cloudModel: String
        get() = cloudLlmSettingsState.value.model
        private set(value) {
            cloudLlmSettingsState.value = cloudLlmSettingsState.value.copy(model = value)
        }

    /** The OpenAI-compatible endpoint URL. */
    var cloudBaseUrl: String
        get() = cloudLlmSettingsState.value.baseUrl
        private set(value) {
            cloudLlmSettingsState.value = cloudLlmSettingsState.value.copy(baseUrl = value)
        }

    /** The selected provider preset (drives the base-URL shortcut). */
    var cloudProviderPreset: CloudLlmProviderPreset
        get() = CloudLlmProviderPreset.fromId(cloudLlmSettingsState.value.presetId)
        private set(value) {
            cloudLlmSettingsState.value = cloudLlmSettingsState.value.copy(presetId = value.id)
        }

    // Model discovery: last-fetched list is cached in memory only (never persisted);
    // status carries the latest fetch outcome for the UI.
    private var cachedCloudModels: List<String> = emptyList()

    /** Models from the last successful discovery, in-memory only. */
    val cloudModelChoices: List<String>
        get() = cachedCloudModels

    /** Status message from the last models fetch, or null when idle/successful. */
    var cloudModelsStatus: String? by mutableStateOf(null)
        private set

    /** True while a models fetch is in flight. */
    var isFetchingCloudModels: Boolean by mutableStateOf(false)
        private set

    /**
     * Exports every patient's records to a dated CSV file and shares it.
     */
    fun onExportClick() {
        viewModelScope.launch { exportCsvAwait() }
    }

    /** Completes the CSV export, allowing native bridges to await its outcome. */
    suspend fun exportCsvAwait() {
        if (isExportingCsv) return
        isExportingCsv = true
        csvStatus = null
        try {
            val artifact =
                withContext(ioDispatcher) {
                    val csv = exportCsvUseCase(patientId = null, from = null, to = null)
                    val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
                    "animally-patients-$today.csv" to csv
                }
            shareFile(fileName = artifact.first, content = artifact.second, contentType = "text/csv")
            csvStatus = "CSV exported: ${artifact.first}"
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            csvStatus = "CSV export failed: ${e.message}"
        } finally {
            isExportingCsv = false
        }
    }

    /**
     * Writes a full database backup and shares the JSON artifact.
     */
    fun onExportBackupClick() {
        viewModelScope.launch { exportBackupAwait() }
    }

    /** Completes the backup export, allowing native bridges to await its outcome. */
    suspend fun exportBackupAwait() {
        if (isExportingBackup) return
        isExportingBackup = true
        backupStatus = null
        try {
            val result = withContext(ioDispatcher) { exportBackupUseCase() }
            shareFileAt(fileName = result.fileName, path = result.backupPath, contentType = "application/json")
            backupStatus = "Backup exported: ${result.fileName}"
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            backupStatus = "Backup export failed: ${e.message}"
        } finally {
            isExportingBackup = false
        }
    }

    /**
     * Restores the database from the JSON pasted into [restoreJson].
     */
    fun onRestoreBackupClick() {
        if (restoreJson.isBlank()) {
            restoreStatus = "Paste backup JSON first"
            return
        }
        viewModelScope.launch { restoreBackupAwait(restoreJson) }
    }

    /** Completes a restore, allowing native bridges to await its outcome. */
    suspend fun restoreBackupAwait(jsonContent: String = restoreJson) {
        if (jsonContent.isBlank()) {
            restoreStatus = "Paste backup JSON first"
            return
        }
        if (isRestoringBackup) return
        isRestoringBackup = true
        restoreStatus = null
        try {
            withContext(ioDispatcher) { restoreBackupUseCase(jsonContent) }
            restoreStatus = "Restore complete"
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            restoreStatus = "Restore failed: ${e.message}"
        } finally {
            isRestoringBackup = false
        }
    }

    /**
     * Selects the patient whose history the PDF export will include.
     */
    fun onSelectPatient(patientId: Long) {
        selectedPatientIdState.value = patientId
    }

    /**
     * Erases every table and resets the search index. Call only after an
     * explicit user confirmation — irreversible. On success the patient list
     * reloads (now empty) so open screens reflect the wiped state.
     */
    fun onWipeAllDataClick() {
        viewModelScope.launch { wipeAllDataAwait() }
    }

    /** Completes a wipe, allowing native bridges to await its outcome. */
    suspend fun wipeAllDataAwait() {
        if (isWipingData) return
        isWipingData = true
        wipeStatus = null
        try {
            val patientsAfterWipe =
                withContext(ioDispatcher) {
                    val result = wipeAllDataUseCase()
                    result to patientRepository.getPatientList()
                }
            val (wipeResult, patients) = patientsAfterWipe
            dataWiped = wipeResult.isComplete
            wipeStatus =
                if (wipeResult.isComplete) {
                    null
                } else {
                    buildString {
                        append("Erase completed with cleanup still needed")
                        if (wipeResult.residualMediaPaths.isNotEmpty()) {
                            append(" (${wipeResult.residualMediaPaths.size} media file(s))")
                        }
                        if (!wipeResult.notificationsCancelled) append("; notifications could not be cancelled")
                        if (!wipeResult.searchIndexRebuilt) append("; search index will retry on next launch")
                    }
                }
            patientsState.value = patients
            selectedPatientIdState.value = null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            wipeStatus = "Erase failed: ${e.message}"
        } finally {
            isWipingData = false
        }
    }

    /**
     * Renders the selected patient's history as a PDF and shares it.
     */
    fun onExportPdfClick() {
        val patientId = selectedPatientIdState.value
        if (patientId == null) {
            pdfStatus = "Select a patient first"
            return
        }
        viewModelScope.launch { exportPdfAwait(patientId) }
    }

    /** Completes a patient PDF export, allowing native bridges to await its outcome. */
    suspend fun exportPdfAwait(patientId: Long? = selectedPatientIdState.value) {
        if (patientId == null) {
            pdfStatus = "Select a patient first"
            return
        }
        if (isExportingPdf) return
        isExportingPdf = true
        pdfStatus = null
        try {
            val artifact =
                withContext(ioDispatcher) {
                    val report =
                        exportReportUseCase(patientId = patientId, from = null, to = null).copy(
                            palette = PdfPalette.forAccentId(themePreferenceStore.getAccentColor().id),
                        )
                    report.patient.name to generatePdf(report)
                }
            val fileName = "patient-history-${artifact.first}.pdf"
            sharePdf(fileName = fileName, bytes = artifact.second)
            pdfStatus = "PDF exported for ${artifact.first}"
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            pdfStatus = "PDF export failed: ${e.message}"
        } finally {
            isExportingPdf = false
        }
    }

    /**
     * Updates the theme mode, persisting the choice and notifying observers.
     *
     * @param mode The new theme mode to apply.
     */
    fun onThemeModeChange(mode: ThemeMode) {
        themePreferenceStore.setThemeMode(mode)
        _themeMode.value = mode
    }

    /** Updates the brand accent, persisting it for the next app launch. */
    fun onAccentColorChange(accent: AccentColor) {
        themePreferenceStore.setAccentColor(accent)
        _accentColor.value = accent
    }

    /** Toggles cloud AI routing, persisting the choice. */
    fun onCloudAiEnabledChange(enabled: Boolean) {
        cloudLlmSettings.setEnabled(enabled)
        cloudAiEnabled = enabled
    }

    /** Persists the API key (blank clears it). */
    fun onCloudApiKeyChange(key: String) {
        cloudLlmSettings.setApiKey(key.ifBlank { null })
        cloudApiKey = key
    }

    /** Persists the model name. */
    fun onCloudModelChange(model: String) {
        cloudLlmSettings.setModel(model)
        cloudModel = model
    }

    /** Persists the endpoint URL. */
    fun onCloudBaseUrlChange(url: String) {
        cloudLlmSettings.setBaseUrl(url)
        cloudBaseUrl = url
    }

    /**
     * Selects a provider preset, persisting its id. Non-custom presets also fill
     * the base URL (still editable afterwards); [CloudLlmProviderPreset.CUSTOM]
     * leaves whatever URL is stored untouched.
     */
    fun onCloudProviderChange(preset: CloudLlmProviderPreset) {
        cloudLlmSettings.setPresetId(preset.id)
        cloudProviderPreset = preset
        if (preset.baseUrl.isNotBlank()) {
            onCloudBaseUrlChange(preset.baseUrl)
        }
    }

    /**
     * Fetches the model list from the currently configured endpoint. Local presets
     * work keyless; a blank key is simply omitted from the request. Results are
     * cached in memory ([cloudModelChoices]); errors surface via [cloudModelsStatus].
     */
    fun onFetchCloudModels() {
        if (isFetchingCloudModels) return
        viewModelScope.launch { runCloudModelsFetch() }
    }

    /**
     * Suspend variant of [onFetchCloudModels] that completes when the fetch does,
     * so bridges can await completion before reading back the cached results.
     */
    suspend fun fetchCloudModelsAwait() {
        if (isFetchingCloudModels) return
        runCloudModelsFetch()
    }

    private suspend fun runCloudModelsFetch() {
        if (!cloudLlmSettings.isReadyForCloudModelDiscovery()) {
            cloudModelsStatus = "Complete Cloud AI settings before fetching models"
            return
        }
        isFetchingCloudModels = true
        cloudModelsStatus = null
        try {
            val result =
                cloudModelCatalog.fetch(
                    baseUrl = cloudBaseUrl,
                    apiKey = cloudApiKey.ifBlank { null },
                    allowInsecureLocalEndpoint = cloudProviderPreset.isLocalRuntime,
                )
            when (result) {
                is CloudModelsResult.Success -> {
                    cachedCloudModels = result.models
                    if (result.models.isEmpty()) cloudModelsStatus = "No models found at this endpoint"
                }
                is CloudModelsResult.Unauthorized -> cloudModelsStatus = "Invalid API key"
                is CloudModelsResult.Failure ->
                    cloudModelsStatus = "Could not fetch models: ${result.message}"
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            cloudModelsStatus = "Could not fetch models: ${e.message}"
        } finally {
            isFetchingCloudModels = false
        }
    }
}
