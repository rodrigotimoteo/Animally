package com.github.rodrigotimoteo.animally.domain.export

import com.github.rodrigotimoteo.animally.domain.anamnese.IAnamneseRepository
import com.github.rodrigotimoteo.animally.domain.consultation.IConsultationRepository
import com.github.rodrigotimoteo.animally.domain.dentistry.IDentistryRepository
import com.github.rodrigotimoteo.animally.domain.deworming.IDewormingRepository
import com.github.rodrigotimoteo.animally.domain.vaccination.IVaccinationRepository
import com.github.rodrigotimoteo.animally.domain.weight.IWeightRepository
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single

/**
 * Gathers a patient's basic care records for CSV export.
 *
 * Covers anamnese, weight, consultation, vaccination, deworming and dentistry.
 * Only the fields of the returned [ExportRecords] that this use case owns are
 * populated; the remaining lists stay empty.
 */
@Single
class ExportBasicRecordsUseCase(
    @Provided private val anamneseRepository: IAnamneseRepository,
    @Provided private val weightRepository: IWeightRepository,
    @Provided private val consultationRepository: IConsultationRepository,
    @Provided private val vaccinationRepository: IVaccinationRepository,
    @Provided private val dewormingRepository: IDewormingRepository,
    @Provided private val dentistryRepository: IDentistryRepository,
) {
    operator fun invoke(patientId: Long): ExportRecords =
        ExportRecords(
            anamnese = listOfNotNull(anamneseRepository.getByPatient(patientId)),
            weights = weightRepository.getByPatient(patientId).orEmpty(),
            consultations = consultationRepository.getByPatient(patientId).orEmpty(),
            vaccinations = vaccinationRepository.getByPatient(patientId).orEmpty(),
            dewormings = dewormingRepository.getByPatient(patientId).orEmpty(),
            dentistries = dentistryRepository.getByPatient(patientId).orEmpty(),
        )
}
