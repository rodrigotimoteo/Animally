package com.github.rodrigotimoteo.animally.presentation

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.github.rodrigotimoteo.animally.presentation.navigation.AnimallyNavHost

@Composable
@Preview
fun AnimallyApp() {
    MaterialTheme {
        AnimallyNavHost()
    }
}
