package com.github.rodrigotimoteo.animally.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.rodrigotimoteo.animally.presentation.navigation.AnimallyNavHost
import com.github.rodrigotimoteo.animally.presentation.settings.SettingsViewModel
import com.github.rodrigotimoteo.animally.presentation.theme.AnimallyTheme
import org.koin.compose.viewmodel.koinViewModel

@Composable
@Preview
fun AnimallyApp(viewModel: SettingsViewModel = koinViewModel()) {
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    AnimallyTheme(themeMode = themeMode) {
        AnimallyNavHost()
    }
}
