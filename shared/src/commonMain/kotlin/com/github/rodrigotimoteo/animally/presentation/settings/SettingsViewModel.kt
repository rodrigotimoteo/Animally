package com.github.rodrigotimoteo.animally.presentation.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.github.rodrigotimoteo.animally.domain.backup.ExportBackupUseCase
import com.github.rodrigotimoteo.animally.domain.backup.RestoreBackupUseCase
import com.github.rodrigotimoteo.animally.domain.export.ExportCsvUseCase
import com.github.rodrigotimoteo.animally.domain.export.pdf.ExportPatientReportUseCase
import com.github.rodrigotimoteo.animally.domain.export.pdf.generatePdf
import com.github.rodrigotimoteo.animally.domain.export.shareFile
import com.github.rodrigotimoteo.animally.domain.export.shareFileAt
import com.github.rodrigotimoteo.animally.domain.export.sharePdf
import com.github.rodrigotimoteo.animally.domain.patient.IPatientRepository
import com.github.rodrigotimoteo.animally.domain.patient.model.Patient
import com.github.rodrigotimoteo.animally.presentation.navigation.AnimallyNavigationViewModel
import com.github.rodrigotimoteo.animally.presentation.navigation.AnimallyNavigator
import com.github.rodrigotimoteo.animally.presentation.theme.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class SettingsViewModel(
    private val exportCsvUseCase: ExportCsvUseCase,
    private val exportBackupUseCase: ExportBackupUseCase,
    private val restoreBackupUseCase: RestoreBackupUseCase,
    private val exportReportUseCase: ExportPatientReportUseCase,
    private val patientRepository: IPatientRepository,
    private val themePreferenceStore: ThemePreferenceStore,
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

    private val _themeMode = MutableStateFlow(themePreferenceStore.getThemeMode())
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    /**
     * Exports every patient's records to a CSV file and shares it.
     */
    fun onExportClick() {
        val csv = exportCsvUseCase(patientId = null, from = null, to = null)
        shareFile(fileName = "animally-patients.csv", content = csv, contentType = "text/csv")
    }

    /**
     * Writes a full database backup and shares the JSON artifact.
     */
    fun onExportBackupClick() {
        val result = exportBackupUseCase()
        shareFileAt(fileName = result.fileName, path = result.backupPath, contentType = "application/json")
        backupStatus = "Backup exported: ${result.fileName}"
    }

    /**
     * Restores the database from the JSON pasted into [restoreJson].
     */
    fun onRestoreBackupClick() {
        if (restoreJson.isBlank()) {
            restoreStatus = "Paste backup JSON first"
            return
        }
        restoreStatus =
            try {
                restoreBackupUseCase(restoreJson)
                "Restore complete"
            } catch (e: Exception) {
                "Restore failed: ${e.message}"
            }
    }

    /**
     * Selects the patient whose history the PDF export will include.
     */
    fun onSelectPatient(patientId: Long) {
        selectedPatientIdState.value = patientId
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
        val report = exportReportUseCase(patientId = patientId, from = null, to = null)
        sharePdf(fileName = "patient-history-${report.patient.name}.pdf", bytes = generatePdf(report))
        pdfStatus = "PDF exported for ${report.patient.name}"
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
}
