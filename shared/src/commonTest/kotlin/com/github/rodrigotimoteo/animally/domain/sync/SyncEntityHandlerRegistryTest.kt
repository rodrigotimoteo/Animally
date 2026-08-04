@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class)

package com.github.rodrigotimoteo.animally.domain.sync

import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.data.anamnese.AnamneseRepositoryImpl
import com.github.rodrigotimoteo.animally.data.consultation.ConsultationRepositoryImpl
import com.github.rodrigotimoteo.animally.data.customreminder.CustomReminderRepositoryImpl
import com.github.rodrigotimoteo.animally.data.dentistry.DentistryRepositoryImpl
import com.github.rodrigotimoteo.animally.data.deworming.DewormingRepositoryImpl
import com.github.rodrigotimoteo.animally.data.farrier.FarrierVisitRepositoryImpl
import com.github.rodrigotimoteo.animally.data.gestation.GestationRepositoryImpl
import com.github.rodrigotimoteo.animally.data.imaging.ImagingRepositoryImpl
import com.github.rodrigotimoteo.animally.data.labresult.LabResultRepositoryImpl
import com.github.rodrigotimoteo.animally.data.lameness.LamenessRepositoryImpl
import com.github.rodrigotimoteo.animally.data.medication.MedicationRepositoryImpl
import com.github.rodrigotimoteo.animally.data.owner.OwnerRepositoryImpl
import com.github.rodrigotimoteo.animally.data.patient.PatientRepositoryImpl
import com.github.rodrigotimoteo.animally.data.reproduction.ReproductionRepositoryImpl
import com.github.rodrigotimoteo.animally.data.repromedication.ReproMedicationRepositoryImpl
import com.github.rodrigotimoteo.animally.data.substance.ControlledSubstanceRepositoryImpl
import com.github.rodrigotimoteo.animally.data.surgery.SurgeryRepositoryImpl
import com.github.rodrigotimoteo.animally.data.ultrasound.UltrasoundRepositoryImpl
import com.github.rodrigotimoteo.animally.data.vaccination.VaccinationRepositoryImpl
import com.github.rodrigotimoteo.animally.data.weight.WeightRepositoryImpl
import com.github.rodrigotimoteo.animally.di.database.createTestDatabase
import com.github.rodrigotimoteo.animally.domain.sync.handlers.AnamneseSyncHandler
import com.github.rodrigotimoteo.animally.domain.sync.handlers.ConsultationSyncHandler
import com.github.rodrigotimoteo.animally.domain.sync.handlers.CustomReminderSyncHandler
import com.github.rodrigotimoteo.animally.domain.sync.handlers.DentistrySyncHandler
import com.github.rodrigotimoteo.animally.domain.sync.handlers.DewormingSyncHandler
import com.github.rodrigotimoteo.animally.domain.sync.handlers.FarrierVisitSyncHandler
import com.github.rodrigotimoteo.animally.domain.sync.handlers.GestationSyncHandler
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
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class SyncEntityHandlerRegistryTest {
    private lateinit var database: AnimallyDatabase
    private lateinit var sut: SyncEntityHandlerRegistry

    @BeforeTest
    fun setup() {
        database = createTestDatabase()
        val ownerRepo = OwnerRepositoryImpl(database.ownerQueries)
        val patientRepo = PatientRepositoryImpl(database)
        val anamneseRepo = AnamneseRepositoryImpl(database)
        val consultationRepo = ConsultationRepositoryImpl(database)
        val dentistryRepo = DentistryRepositoryImpl(database)
        val dewormingRepo = DewormingRepositoryImpl(database)
        val farrierRepo = FarrierVisitRepositoryImpl(database)
        val gestationRepo = GestationRepositoryImpl(database)
        val imagingRepo = ImagingRepositoryImpl(database)
        val labResultRepo = LabResultRepositoryImpl(database)
        val lamenessRepo = LamenessRepositoryImpl(database)
        val medicationRepo = MedicationRepositoryImpl(database)
        val reproRepo = ReproductionRepositoryImpl(database)
        val reproMedRepo = ReproMedicationRepositoryImpl(database)
        val substanceRepo = ControlledSubstanceRepositoryImpl(database)
        val surgeryRepo = SurgeryRepositoryImpl(database)
        val ultrasoundRepo = UltrasoundRepositoryImpl(database)
        val vaccinationRepo = VaccinationRepositoryImpl(database)
        val weightRepo = WeightRepositoryImpl(database)
        val customReminderRepo = CustomReminderRepositoryImpl(database)

        sut =
            SyncEntityHandlerRegistry(
                ownerHandler = OwnerSyncHandler(ownerRepo, database),
                patientHandler = PatientSyncHandler(patientRepo, database),
                anamneseHandler = AnamneseSyncHandler(anamneseRepo, patientRepo, database),
                consultationHandler = ConsultationSyncHandler(consultationRepo, patientRepo, database),
                dentistryHandler = DentistrySyncHandler(dentistryRepo, patientRepo, database),
                dewormingHandler = DewormingSyncHandler(dewormingRepo, patientRepo, database),
                farrierVisitHandler = FarrierVisitSyncHandler(farrierRepo, patientRepo, database),
                gestationHandler = GestationSyncHandler(gestationRepo, patientRepo, database),
                imagingHandler = ImagingSyncHandler(imagingRepo, patientRepo, database),
                labResultHandler = LabResultSyncHandler(labResultRepo, patientRepo, database),
                lamenessHandler = LamenessSyncHandler(lamenessRepo, patientRepo, database),
                medicationHandler = MedicationSyncHandler(medicationRepo, patientRepo, database),
                reproductionHandler = ReproductionSyncHandler(reproRepo, patientRepo, database),
                reproMedicationHandler = ReproMedicationSyncHandler(reproMedRepo, patientRepo, database),
                substanceHandler = SubstanceSyncHandler(substanceRepo, patientRepo, database),
                surgeryHandler = SurgerySyncHandler(surgeryRepo, patientRepo, database),
                ultrasoundHandler = UltrasoundSyncHandler(ultrasoundRepo, patientRepo, database),
                vaccinationHandler = VaccinationSyncHandler(vaccinationRepo, patientRepo, database),
                weightHandler = WeightSyncHandler(weightRepo, patientRepo, database),
                customReminderHandler = CustomReminderSyncHandler(customReminderRepo, patientRepo, database),
            )
    }

    @Test
    fun `when resolving every entity type then returns matching handler`() {
        SyncEntityType.entries.forEach { type ->
            assertSame(type, sut.handlerFor(type).entityType, "handler for $type")
        }
    }

    @Test
    fun `when listing all handlers then covers every entity type`() {
        assertEquals(SyncEntityType.entries.toSet(), sut.all().map { it.entityType }.toSet())
    }

    @Test
    fun `when listing all handlers then ordered parents before patient-linked children`() {
        val types = sut.all().map { it.entityType }

        assertEquals(20, types.size)
        assertEquals(SyncEntityType.OWNER, types[0])
        assertEquals(SyncEntityType.PATIENT, types[1])
        assertEquals(SyncEntityType.ANAMNESE, types[2])
        // Anamnese (1:1) lands with the parents; no child may precede it.
        val patientLinked = types.drop(3)
        assert(patientLinked.none { it == SyncEntityType.OWNER || it == SyncEntityType.PATIENT || it == SyncEntityType.ANAMNESE })
    }
}
