package com.github.rodrigotimoteo.animally.di.presentation
import com.github.rodrigotimoteo.animally.bridge.ObjCHidden
import com.github.rodrigotimoteo.animally.di.dispatchers.IO_DISPATCHER
import com.github.rodrigotimoteo.animally.presentation.customreminder.CustomReminderEditViewModel
import com.github.rodrigotimoteo.animally.presentation.customreminder.CustomReminderListViewModel
import com.github.rodrigotimoteo.animally.presentation.dentistry.DentistryEditViewModel
import com.github.rodrigotimoteo.animally.presentation.dentistry.DentistryListViewModel
import com.github.rodrigotimoteo.animally.presentation.farrier.FarrierVisitEditViewModel
import com.github.rodrigotimoteo.animally.presentation.farrier.FarrierVisitListViewModel
import com.github.rodrigotimoteo.animally.presentation.gestation.GestationEditViewModel
import com.github.rodrigotimoteo.animally.presentation.repromedication.ReproMedicationEditViewModel
import com.github.rodrigotimoteo.animally.presentation.repromedication.ReproMedicationListViewModel
import kotlinx.coroutines.CoroutineDispatcher
import org.koin.core.annotation.Module
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

@Module
@ObjCHidden
internal class CorePresentationModule {
    fun provide() =
        module {
            gestationReproMedViewModels()
            customReminderDentistryViewModels()
            farrierViewModels()
        }

    private fun org.koin.core.module.Module.gestationReproMedViewModels() {
        viewModel { (patientId: Long, gestationId: Long?) ->
            GestationEditViewModel(
                patientId = patientId,
                gestationId = gestationId,
                getGestationDetailUseCase = get(),
                saveGestationUseCase = get(),
                animallyNavigator = get(),
                ioDispatcher = get<CoroutineDispatcher>(named(IO_DISPATCHER)),
            )
        }
        viewModel { (patientId: Long, reproMedId: Long?) ->
            ReproMedicationEditViewModel(
                patientId = patientId,
                reproMedId = reproMedId,
                getReproMedicationDetailUseCase = get(),
                saveReproMedicationUseCase = get(),
                animallyNavigator = get(),
                ioDispatcher = get<CoroutineDispatcher>(named(IO_DISPATCHER)),
            )
        }
        viewModel { (patientId: Long) ->
            ReproMedicationListViewModel(
                patientId = patientId,
                getReproMedicationsByPatientUseCase = get(),
                deleteReproMedicationUseCase = get(),
                animallyNavigator = get(),
                ioDispatcher = get<CoroutineDispatcher>(named(IO_DISPATCHER)),
            )
        }
    }

    private fun org.koin.core.module.Module.customReminderDentistryViewModels() {
        viewModel { (patientId: Long) ->
            CustomReminderListViewModel(
                patientId = patientId,
                getCustomRemindersByPatientUseCase = get(),
                deleteCustomReminderUseCase = get(),
                animallyNavigator = get(),
                ioDispatcher = get<CoroutineDispatcher>(named(IO_DISPATCHER)),
            )
        }
        viewModel { (patientId: Long, reminderId: Long?) ->
            CustomReminderEditViewModel(
                patientId = patientId,
                reminderId = reminderId,
                getCustomReminderDetailUseCase = get(),
                saveCustomReminderUseCase = get(),
                animallyNavigator = get(),
                ioDispatcher = get<CoroutineDispatcher>(named(IO_DISPATCHER)),
            )
        }
        viewModel { (patientId: Long, dentistryId: Long?) ->
            DentistryEditViewModel(
                patientId = patientId,
                dentistryId = dentistryId,
                getDentistryDetailUseCase = get(),
                saveDentistryUseCase = get(),
                animallyNavigator = get(),
                ioDispatcher = get<CoroutineDispatcher>(named(IO_DISPATCHER)),
            )
        }
    }

    private fun org.koin.core.module.Module.farrierViewModels() {
        viewModel { (patientId: Long) ->
            DentistryListViewModel(
                patientId = patientId,
                getDentistryListByPatientUseCase = get(),
                deleteDentistryUseCase = get(),
                animallyNavigator = get(),
                ioDispatcher = get<CoroutineDispatcher>(named(IO_DISPATCHER)),
            )
        }
        viewModel { (patientId: Long, farrierVisitId: Long?) ->
            FarrierVisitEditViewModel(
                patientId = patientId,
                farrierVisitId = farrierVisitId,
                getFarrierVisitDetailUseCase = get(),
                saveFarrierVisitUseCase = get(),
                animallyNavigator = get(),
                ioDispatcher = get<CoroutineDispatcher>(named(IO_DISPATCHER)),
            )
        }
        viewModel { (patientId: Long) ->
            FarrierVisitListViewModel(
                patientId = patientId,
                getFarrierVisitsByPatientUseCase = get(),
                deleteFarrierVisitUseCase = get(),
                animallyNavigator = get(),
                ioDispatcher = get<CoroutineDispatcher>(named(IO_DISPATCHER)),
            )
        }
    }
}
