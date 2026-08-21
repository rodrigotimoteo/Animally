package com.github.rodrigotimoteo.animally.di.presentation

import com.github.rodrigotimoteo.animally.bridge.ObjCHidden
import com.github.rodrigotimoteo.animally.di.dispatchers.IO_DISPATCHER
import com.github.rodrigotimoteo.animally.presentation.anamnese.AnamneseViewModel
import com.github.rodrigotimoteo.animally.presentation.consultation.ConsultationEditViewModel
import com.github.rodrigotimoteo.animally.presentation.consultation.ConsultationListViewModel
import com.github.rodrigotimoteo.animally.presentation.customreminder.CustomReminderEditViewModel
import com.github.rodrigotimoteo.animally.presentation.customreminder.CustomReminderListViewModel
import com.github.rodrigotimoteo.animally.presentation.dentistry.DentistryEditViewModel
import com.github.rodrigotimoteo.animally.presentation.dentistry.DentistryListViewModel
import com.github.rodrigotimoteo.animally.presentation.deworming.DewormingEditViewModel
import com.github.rodrigotimoteo.animally.presentation.deworming.DewormingListViewModel
import com.github.rodrigotimoteo.animally.presentation.farrier.FarrierVisitEditViewModel
import com.github.rodrigotimoteo.animally.presentation.farrier.FarrierVisitListViewModel
import com.github.rodrigotimoteo.animally.presentation.gestation.GestationEditViewModel
import com.github.rodrigotimoteo.animally.presentation.gestation.GestationListViewModel
import com.github.rodrigotimoteo.animally.presentation.imaging.ImagingEditViewModel
import com.github.rodrigotimoteo.animally.presentation.imaging.ImagingListViewModel
import com.github.rodrigotimoteo.animally.presentation.labresult.LabResultEditViewModel
import com.github.rodrigotimoteo.animally.presentation.labresult.LabResultListViewModel
import com.github.rodrigotimoteo.animally.presentation.lameness.LamenessEditViewModel
import com.github.rodrigotimoteo.animally.presentation.lameness.LamenessListViewModel
import com.github.rodrigotimoteo.animally.presentation.medication.MedicationEditViewModel
import com.github.rodrigotimoteo.animally.presentation.medication.MedicationListViewModel
import com.github.rodrigotimoteo.animally.presentation.ownerDetail.OwnerDetailViewModel
import com.github.rodrigotimoteo.animally.presentation.ownerEdit.OwnerEditViewModel
import com.github.rodrigotimoteo.animally.presentation.patientDetail.PatientDetailViewModel
import com.github.rodrigotimoteo.animally.presentation.patientEdit.PatientEditViewModel
import com.github.rodrigotimoteo.animally.presentation.reproduction.ReproductionEventEditViewModel
import com.github.rodrigotimoteo.animally.presentation.reproduction.ReproductionEventListViewModel
import com.github.rodrigotimoteo.animally.presentation.repromedication.ReproMedicationEditViewModel
import com.github.rodrigotimoteo.animally.presentation.repromedication.ReproMedicationListViewModel
import com.github.rodrigotimoteo.animally.presentation.substance.ControlledSubstanceEditViewModel
import com.github.rodrigotimoteo.animally.presentation.substance.ControlledSubstanceListViewModel
import com.github.rodrigotimoteo.animally.presentation.surgery.SurgeryEditViewModel
import com.github.rodrigotimoteo.animally.presentation.surgery.SurgeryListViewModel
import com.github.rodrigotimoteo.animally.presentation.timeline.TimelineViewModel
import com.github.rodrigotimoteo.animally.presentation.ultrasound.UltrasoundEditViewModel
import com.github.rodrigotimoteo.animally.presentation.ultrasound.UltrasoundListViewModel
import com.github.rodrigotimoteo.animally.presentation.vaccination.VaccinationEditViewModel
import com.github.rodrigotimoteo.animally.presentation.vaccination.VaccinationListViewModel
import com.github.rodrigotimoteo.animally.presentation.weight.WeightEditViewModel
import com.github.rodrigotimoteo.animally.presentation.weight.WeightListViewModel
import kotlinx.coroutines.CoroutineDispatcher
import org.koin.core.annotation.Module
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

/**
 * Module providing navigation-parameterized view models.
 */
@Module
@ObjCHidden
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
                    animallyNavigator = get(),
                    ioDispatcher = get<CoroutineDispatcher>(named(IO_DISPATCHER)),
                )
            }
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
            viewModel { (patientId: Long) ->
                ImagingListViewModel(
                    patientId = patientId,
                    getImagingListByPatientUseCase = get(),
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
            viewModel { (patientId: Long) ->
                LamenessListViewModel(
                    patientId = patientId,
                    getLamenessListByPatientUseCase = get(),
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
            viewModel { (patientId: Long) ->
                SurgeryListViewModel(
                    patientId = patientId,
                    getSurgeriesByPatientUseCase = get(),
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
                    animallyNavigator = get(),
                    ioDispatcher = get<CoroutineDispatcher>(named(IO_DISPATCHER)),
                )
            }
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
            viewModel { (patientId: Long) ->
                DentistryListViewModel(
                    patientId = patientId,
                    getDentistryListByPatientUseCase = get(),
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
                    animallyNavigator = get(),
                    ioDispatcher = get<CoroutineDispatcher>(named(IO_DISPATCHER)),
                )
            }
            viewModel { (patientId: Long, ultrasoundId: Long?) ->
                UltrasoundEditViewModel(
                    patientId = patientId,
                    ultrasoundId = ultrasoundId,
                    getUltrasoundDetailUseCase = get(),
                    saveUltrasoundUseCase = get(),
                    animallyNavigator = get(),
                    ioDispatcher = get<CoroutineDispatcher>(named(IO_DISPATCHER)),
                )
            }
            viewModel { (patientId: Long) ->
                UltrasoundListViewModel(
                    patientId = patientId,
                    getUltrasoundsByPatientUseCase = get(),
                    animallyNavigator = get(),
                    ioDispatcher = get<CoroutineDispatcher>(named(IO_DISPATCHER)),
                )
            }
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
            viewModel { (patientId: Long) ->
                GestationListViewModel(
                    patientId = patientId,
                    getGestationsByPatientUseCase = get(),
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
                    animallyNavigator = get(),
                    ioDispatcher = get<CoroutineDispatcher>(named(IO_DISPATCHER)),
                )
            }
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
            viewModel { (patientId: Long?) ->
                TimelineViewModel(
                    patientId = patientId,
                    getTimelineUseCase = get(),
                    animallyNavigator = get(),
                    ioDispatcher = get<CoroutineDispatcher>(named(IO_DISPATCHER)),
                )
            }
        }
}
