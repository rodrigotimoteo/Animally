package com.github.rodrigotimoteo.animally.di.navigation

import com.github.rodrigotimoteo.animally.presentation.navigation.Route
import com.github.rodrigotimoteo.animally.presentation.ownerList.view.OwnerListScreen
import com.github.rodrigotimoteo.animally.presentation.patientList.view.PatientListScreen
import com.github.rodrigotimoteo.animally.presentation.settings.view.SettingsScreen
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.dsl.module
import org.koin.dsl.navigation3.navigation

@OptIn(KoinExperimentalAPI::class)
val navigationEntryModule =
    module {
        navigation<Route.PatientList> {
            PatientListScreen()
        }

        navigation<Route.OwnerList> {
            OwnerListScreen()
        }

        navigation<Route.Settings> {
            SettingsScreen()
        }
    }
