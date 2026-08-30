package com.github.rodrigotimoteo.animally.di.presentation
import com.github.rodrigotimoteo.animally.bridge.ObjCHidden
import com.github.rodrigotimoteo.animally.di.dispatchers.IO_DISPATCHER
import com.github.rodrigotimoteo.animally.presentation.consultation.ConsultationEditViewModel
import com.github.rodrigotimoteo.animally.presentation.consultation.ConsultationListViewModel
import com.github.rodrigotimoteo.animally.presentation.lameness.LamenessListViewModel
import com.github.rodrigotimoteo.animally.presentation.medication.MedicationEditViewModel
import com.github.rodrigotimoteo.animally.presentation.medication.MedicationListViewModel
import com.github.rodrigotimoteo.animally.presentation.substance.ControlledSubstanceEditViewModel
import com.github.rodrigotimoteo.animally.presentation.substance.ControlledSubstanceListViewModel
import com.github.rodrigotimoteo.animally.presentation.surgery.SurgeryEditViewModel
import com.github.rodrigotimoteo.animally.presentation.surgery.SurgeryListViewModel
import kotlinx.coroutines.CoroutineDispatcher
import org.koin.core.annotation.Module
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

@Module
@ObjCHidden
internal class MedicalPresentationModule {
    fun provide() =
        module {
            consultationSurgeryViewModels()
            surgeryMedicationViewModels()
            substanceLamenessViewModels()
        }

    private fun org.koin.core.module.Module.consultationSurgeryViewModels() {
        viewModel { (patientId: Long, consultationId: Long?) ->
            ConsultationEditViewModel(
                patientId = patientId,
                consultationId = consultationId,
                getConsultationDetailUseCase = get(),
                saveConsultationUseCase = get(),
                animallyNavigator = get(),
                ioDispatcher = get<CoroutineDispatcher>(named(IO_DISPATCHER)),
            )
        }
        viewModel { (patientId: Long) ->
            ConsultationListViewModel(
                patientId = patientId,
                getConsultationsByPatientUseCase = get(),
                deleteConsultationUseCase = get(),
                animallyNavigator = get(),
                ioDispatcher = get<CoroutineDispatcher>(named(IO_DISPATCHER)),
            )
        }
        viewModel { (patientId: Long, surgeryId: Long?) ->
            SurgeryEditViewModel(
                patientId = patientId,
                surgeryId = surgeryId,
                getSurgeryDetailUseCase = get(),
                saveSurgeryUseCase = get(),
                animallyNavigator = get(),
                ioDispatcher = get<CoroutineDispatcher>(named(IO_DISPATCHER)),
            )
        }
    }

    private fun org.koin.core.module.Module.surgeryMedicationViewModels() {
        viewModel { (patientId: Long) ->
            SurgeryListViewModel(
                patientId = patientId,
                getSurgeriesByPatientUseCase = get(),
                deleteSurgeryUseCase = get(),
                animallyNavigator = get(),
                ioDispatcher = get<CoroutineDispatcher>(named(IO_DISPATCHER)),
            )
        }
        viewModel { (patientId: Long, medicationId: Long?) ->
            MedicationEditViewModel(
                patientId = patientId,
                medicationId = medicationId,
                getMedicationDetailUseCase = get(),
                saveMedicationUseCase = get(),
                animallyNavigator = get(),
                ioDispatcher = get<CoroutineDispatcher>(named(IO_DISPATCHER)),
            )
        }
        viewModel { (patientId: Long) ->
            MedicationListViewModel(
                patientId = patientId,
                getMedicationsByPatientUseCase = get(),
                deleteMedicationUseCase = get(),
                animallyNavigator = get(),
                ioDispatcher = get<CoroutineDispatcher>(named(IO_DISPATCHER)),
            )
        }
    }

    private fun org.koin.core.module.Module.substanceLamenessViewModels() {
        viewModel { (patientId: Long, substanceId: Long?) ->
            ControlledSubstanceEditViewModel(
                patientId = patientId,
                substanceId = substanceId,
                getControlledSubstanceDetailUseCase = get(),
                saveControlledSubstanceUseCase = get(),
                animallyNavigator = get(),
                ioDispatcher = get<CoroutineDispatcher>(named(IO_DISPATCHER)),
            )
        }
        viewModel { (patientId: Long) ->
            ControlledSubstanceListViewModel(
                patientId = patientId,
                getControlledSubstancesByPatientUseCase = get(),
                deleteControlledSubstanceUseCase = get(),
                animallyNavigator = get(),
                ioDispatcher = get<CoroutineDispatcher>(named(IO_DISPATCHER)),
            )
        }
        viewModel { (patientId: Long) ->
            LamenessListViewModel(
                patientId = patientId,
                getLamenessListByPatientUseCase = get(),
                deleteLamenessUseCase = get(),
                animallyNavigator = get(),
                ioDispatcher = get<CoroutineDispatcher>(named(IO_DISPATCHER)),
            )
        }
    }
}
