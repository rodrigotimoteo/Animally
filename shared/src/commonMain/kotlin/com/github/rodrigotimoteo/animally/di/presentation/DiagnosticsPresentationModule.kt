package com.github.rodrigotimoteo.animally.di.presentation
import com.github.rodrigotimoteo.animally.bridge.ObjCHidden
import com.github.rodrigotimoteo.animally.di.dispatchers.IO_DISPATCHER
import com.github.rodrigotimoteo.animally.presentation.deworming.DewormingEditViewModel
import com.github.rodrigotimoteo.animally.presentation.deworming.DewormingListViewModel
import com.github.rodrigotimoteo.animally.presentation.imaging.ImagingEditViewModel
import com.github.rodrigotimoteo.animally.presentation.imaging.ImagingListViewModel
import com.github.rodrigotimoteo.animally.presentation.labresult.LabResultEditViewModel
import com.github.rodrigotimoteo.animally.presentation.labresult.LabResultListViewModel
import com.github.rodrigotimoteo.animally.presentation.lameness.LamenessEditViewModel
import com.github.rodrigotimoteo.animally.presentation.vaccination.VaccinationEditViewModel
import com.github.rodrigotimoteo.animally.presentation.vaccination.VaccinationListViewModel
import kotlinx.coroutines.CoroutineDispatcher
import org.koin.core.annotation.Module
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

@Module
@ObjCHidden
internal class DiagnosticsPresentationModule {
    fun provide() =
        module {
            labImagingViewModels()
            imagingLamenessVaccinationViewModels()
            preventiveViewModels()
        }

    private fun org.koin.core.module.Module.labImagingViewModels() {
        viewModel { (patientId: Long, labResultId: Long?) ->
            LabResultEditViewModel(
                patientId = patientId,
                labResultId = labResultId,
                getLabResultDetailUseCase = get(),
                saveLabResultUseCase = get(),
                animallyNavigator = get(),
                ioDispatcher = get<CoroutineDispatcher>(named(IO_DISPATCHER)),
            )
        }
        viewModel { (patientId: Long) ->
            LabResultListViewModel(
                patientId = patientId,
                getLabResultsByPatientUseCase = get(),
                deleteLabResultUseCase = get(),
                animallyNavigator = get(),
                ioDispatcher = get<CoroutineDispatcher>(named(IO_DISPATCHER)),
            )
        }
        viewModel { (patientId: Long, imagingId: Long?) ->
            ImagingEditViewModel(
                patientId = patientId,
                imagingId = imagingId,
                getImagingDetailUseCase = get(),
                saveImagingUseCase = get(),
                animallyNavigator = get(),
                ioDispatcher = get<CoroutineDispatcher>(named(IO_DISPATCHER)),
            )
        }
    }

    private fun org.koin.core.module.Module.imagingLamenessVaccinationViewModels() {
        viewModel { (patientId: Long) ->
            ImagingListViewModel(
                patientId = patientId,
                getImagingListByPatientUseCase = get(),
                deleteImagingUseCase = get(),
                animallyNavigator = get(),
                ioDispatcher = get<CoroutineDispatcher>(named(IO_DISPATCHER)),
            )
        }
        viewModel { (patientId: Long, lamenessId: Long?) ->
            LamenessEditViewModel(
                patientId = patientId,
                lamenessId = lamenessId,
                getLamenessDetailUseCase = get(),
                saveLamenessUseCase = get(),
                animallyNavigator = get(),
                ioDispatcher = get<CoroutineDispatcher>(named(IO_DISPATCHER)),
            )
        }
        viewModel { (patientId: Long, vaccinationId: Long?) ->
            VaccinationEditViewModel(
                patientId = patientId,
                vaccinationId = vaccinationId,
                getVaccinationDetailUseCase = get(),
                saveVaccinationUseCase = get(),
                animallyNavigator = get(),
                ioDispatcher = get<CoroutineDispatcher>(named(IO_DISPATCHER)),
            )
        }
    }

    private fun org.koin.core.module.Module.preventiveViewModels() {
        viewModel { (patientId: Long) ->
            VaccinationListViewModel(
                patientId = patientId,
                getVaccinationsByPatientUseCase = get(),
                deleteVaccinationUseCase = get(),
                animallyNavigator = get(),
                ioDispatcher = get<CoroutineDispatcher>(named(IO_DISPATCHER)),
            )
        }
        viewModel { (patientId: Long, dewormingId: Long?) ->
            DewormingEditViewModel(
                patientId = patientId,
                dewormingId = dewormingId,
                getDewormingDetailUseCase = get(),
                saveDewormingUseCase = get(),
                animallyNavigator = get(),
                ioDispatcher = get<CoroutineDispatcher>(named(IO_DISPATCHER)),
            )
        }
        viewModel { (patientId: Long) ->
            DewormingListViewModel(
                patientId = patientId,
                getDewormingsByPatientUseCase = get(),
                deleteDewormingUseCase = get(),
                animallyNavigator = get(),
                ioDispatcher = get<CoroutineDispatcher>(named(IO_DISPATCHER)),
            )
        }
    }
}
