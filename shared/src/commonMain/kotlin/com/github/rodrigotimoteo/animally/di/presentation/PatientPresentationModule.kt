package com.github.rodrigotimoteo.animally.di.presentation
import com.github.rodrigotimoteo.animally.bridge.ObjCHidden
import com.github.rodrigotimoteo.animally.di.dispatchers.IO_DISPATCHER
import com.github.rodrigotimoteo.animally.presentation.anamnese.AnamneseViewModel
import com.github.rodrigotimoteo.animally.presentation.care.UpcomingCareViewModel
import com.github.rodrigotimoteo.animally.presentation.ownerDetail.OwnerDetailViewModel
import com.github.rodrigotimoteo.animally.presentation.ownerEdit.OwnerEditViewModel
import com.github.rodrigotimoteo.animally.presentation.patientDetail.PatientDetailViewModel
import com.github.rodrigotimoteo.animally.presentation.patientEdit.PatientEditViewModel
import com.github.rodrigotimoteo.animally.presentation.timeline.TimelineViewModel
import com.github.rodrigotimoteo.animally.presentation.weight.WeightEditViewModel
import com.github.rodrigotimoteo.animally.presentation.weight.WeightListViewModel
import kotlinx.coroutines.CoroutineDispatcher
import org.koin.core.annotation.Module
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

@Module
@ObjCHidden
internal class PatientPresentationModule {
    fun provide() =
        module {
            patientOwnerViewModels()
            patientDetailViewModels()
            patientCareViewModels()
        }

    private fun org.koin.core.module.Module.patientOwnerViewModels() {
        viewModel { (ownerId: Long) ->
            OwnerDetailViewModel(
                ownerId = ownerId,
                getOwnerDetailUseCase = get(),
                patientRepository = get(),
                animallyNavigator = get(),
                ioDispatcher = get<CoroutineDispatcher>(named(IO_DISPATCHER)),
            )
        }
        viewModel { (ownerId: Long?) ->
            OwnerEditViewModel(
                ownerId = ownerId,
                getOwnerDetailUseCase = get(),
                saveOwnerUseCase = get(),
                animallyNavigator = get(),
                ioDispatcher = get<CoroutineDispatcher>(named(IO_DISPATCHER)),
            )
        }
        viewModel { (patientId: Long) ->
            PatientDetailViewModel(
                patientId = patientId,
                getPatientDetailUseCase = get(),
                getOwnerDetailUseCase = get(),
                animallyNavigator = get(),
                ioDispatcher = get<CoroutineDispatcher>(named(IO_DISPATCHER)),
            )
        }
    }

    private fun org.koin.core.module.Module.patientDetailViewModels() {
        viewModel { (patientId: Long?, initialOwnerId: Long?) ->
            PatientEditViewModel(
                patientId = patientId,
                initialOwnerId = initialOwnerId,
                getPatientDetailUseCase = get(),
                savePatientUseCase = get(),
                getOwnerListUseCase = get(),
                animallyNavigator = get(),
                ioDispatcher = get<CoroutineDispatcher>(named(IO_DISPATCHER)),
            )
        }
        viewModel { (patientId: Long, anamneseId: Long?) ->
            AnamneseViewModel(
                patientId = patientId,
                anamneseId = anamneseId,
                getAnamneseByPatientUseCase = get(),
                saveAnamneseUseCase = get(),
                animallyNavigator = get(),
                ioDispatcher = get<CoroutineDispatcher>(named(IO_DISPATCHER)),
            )
        }
        viewModel { (patientId: Long?) ->
            TimelineViewModel(
                patientId = patientId,
                getTimelineUseCase = get(),
                animallyNavigator = get(),
                ioDispatcher = get<CoroutineDispatcher>(named(IO_DISPATCHER)),
            )
        }
    }

    private fun org.koin.core.module.Module.patientCareViewModels() {
        viewModel { (patientId: Long) ->
            UpcomingCareViewModel(
                patientId = patientId,
                getUpcomingRemindersUseCase = get(),
                ioDispatcher = get<CoroutineDispatcher>(named(IO_DISPATCHER)),
            )
        }
        viewModel { (patientId: Long, weightId: Long?) ->
            WeightEditViewModel(
                patientId = patientId,
                weightId = weightId,
                getWeightDetailUseCase = get(),
                saveWeightUseCase = get(),
                animallyNavigator = get(),
                ioDispatcher = get<CoroutineDispatcher>(named(IO_DISPATCHER)),
            )
        }
        viewModel { (patientId: Long) ->
            WeightListViewModel(
                patientId = patientId,
                getWeightsByPatientUseCase = get(),
                deleteWeightUseCase = get(),
                animallyNavigator = get(),
                ioDispatcher = get<CoroutineDispatcher>(named(IO_DISPATCHER)),
            )
        }
    }
}
