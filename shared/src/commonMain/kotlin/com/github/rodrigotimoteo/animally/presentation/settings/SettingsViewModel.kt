package com.github.rodrigotimoteo.animally.presentation.settings

import com.github.rodrigotimoteo.animally.domain.export.ExportCsvUseCase
import com.github.rodrigotimoteo.animally.domain.export.shareFile
import com.github.rodrigotimoteo.animally.presentation.navigation.AnimallyNavigationViewModel
import com.github.rodrigotimoteo.animally.presentation.navigation.AnimallyNavigator
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class SettingsViewModel(
    private val exportCsvUseCase: ExportCsvUseCase,
    animallyNavigator: AnimallyNavigator,
) : AnimallyNavigationViewModel(animallyNavigator) {
    /**
     * Exports every patient's records to a CSV file and shares it.
     */
    fun onExportClick() {
        val csv = exportCsvUseCase(patientId = null, from = null, to = null)
        shareFile(fileName = "animally-patients.csv", content = csv, contentType = "text/csv")
    }
}
