package com.github.rodrigotimoteo.animally.di.presentation
import com.github.rodrigotimoteo.animally.bridge.ObjCHidden
import com.github.rodrigotimoteo.animally.di.dispatchers.IO_DISPATCHER
import com.github.rodrigotimoteo.animally.presentation.embryotransfer.EmbryoTransferEditViewModel
import com.github.rodrigotimoteo.animally.presentation.embryotransfer.EmbryoTransferListViewModel
import com.github.rodrigotimoteo.animally.presentation.gestation.GestationListViewModel
import com.github.rodrigotimoteo.animally.presentation.icsi.IcsiEditViewModel
import com.github.rodrigotimoteo.animally.presentation.icsi.IcsiListViewModel
import com.github.rodrigotimoteo.animally.presentation.reproduction.ReproductionEventEditViewModel
import com.github.rodrigotimoteo.animally.presentation.reproduction.ReproductionEventListViewModel
import com.github.rodrigotimoteo.animally.presentation.ultrasound.UltrasoundEditViewModel
import com.github.rodrigotimoteo.animally.presentation.ultrasound.UltrasoundListViewModel
import kotlinx.coroutines.CoroutineDispatcher
import org.koin.core.annotation.Module
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

@Module
@ObjCHidden
internal class ReproductionPresentationModule {
    fun provide() =
        module {
            embryoIcsiViewModels()
            icsiReproEventViewModels()
            ultrasoundGestationViewModels()
        }

    private fun org.koin.core.module.Module.embryoIcsiViewModels() {
        viewModel { (patientId: Long, embryoTransferId: Long?) ->
            EmbryoTransferEditViewModel(
                patientId = patientId,
                embryoTransferId = embryoTransferId,
                getEmbryoTransferDetailUseCase = get(),
                saveEmbryoTransferUseCase = get(),
                animallyNavigator = get(),
                ioDispatcher = get<CoroutineDispatcher>(named(IO_DISPATCHER)),
            )
        }
        viewModel { (patientId: Long) ->
            EmbryoTransferListViewModel(
                patientId = patientId,
                getEmbryoTransfersByPatientUseCase = get(),
                deleteEmbryoTransferUseCase = get(),
                animallyNavigator = get(),
                ioDispatcher = get<CoroutineDispatcher>(named(IO_DISPATCHER)),
            )
        }
        viewModel { (patientId: Long, icsiId: Long?) ->
            IcsiEditViewModel(
                patientId = patientId,
                icsiId = icsiId,
                getIcsiDetailUseCase = get(),
                saveIcsiUseCase = get(),
                animallyNavigator = get(),
                ioDispatcher = get<CoroutineDispatcher>(named(IO_DISPATCHER)),
            )
        }
    }

    private fun org.koin.core.module.Module.icsiReproEventViewModels() {
        viewModel { (patientId: Long) ->
            IcsiListViewModel(
                patientId = patientId,
                getIcsiByPatientUseCase = get(),
                deleteIcsiUseCase = get(),
                animallyNavigator = get(),
                ioDispatcher = get<CoroutineDispatcher>(named(IO_DISPATCHER)),
            )
        }
        viewModel { (patientId: Long, reproductionEventId: Long?) ->
            ReproductionEventEditViewModel(
                patientId = patientId,
                reproductionEventId = reproductionEventId,
                getReproductionEventDetailUseCase = get(),
                saveReproductionEventUseCase = get(),
                animallyNavigator = get(),
                ioDispatcher = get<CoroutineDispatcher>(named(IO_DISPATCHER)),
            )
        }
        viewModel { (patientId: Long) ->
            ReproductionEventListViewModel(
                patientId = patientId,
                getReproductionEventsByPatientUseCase = get(),
                deleteReproductionEventUseCase = get(),
                animallyNavigator = get(),
                ioDispatcher = get<CoroutineDispatcher>(named(IO_DISPATCHER)),
            )
        }
    }

    private fun org.koin.core.module.Module.ultrasoundGestationViewModels() {
        viewModel { (patientId: Long, ultrasoundId: Long?) ->
            UltrasoundEditViewModel(
                patientId = patientId,
                ultrasoundId = ultrasoundId,
                getUltrasoundDetailUseCase = get(),
                saveUltrasoundUseCase = get(),
                getFolliclesByUltrasoundUseCase = get(),
                saveFolliclesUseCase = get(),
                animallyNavigator = get(),
                ioDispatcher = get<CoroutineDispatcher>(named(IO_DISPATCHER)),
            )
        }
        viewModel { (patientId: Long) ->
            UltrasoundListViewModel(
                patientId = patientId,
                getUltrasoundsByPatientUseCase = get(),
                deleteUltrasoundUseCase = get(),
                animallyNavigator = get(),
                ioDispatcher = get<CoroutineDispatcher>(named(IO_DISPATCHER)),
            )
        }
        viewModel { (patientId: Long) ->
            GestationListViewModel(
                patientId = patientId,
                getGestationsByPatientUseCase = get(),
                deleteGestationUseCase = get(),
                animallyNavigator = get(),
                ioDispatcher = get<CoroutineDispatcher>(named(IO_DISPATCHER)),
            )
        }
    }
}
