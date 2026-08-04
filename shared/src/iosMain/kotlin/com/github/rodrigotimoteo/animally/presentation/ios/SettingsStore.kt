@file:OptIn(ExperimentalObjCName::class)

package com.github.rodrigotimoteo.animally.presentation.ios

import androidx.lifecycle.viewModelScope
import com.github.rodrigotimoteo.animally.bridge.NativeFlow
import com.github.rodrigotimoteo.animally.domain.patient.model.Patient
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

    /** Updates the theme mode, persisting the choice. */
    fun setThemeMode(mode: ThemeMode) {
        viewModel.onThemeModeChange(mode)
    }
}
