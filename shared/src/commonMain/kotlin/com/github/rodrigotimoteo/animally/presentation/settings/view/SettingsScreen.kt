package com.github.rodrigotimoteo.animally.presentation.settings.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.rodrigotimoteo.animally.presentation.common.glass.GlassTopAppBar
import com.github.rodrigotimoteo.animally.presentation.common.glass.hazeSourceFrom
import com.github.rodrigotimoteo.animally.presentation.common.glass.rememberHazeState
import com.github.rodrigotimoteo.animally.presentation.reminder.ReminderSettingsUiState
import com.github.rodrigotimoteo.animally.presentation.reminder.ReminderSettingsViewModel
import com.github.rodrigotimoteo.animally.presentation.settings.SettingsViewModel
import com.github.rodrigotimoteo.animally.presentation.settings.SyncUiState
import com.github.rodrigotimoteo.animally.presentation.settings.SyncViewModel
import com.github.rodrigotimoteo.animally.presentation.theme.ThemeMode
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Instant

/**
 * Settings screen. Hosts the appearance selector, CSV export action, backup & restore controls,
 * the PDF export and the reminders section.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier,
) {
    val hazeState = rememberHazeState()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        modifier = modifier,
        topBar = {
            GlassTopAppBar(
                title = { Text("Settings") },
                hazeState = hazeState,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        val columnModifier =
            Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp)
                .hazeSourceFrom(hazeState)
        Column(
            modifier = columnModifier,
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            AppearanceSection(viewModel)
            CsvExportSection(viewModel)
            BackupSection(viewModel)
            PdfExportSection(viewModel)
            CloudSyncSection()
            RemindersSection()
        }
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
                    modifier = Modifier.semantics { contentDescription = "Theme ${mode.label}" },
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

@Composable
private fun CloudSyncSection() {
    val syncViewModel: SyncViewModel = koinViewModel()
    val syncState by syncViewModel.uiState.collectAsStateWithLifecycle()

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Cloud Sync", style = MaterialTheme.typography.titleMedium)
        SyncStatusLine(syncState)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(
                onClick = syncViewModel::syncNow,
                enabled = !syncState.isSyncing,
            ) {
                if (syncState.isSyncing) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(end = 8.dp),
                        strokeWidth = 2.dp,
                    )
                }
                Text(if (syncState.isSyncing) "Syncing…" else "Sync now")
            }
            syncState.errorMessage?.let { message ->
                Button(onClick = syncViewModel::onDismissError) {
                    Text("Dismiss error")
                }
            }
        }
        syncState.lastResult?.let { result ->
            if (result.success) {
                Text(
                    text = formatSyncSummary(result),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        syncState.errorMessage?.let { message ->
            Text(
                text = "Sync failed — $message",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun SyncStatusLine(state: SyncUiState) {
    val text =
        when {
            state.isSyncing -> "Syncing…"
            state.lastSyncAt != null -> "Last sync: ${formatInstant(state.lastSyncAt)}"
            else -> "Never synced"
        }
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

private const val MONTH_ABBREVIATION_LENGTH = 3

private fun formatInstant(instant: Instant): String {
    val local = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    val month =
        local.month.name
            .take(MONTH_ABBREVIATION_LENGTH)
            .replaceFirstChar { it.uppercase() }
    val hour = local.hour.toString().padStart(2, '0')
    val minute = local.minute.toString().padStart(2, '0')
    return "${local.day} $month ${local.year}, $hour:$minute"
}

private fun formatSyncSummary(result: com.github.rodrigotimoteo.animally.domain.sync.SyncResult): String {
    val parts =
        buildList {
            if (result.pushedCount > 0) add("pushed ${result.pushedCount}")
            if (result.pulledCount > 0) add("pulled ${result.pulledCount}")
            if (result.rejectedCount > 0) add("rejected ${result.rejectedCount}")
            if (result.deferredCount > 0) add("deferred ${result.deferredCount}")
        }
    return if (parts.isEmpty()) "Sync complete — nothing to transfer" else "Sync complete: ${parts.joinToString(", ")}"
}
