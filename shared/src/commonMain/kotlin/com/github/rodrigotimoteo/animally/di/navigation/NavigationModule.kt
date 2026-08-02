package com.github.rodrigotimoteo.animally.di.navigation

import com.github.rodrigotimoteo.animally.presentation.anamnese.view.AnamneseScreen
import com.github.rodrigotimoteo.animally.presentation.consultation.view.ConsultationEditScreen
import com.github.rodrigotimoteo.animally.presentation.dentistry.view.DentistryEditScreen
import com.github.rodrigotimoteo.animally.presentation.deworming.view.DewormingEditScreen
import com.github.rodrigotimoteo.animally.presentation.farrier.view.FarrierVisitEditScreen
import com.github.rodrigotimoteo.animally.presentation.gestation.view.GestationEditScreen
import com.github.rodrigotimoteo.animally.presentation.imaging.view.ImagingEditScreen
import com.github.rodrigotimoteo.animally.presentation.labresult.view.LabResultEditScreen
import com.github.rodrigotimoteo.animally.presentation.lameness.view.LamenessEditScreen
import com.github.rodrigotimoteo.animally.presentation.medication.view.MedicationEditScreen
import com.github.rodrigotimoteo.animally.presentation.navigation.Route
import com.github.rodrigotimoteo.animally.presentation.ownerDetail.view.OwnerDetailScreen
import com.github.rodrigotimoteo.animally.presentation.ownerEdit.view.OwnerEditScreen
import com.github.rodrigotimoteo.animally.presentation.ownerList.view.OwnerListScreen
import com.github.rodrigotimoteo.animally.presentation.patientDetail.view.PatientDetailScreen
import com.github.rodrigotimoteo.animally.presentation.patientEdit.view.PatientEditScreen
import com.github.rodrigotimoteo.animally.presentation.patientList.view.PatientListScreen
import com.github.rodrigotimoteo.animally.presentation.reproduction.view.ReproductionEventEditScreen
import com.github.rodrigotimoteo.animally.presentation.repromedication.view.ReproMedicationEditScreen
import com.github.rodrigotimoteo.animally.presentation.settings.view.SettingsScreen
import com.github.rodrigotimoteo.animally.presentation.substance.view.ControlledSubstanceEditScreen
import com.github.rodrigotimoteo.animally.presentation.surgery.view.SurgeryEditScreen
import com.github.rodrigotimoteo.animally.presentation.ultrasound.view.UltrasoundEditScreen
import com.github.rodrigotimoteo.animally.presentation.vaccination.view.VaccinationEditScreen
import com.github.rodrigotimoteo.animally.presentation.weight.view.WeightEditScreen
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

        navigation<Route.AddEditWeight> { route ->
            WeightEditScreen(viewModel = koinViewModel { parametersOf(route.patientId, route.weightId) })
        }

        navigation<Route.AddEditDeworming> { route ->
            DewormingEditScreen(viewModel = koinViewModel { parametersOf(route.patientId, route.dewormingId) })
        }

        navigation<Route.AddEditDentistry> { route ->
            DentistryEditScreen(viewModel = koinViewModel { parametersOf(route.patientId, route.dentistryId) })
        }

        navigation<Route.AddEditLameness> { route ->
            LamenessEditScreen(viewModel = koinViewModel { parametersOf(route.patientId, route.lamenessId) })
        }

        navigation<Route.AddEditSurgery> { route ->
            SurgeryEditScreen(viewModel = koinViewModel { parametersOf(route.patientId, route.surgeryId) })
        }

        navigation<Route.AddEditMedication> { route ->
            MedicationEditScreen(viewModel = koinViewModel { parametersOf(route.patientId, route.medicationId) })
        }

        navigation<Route.AddEditLabResult> { route ->
            LabResultEditScreen(viewModel = koinViewModel { parametersOf(route.patientId, route.labResultId) })
        }

        navigation<Route.AddEditImaging> { route ->
            ImagingEditScreen(viewModel = koinViewModel { parametersOf(route.patientId, route.imagingId) })
        }

        navigation<Route.AddEditFarrierVisit> { route ->
            FarrierVisitEditScreen(viewModel = koinViewModel { parametersOf(route.patientId, route.farrierVisitId) })
        }

        navigation<Route.AddEditReproductionEvent> { route ->
            ReproductionEventEditScreen(
                viewModel = koinViewModel { parametersOf(route.patientId, route.reproductionEventId) },
            )
        }

        navigation<Route.AddEditUltrasound> { route ->
            UltrasoundEditScreen(viewModel = koinViewModel { parametersOf(route.patientId, route.ultrasoundId) })
        }

        navigation<Route.AddEditGestation> { route ->
            GestationEditScreen(viewModel = koinViewModel { parametersOf(route.patientId, route.gestationId) })
        }

        navigation<Route.AddEditReproMed> { route ->
            ReproMedicationEditScreen(viewModel = koinViewModel { parametersOf(route.patientId, route.reproMedId) })
        }

        navigation<Route.AddEditControlledSubstance> { route ->
            ControlledSubstanceEditScreen(
                viewModel = koinViewModel { parametersOf(route.patientId, route.substanceId) },
            )
        }

        navigation<Route.Settings> {
            SettingsScreen()
        }
    }
