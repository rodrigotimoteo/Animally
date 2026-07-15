package com.github.rodrigotimoteo.animally.presentation.patientList

import com.github.rodrigotimoteo.animally.presentation.navigation.AnimallyNavigationViewModel
import com.github.rodrigotimoteo.animally.presentation.navigation.AnimallyNavigator
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class PatientListViewModel(
    animallyNavigator: AnimallyNavigator,
) : AnimallyNavigationViewModel(animallyNavigator)
