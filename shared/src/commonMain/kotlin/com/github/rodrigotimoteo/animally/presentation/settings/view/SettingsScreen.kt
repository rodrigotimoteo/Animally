package com.github.rodrigotimoteo.animally.presentation.settings.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.rodrigotimoteo.animally.presentation.reminder.ReminderSettingsUiState
import com.github.rodrigotimoteo.animally.presentation.reminder.ReminderSettingsViewModel
import com.github.rodrigotimoteo.animally.presentation.settings.SettingsViewModel
import com.github.rodrigotimoteo.animally.presentation.theme.ThemeMode
import org.koin.compose.viewmodel.koinViewModel

/**
 * Settings screen. Hosts the appearance selector, CSV export action, backup & restore controls,
 * the PDF export and the reminders section.
 */
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        AppearanceSection(viewModel)
        CsvExportSection(viewModel)
        BackupSection(viewModel)
        PdfExportSection(viewModel)
        RemindersSection()
    }
}

@Composable
private fun AppearanceSection(viewModel: SettingsViewModel) {
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Appearance", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ThemeMode.entries.forEach { mode ->
                FilterChip(
                    selected = themeMode == mode,
                    onClick = { viewModel.onThemeModeChange(mode) },
                    label = { Text(mode.label) },
                )
            }
        }
    }
}

@Composable
private fun CsvExportSection(viewModel: SettingsViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Export", style = MaterialTheme.typography.titleMedium)
        Button(onClick = viewModel::onExportClick) {
            Text("Export CSV")
        }
    }
}

@Composable
private fun BackupSection(viewModel: SettingsViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Backup & Restore", style = MaterialTheme.typography.titleMedium)
        Button(onClick = viewModel::onExportBackupClick) {
            Text("Export backup")
        }
        viewModel.backupStatus?.let { status ->
            Text(status, style = MaterialTheme.typography.bodySmall)
        }
        OutlinedTextField(
            value = viewModel.restoreJson,
            onValueChange = { viewModel.restoreJson = it },
            label = { Text("Backup JSON") },
            modifier = Modifier.fillMaxWidth(),
        )
        Button(onClick = viewModel::onRestoreBackupClick) {
            Text("Restore backup")
        }
        viewModel.restoreStatus?.let { status ->
            Text(status, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun PdfExportSection(viewModel: SettingsViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("PDF Export", style = MaterialTheme.typography.titleMedium)
        viewModel.patients.forEach { patient ->
            val selected = viewModel.selectedPatientId == patient.id
            Button(
                onClick = { viewModel.onSelectPatient(patient.id) },
                enabled = !selected,
            ) {
                Text(patient.name)
            }
        }
        Button(onClick = viewModel::onExportPdfClick) {
            Text("Export PDF")
        }
        viewModel.pdfStatus?.let { status ->
            Text(status, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun RemindersSection() {
    val reminderViewModel: ReminderSettingsViewModel = koinViewModel()
    val reminderState by reminderViewModel.uiState.collectAsStateWithLifecycle()

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Reminders", style = MaterialTheme.typography.titleMedium)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Schedule vaccination and dentistry reminders",
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = reminderState.remindersEnabled,
                onCheckedChange = reminderViewModel::setRemindersEnabled,
                enabled = !reminderState.isPermissionRequesting,
            )
        }
        PermissionStatus(reminderState)
        reminderState.permissionMessage?.let { message ->
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Button(
            onClick = reminderViewModel::checkRemindersNow,
            enabled = !reminderState.isChecking,
        ) {
            Text(if (reminderState.isChecking) "Checking…" else "Check reminders now")
        }
        reminderState.lastCheckedCount?.let { count ->
            Text("Found $count reminder(s)")
        }
        reminderState.errorMessage?.let { message ->
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun PermissionStatus(state: ReminderSettingsUiState) {
    val text =
        when {
            state.isPermissionRequesting -> "Requesting notification permission…"
            state.notificationsEnabled == false -> "Notifications disabled"
            else -> "Notifications enabled"
        }
    Text(text = text, style = MaterialTheme.typography.bodySmall)
}
