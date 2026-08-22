package com.github.rodrigotimoteo.animally.domain.sync

import com.github.rodrigotimoteo.animally.domain.sync.handlers.AnamneseSyncHandler
import com.github.rodrigotimoteo.animally.domain.sync.handlers.ConsultationSyncHandler
import com.github.rodrigotimoteo.animally.domain.sync.handlers.CustomReminderSyncHandler
import com.github.rodrigotimoteo.animally.domain.sync.handlers.DentistrySyncHandler
import com.github.rodrigotimoteo.animally.domain.sync.handlers.DewormingSyncHandler
import com.github.rodrigotimoteo.animally.domain.sync.handlers.EmbryoTransferSyncHandler
import com.github.rodrigotimoteo.animally.domain.sync.handlers.FarrierVisitSyncHandler
import com.github.rodrigotimoteo.animally.domain.sync.handlers.GestationSyncHandler
import com.github.rodrigotimoteo.animally.domain.sync.handlers.IcsiSyncHandler
import com.github.rodrigotimoteo.animally.domain.sync.handlers.ImagingSyncHandler
import com.github.rodrigotimoteo.animally.domain.sync.handlers.LabResultSyncHandler
import com.github.rodrigotimoteo.animally.domain.sync.handlers.LamenessSyncHandler
import com.github.rodrigotimoteo.animally.domain.sync.handlers.MedicationSyncHandler
import com.github.rodrigotimoteo.animally.domain.sync.handlers.OwnerSyncHandler
import com.github.rodrigotimoteo.animally.domain.sync.handlers.PatientSyncHandler
import com.github.rodrigotimoteo.animally.domain.sync.handlers.ReproMedicationSyncHandler
import com.github.rodrigotimoteo.animally.domain.sync.handlers.ReproductionSyncHandler
import com.github.rodrigotimoteo.animally.domain.sync.handlers.SubstanceSyncHandler
import com.github.rodrigotimoteo.animally.domain.sync.handlers.SurgerySyncHandler
import com.github.rodrigotimoteo.animally.domain.sync.handlers.UltrasoundSyncHandler
import com.github.rodrigotimoteo.animally.domain.sync.handlers.VaccinationSyncHandler
import com.github.rodrigotimoteo.animally.domain.sync.handlers.WeightSyncHandler
import org.koin.core.annotation.Single

/**
 * Resolves [SyncEntityHandler]s by entity type and exposes them in sync
 * dependency order.
 *
 * [all] ordering matters for pull: owners must land before patients, patients
 * before every patient-linked child (Anamnese is 1:1 with a patient and
 * belongs with the parents, before the many-per-patient children).
 */
@Suppress("LongParameterList")
@Single
class SyncEntityHandlerRegistry(
    ownerHandler: OwnerSyncHandler,
    patientHandler: PatientSyncHandler,
    anamneseHandler: AnamneseSyncHandler,
    consultationHandler: ConsultationSyncHandler,
    dentistryHandler: DentistrySyncHandler,
    dewormingHandler: DewormingSyncHandler,
    farrierVisitHandler: FarrierVisitSyncHandler,
    gestationHandler: GestationSyncHandler,
    imagingHandler: ImagingSyncHandler,
    labResultHandler: LabResultSyncHandler,
    lamenessHandler: LamenessSyncHandler,
    medicationHandler: MedicationSyncHandler,
    reproductionHandler: ReproductionSyncHandler,
    reproMedicationHandler: ReproMedicationSyncHandler,
    substanceHandler: SubstanceSyncHandler,
    surgeryHandler: SurgerySyncHandler,
    ultrasoundHandler: UltrasoundSyncHandler,
    vaccinationHandler: VaccinationSyncHandler,
    weightHandler: WeightSyncHandler,
    customReminderHandler: CustomReminderSyncHandler,
    embryoTransferHandler: EmbryoTransferSyncHandler,
    icsiHandler: IcsiSyncHandler,
) {
    private val allHandlers: List<SyncEntityHandler> =
        listOf(
            ownerHandler,
            patientHandler,
            anamneseHandler,
            consultationHandler,
            dentistryHandler,
            dewormingHandler,
            farrierVisitHandler,
            gestationHandler,
            imagingHandler,
            labResultHandler,
            lamenessHandler,
            medicationHandler,
            reproductionHandler,
            reproMedicationHandler,
            substanceHandler,
            surgeryHandler,
            ultrasoundHandler,
            vaccinationHandler,
            weightHandler,
            customReminderHandler,
            embryoTransferHandler,
            icsiHandler,
        )

    private val byType: Map<SyncEntityType, SyncEntityHandler> = allHandlers.associateBy { it.entityType }

    /** Resolves the handler for [type], or throws when none is registered. */
    fun handlerFor(type: SyncEntityType): SyncEntityHandler = byType.getValue(type)

    /** All handlers in sync dependency order: OWNER, PATIENT, ANAMNESE, then patient-linked. */
    fun all(): List<SyncEntityHandler> = allHandlers
}
