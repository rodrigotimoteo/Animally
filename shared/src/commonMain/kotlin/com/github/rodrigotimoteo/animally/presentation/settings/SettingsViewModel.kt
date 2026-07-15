package com.github.rodrigotimoteo.animally.presentation.settings

import com.github.rodrigotimoteo.animally.presentation.navigation.AnimallyNavigationViewModel
import com.github.rodrigotimoteo.animally.presentation.navigation.AnimallyNavigator
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class SettingsViewModel(
    animallyNavigator: AnimallyNavigator,
) : AnimallyNavigationViewModel(animallyNavigator)
