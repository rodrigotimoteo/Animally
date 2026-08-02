package com.github.rodrigotimoteo.animally.domain.export

import com.github.rodrigotimoteo.animally.domain.farrier.IFarrierVisitRepository
import com.github.rodrigotimoteo.animally.domain.imaging.IImagingRepository
import com.github.rodrigotimoteo.animally.domain.labresult.ILabResultRepository
import com.github.rodrigotimoteo.animally.domain.lameness.ILamenessRepository
import com.github.rodrigotimoteo.animally.domain.medication.IMedicationRepository
import com.github.rodrigotimoteo.animally.domain.surgery.ISurgeryRepository
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single

/**
 * Gathers a patient's clinical care records for CSV export.
 *
 * Covers lameness, surgery, medication, lab result, imaging and farrier visit.
 * Only the fields of the returned [ExportRecords] that this use case owns are
 * populated; the remaining lists stay empty.
 */
@Single
class ExportClinicalRecordsUseCase(
    @Provided private val lamenessRepository: ILamenessRepository,
    @Provided private val surgeryRepository: ISurgeryRepository,
    @Provided private val medicationRepository: IMedicationRepository,
    @Provided private val labResultRepository: ILabResultRepository,
    @Provided private val imagingRepository: IImagingRepository,
    @Provided private val farrierRepository: IFarrierVisitRepository,
) {
    operator fun invoke(patientId: Long): ExportRecords =
        ExportRecords(
            lamenesses = lamenessRepository.getByPatient(patientId).orEmpty(),
            surgeries = surgeryRepository.getByPatient(patientId).orEmpty(),
            medications = medicationRepository.getByPatient(patientId).orEmpty(),
            labResults = labResultRepository.getByPatient(patientId).orEmpty(),
            imagings = imagingRepository.getByPatient(patientId).orEmpty(),
            farrierVisits = farrierRepository.getByPatient(patientId).orEmpty(),
        )
}
