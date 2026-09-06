package com.github.rodrigotimoteo.animally.domain.export

import com.github.rodrigotimoteo.animally.domain.customreminder.ICustomReminderRepository
import com.github.rodrigotimoteo.animally.domain.embryotransfer.IEmbryoTransferRepository
import com.github.rodrigotimoteo.animally.domain.follicle.IFollicleRepository
import com.github.rodrigotimoteo.animally.domain.gestation.IGestationRepository
import com.github.rodrigotimoteo.animally.domain.icsi.IIcsiRepository
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
 * Each repository is a separate record family, so the dependencies remain
 * explicit instead of being hidden behind an untyped aggregate.
 */
@Single
@Suppress("LongParameterList")
class ExportReproductiveRecordsUseCase(
    @Provided private val reproductionRepository: IReproductionRepository,
    @Provided private val ultrasoundRepository: IUltrasoundRepository,
    @Provided private val gestationRepository: IGestationRepository,
    @Provided private val reproMedicationRepository: IReproMedicationRepository,
    @Provided private val substanceRepository: IControlledSubstanceRepository,
    @Provided private val customReminderRepository: ICustomReminderRepository,
    @Provided private val embryoTransferRepository: IEmbryoTransferRepository,
    @Provided private val icsiRepository: IIcsiRepository,
    @Provided private val follicleRepository: IFollicleRepository,
) {
    operator fun invoke(patientId: Long): ExportRecords {
        val ultrasounds = ultrasoundRepository.getByPatient(patientId).orEmpty()
        return ExportRecords(
            reproductionEvents = reproductionRepository.getByPatient(patientId).orEmpty(),
            ultrasounds = ultrasounds,
            gestations = gestationRepository.getByPatient(patientId).orEmpty(),
            reproMedications = reproMedicationRepository.getByPatient(patientId).orEmpty(),
            controlledSubstances = substanceRepository.getByPatient(patientId).orEmpty(),
            customReminders = customReminderRepository.getByPatient(patientId),
            embryoTransfers = embryoTransferRepository.getByPatient(patientId),
            icsi = icsiRepository.getByPatient(patientId),
            follicles = ultrasounds.flatMap { follicleRepository.getByUltrasound(it.id) },
        )
    }
}
