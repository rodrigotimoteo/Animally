package com.github.rodrigotimoteo.animally.presentation.settings.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.github.rodrigotimoteo.animally.getPlatform
import com.github.rodrigotimoteo.animally.llm.cloud.CloudLlmProviderPreset
import com.github.rodrigotimoteo.animally.presentation.settings.SettingsViewModel

/**
 * Cloud AI section: opt-in toggle, provider preset picker, API key (masked),
 * model discovery ("Fetch models" + searchable picker), and an advanced endpoint
 * URL field. Answers produced by the cloud model are badged in the chat.
 */
@Composable
internal fun CloudAiSection(viewModel: SettingsViewModel) {
    var showAdvanced by remember { mutableStateOf(false) }
    var showModelPicker by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Cloud AI", style = MaterialTheme.typography.titleMedium)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Answer with a cloud model when on-device AI is unavailable",
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = viewModel.cloudAiEnabled,
                onCheckedChange = viewModel::onCloudAiEnabledChange,
                modifier = Modifier.semantics { contentDescription = "Cloud AI enabled" },
            )
        }
        if (viewModel.cloudAiEnabled) {
            ProviderPresetField(viewModel)
            OutlinedTextField(
                value = viewModel.cloudApiKey,
                onValueChange = viewModel::onCloudApiKeyChange,
                label = { Text("API key") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
            )
            ModelDiscoveryRow(viewModel, onShowPicker = { showModelPicker = true })
            TextButton(onClick = { showAdvanced = !showAdvanced }) {
                Text(if (showAdvanced) "Hide advanced" else "Advanced")
            }
            if (showAdvanced) {
                OutlinedTextField(
                    value = viewModel.cloudBaseUrl,
                    onValueChange = viewModel::onCloudBaseUrlChange,
                    label = { Text("Endpoint URL") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
    if (showModelPicker) {
        CloudModelPickerDialog(
            models = viewModel.cloudModelChoices,
            onSelect = { model ->
                viewModel.onCloudModelChange(model)
                showModelPicker = false
            },
            onDismiss = { showModelPicker = false },
        )
    }
}

/** Manual model entry plus the "Fetch models" discovery action and its status. */
@Composable
private fun ModelDiscoveryRow(
    viewModel: SettingsViewModel,
    onShowPicker: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = viewModel.cloudModel,
            onValueChange = viewModel::onCloudModelChange,
            label = { Text("Model") },
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onShowPicker) {
            Text("Fetch models")
        }
        if (viewModel.isFetchingCloudModels) {
            CircularProgressIndicator(
                modifier = Modifier.semantics { contentDescription = "Fetching models" },
            )
        }
    }
    viewModel.cloudModelsStatus?.let { status ->
        Text(status, style = MaterialTheme.typography.bodySmall)
    }
}

/** Provider preset dropdown; selecting a preset fills the endpoint URL. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProviderPresetField(viewModel: SettingsViewModel) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        val fieldModifier =
            Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .semantics { contentDescription = "Cloud provider" }
        OutlinedTextField(
            value = viewModel.cloudProviderPreset.displayName,
            onValueChange = {},
            readOnly = true,
            label = { Text("Provider") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = fieldModifier,
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            // Local runtimes (Ollama/LM Studio) point at localhost: desktop-only.
            val presets =
                CloudLlmProviderPreset.entries.filter {
                    it.visibleOnMobile || getPlatform().isDesktop
                }
            presets.forEach { preset ->
                DropdownMenuItem(
                    text = { Text(preset.displayName) },
                    onClick = {
                        viewModel.onCloudProviderChange(preset)
                        expanded = false
                    },
                )
            }
        }
    }
}

/** Searchable dialog over the last-fetched model list; tapping selects a model. */
@Composable
private fun CloudModelPickerDialog(
    models: List<String>,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var filter by remember { mutableStateOf("") }
    val filtered = models.filter { it.contains(filter, ignoreCase = true) }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
        title = { Text("Choose a model") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = filter,
                    onValueChange = { filter = it },
                    label = { Text("Filter") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                when {
                    models.isEmpty() -> {
                        Text(
                            "No models fetched yet. Tap \"Fetch models\" first.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    filtered.isEmpty() -> {
                        Text(
                            "No models match \"$filter\"",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    else -> {
                        LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp)) {
                            items(filtered) { model ->
                                TextButton(onClick = { onSelect(model) }) {
                                    Text(model)
                                }
                            }
                        }
                    }
                }
            }
        },
    )
}
