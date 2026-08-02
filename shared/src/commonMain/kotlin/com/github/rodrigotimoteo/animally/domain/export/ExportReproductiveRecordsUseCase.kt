package com.github.rodrigotimoteo.animally.domain.export

import com.github.rodrigotimoteo.animally.domain.gestation.IGestationRepository
import com.github.rodrigotimoteo.animally.domain.reproduction.IReproductionRepository
import com.github.rodrigotimoteo.animally.domain.repromedication.IReproMedicationRepository
import com.github.rodrigotimoteo.animally.domain.substance.IControlledSubstanceRepository
import com.github.rodrigotimoteo.animally.domain.ultrasound.IUltrasoundRepository
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single

/**
 * Gathers a patient's reproduction-related records for CSV export.
 *
 * Covers reproduction events, ultrasound, gestation, reproduction medication
 * and controlled substances. Only the fields of the returned [ExportRecords]
 * that this use case owns are populated; the remaining lists stay empty.
 */
@Single
class ExportReproductiveRecordsUseCase(
    @Provided private val reproductionRepository: IReproductionRepository,
    @Provided private val ultrasoundRepository: IUltrasoundRepository,
    @Provided private val gestationRepository: IGestationRepository,
    @Provided private val reproMedicationRepository: IReproMedicationRepository,
    @Provided private val substanceRepository: IControlledSubstanceRepository,
) {
    operator fun invoke(patientId: Long): ExportRecords =
        ExportRecords(
            reproductionEvents = reproductionRepository.getByPatient(patientId).orEmpty(),
            ultrasounds = ultrasoundRepository.getByPatient(patientId).orEmpty(),
            gestations = gestationRepository.getByPatient(patientId).orEmpty(),
            reproMedications = reproMedicationRepository.getByPatient(patientId).orEmpty(),
            controlledSubstances = substanceRepository.getByPatient(patientId).orEmpty(),
        )
}
