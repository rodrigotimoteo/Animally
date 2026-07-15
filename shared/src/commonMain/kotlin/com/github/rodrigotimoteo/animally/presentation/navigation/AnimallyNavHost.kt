package com.github.rodrigotimoteo.animally.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation3.ui.NavDisplay
import org.koin.compose.koinInject
import org.koin.compose.navigation3.koinEntryProvider
import org.koin.core.annotation.KoinExperimentalAPI

@OptIn(KoinExperimentalAPI::class)
@Composable
fun AnimallyNavHost(animallyNavigator: AnimallyNavigator = koinInject()) {
    NavDisplay(
        backStack = animallyNavigator.backStack,
        onBack = { animallyNavigator.popBackStack() },
        entryProvider = koinEntryProvider(),
    )
}
