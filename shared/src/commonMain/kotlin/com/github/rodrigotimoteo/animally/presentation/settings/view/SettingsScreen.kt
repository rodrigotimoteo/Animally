package com.github.rodrigotimoteo.animally.presentation.settings.view

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.github.rodrigotimoteo.animally.presentation.settings.SettingsViewModel

/**
 * Settings screen. Currently hosts the CSV export action for the ROD-33 POC.
 */
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Button(onClick = viewModel::onExportClick) {
            Text("Export CSV")
        }
    }
}
