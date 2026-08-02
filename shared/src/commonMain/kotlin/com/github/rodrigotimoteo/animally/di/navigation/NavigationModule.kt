package com.github.rodrigotimoteo.animally.di.navigation

import com.github.rodrigotimoteo.animally.presentation.anamnese.view.AnamneseScreen
import com.github.rodrigotimoteo.animally.presentation.consultation.view.ConsultationEditScreen
import com.github.rodrigotimoteo.animally.presentation.navigation.Route
import com.github.rodrigotimoteo.animally.presentation.ownerDetail.view.OwnerDetailScreen
import com.github.rodrigotimoteo.animally.presentation.ownerEdit.view.OwnerEditScreen
import com.github.rodrigotimoteo.animally.presentation.ownerList.view.OwnerListScreen
import com.github.rodrigotimoteo.animally.presentation.patientDetail.view.PatientDetailScreen
import com.github.rodrigotimoteo.animally.presentation.patientEdit.view.PatientEditScreen
import com.github.rodrigotimoteo.animally.presentation.patientList.view.PatientListScreen
import com.github.rodrigotimoteo.animally.presentation.settings.view.SettingsScreen
import com.github.rodrigotimoteo.animally.presentation.vaccination.view.VaccinationEditScreen
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module
import org.koin.dsl.navigation3.navigation

@OptIn(KoinExperimentalAPI::class)
val navigationEntryModule =
    module {
        navigation<Route.PatientList> {
            PatientListScreen(viewModel = koinViewModel())
        }

        navigation<Route.OwnerList> {
            OwnerListScreen(viewModel = koinViewModel())
        }

        navigation<Route.OwnerDetail> { route ->
            OwnerDetailScreen(viewModel = koinViewModel { parametersOf(route.ownerId) })
        }

        navigation<Route.AddEditOwner> { route ->
            OwnerEditScreen(viewModel = koinViewModel { parametersOf(route.ownerId) })
        }

        navigation<Route.PatientDetail> { route ->
            PatientDetailScreen(viewModel = koinViewModel { parametersOf(route.patientId) })
        }

        navigation<Route.AddEditPatient> { route ->
            PatientEditScreen(viewModel = koinViewModel { parametersOf(route.patientId) })
        }

        navigation<Route.AddEditAnamnese> { route ->
            AnamneseScreen(viewModel = koinViewModel { parametersOf(route.patientId, route.anamneseId) })
        }

        navigation<Route.AddEditConsultation> { route ->
            ConsultationEditScreen(viewModel = koinViewModel { parametersOf(route.patientId, route.consultationId) })
        }

        navigation<Route.AddEditVaccination> { route ->
            VaccinationEditScreen(viewModel = koinViewModel { parametersOf(route.patientId, route.vaccinationId) })
        }

        navigation<Route.Settings> {
            SettingsScreen()
        }
    }
