package com.github.rodrigotimoteo.animally.presentation.ownerList

import com.github.rodrigotimoteo.animally.presentation.navigation.AnimallyNavigationViewModel
import com.github.rodrigotimoteo.animally.presentation.navigation.AnimallyNavigator
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class OwnerListViewModel(
    animallyNavigator: AnimallyNavigator,
) : AnimallyNavigationViewModel(animallyNavigator)
