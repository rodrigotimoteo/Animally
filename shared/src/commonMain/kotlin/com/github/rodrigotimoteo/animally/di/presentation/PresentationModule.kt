package com.github.rodrigotimoteo.animally.di.presentation

import com.github.rodrigotimoteo.animally.di.dispatchers.IO_DISPATCHER
import com.github.rodrigotimoteo.animally.presentation.anamnese.AnamneseViewModel
import com.github.rodrigotimoteo.animally.presentation.consultation.ConsultationEditViewModel
import com.github.rodrigotimoteo.animally.presentation.consultation.ConsultationListViewModel
import com.github.rodrigotimoteo.animally.presentation.ownerDetail.OwnerDetailViewModel
import com.github.rodrigotimoteo.animally.presentation.ownerEdit.OwnerEditViewModel
import com.github.rodrigotimoteo.animally.presentation.patientDetail.PatientDetailViewModel
import com.github.rodrigotimoteo.animally.presentation.patientEdit.PatientEditViewModel
import com.github.rodrigotimoteo.animally.presentation.vaccination.VaccinationEditViewModel
import com.github.rodrigotimoteo.animally.presentation.vaccination.VaccinationListViewModel
import kotlinx.coroutines.CoroutineDispatcher
import org.koin.core.annotation.Module
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

/**
 * Module providing navigation-parameterized view models.
 */
@Module
internal class PresentationModule {
    @Suppress("LongMethod")
    fun provide() =
        module {
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
            viewModel { (patientId: Long?) ->
                PatientEditViewModel(
                    patientId = patientId,
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
            viewModel { (patientId: Long) ->
                VaccinationListViewModel(
                    patientId = patientId,
                    getVaccinationsByPatientUseCase = get(),
                    animallyNavigator = get(),
                    ioDispatcher = get<CoroutineDispatcher>(named(IO_DISPATCHER)),
                )
            }
        }
}
