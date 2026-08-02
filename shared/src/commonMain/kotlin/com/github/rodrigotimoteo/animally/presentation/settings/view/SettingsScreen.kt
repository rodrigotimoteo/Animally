package com.github.rodrigotimoteo.animally.presentation.settings.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.rodrigotimoteo.animally.presentation.reminder.ReminderSettingsViewModel
import com.github.rodrigotimoteo.animally.presentation.settings.SettingsViewModel
import org.koin.compose.viewmodel.koinViewModel

/**
 * Settings screen. Hosts the CSV export action and the reminders section.
 */
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier,
) {
    val reminderViewModel: ReminderSettingsViewModel = koinViewModel()
    val reminderState by reminderViewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Export", style = MaterialTheme.typography.titleMedium)
            Button(onClick = viewModel::onExportClick) {
                Text("Export CSV")
            }
        }
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
}
