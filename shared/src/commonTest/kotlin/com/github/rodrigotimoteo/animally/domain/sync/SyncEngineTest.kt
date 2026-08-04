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
import com.github.rodrigotimoteo.animally.data.sync.SyncChangeTrackerImpl
import com.github.rodrigotimoteo.animally.data.sync.SyncEngineImpl
import com.github.rodrigotimoteo.animally.data.sync.SyncMetadataRepositoryImpl
import com.github.rodrigotimoteo.animally.data.ultrasound.UltrasoundRepositoryImpl
import com.github.rodrigotimoteo.animally.data.vaccination.VaccinationRepositoryImpl
import com.github.rodrigotimoteo.animally.data.weight.WeightRepositoryImpl
import com.github.rodrigotimoteo.animally.di.database.createTestDatabase
import com.github.rodrigotimoteo.animally.domain.consultation.model.Consultation
import com.github.rodrigotimoteo.animally.domain.owner.model.Owner
import com.github.rodrigotimoteo.animally.domain.patient.model.Patient
import com.github.rodrigotimoteo.animally.domain.sync.handlers.AnamneseSyncHandler
import com.github.rodrigotimoteo.animally.domain.sync.handlers.ConsultationPayload
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
import com.github.rodrigotimoteo.animally.domain.sync.handlers.OwnerPayload
import com.github.rodrigotimoteo.animally.domain.sync.handlers.OwnerSyncHandler
import com.github.rodrigotimoteo.animally.domain.sync.handlers.PatientPayload
import com.github.rodrigotimoteo.animally.domain.sync.handlers.PatientSyncHandler
import com.github.rodrigotimoteo.animally.domain.sync.handlers.ReproMedicationSyncHandler
import com.github.rodrigotimoteo.animally.domain.sync.handlers.ReproductionSyncHandler
import com.github.rodrigotimoteo.animally.domain.sync.handlers.SubstanceSyncHandler
import com.github.rodrigotimoteo.animally.domain.sync.handlers.SurgerySyncHandler
import com.github.rodrigotimoteo.animally.domain.sync.handlers.SyncJson
import com.github.rodrigotimoteo.animally.domain.sync.handlers.UltrasoundSyncHandler
import com.github.rodrigotimoteo.animally.domain.sync.handlers.VaccinationSyncHandler
import com.github.rodrigotimoteo.animally.domain.sync.handlers.WeightSyncHandler
import com.github.rodrigotimoteo.animally.sync.InMemorySyncApi
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Instant

class SyncEngineTest {
    private lateinit var database: AnimallyDatabase
    private lateinit var api: InMemorySyncApi
    private lateinit var metadataRepository: SyncMetadataRepositoryImpl
    private lateinit var changeTracker: SyncChangeTrackerImpl
    private lateinit var registry: SyncEntityHandlerRegistry
    private lateinit var sut: SyncEngineImpl
    private lateinit var ownerRepo: OwnerRepositoryImpl
    private lateinit var patientRepo: PatientRepositoryImpl
    private lateinit var consultationRepo: ConsultationRepositoryImpl

    private val epoch = Instant.fromEpochMilliseconds(0)

    @BeforeTest
    fun setup() {
        database = createTestDatabase()
        api = InMemorySyncApi()
        metadataRepository = SyncMetadataRepositoryImpl(database)
        changeTracker = SyncChangeTrackerImpl(database)
        ownerRepo = OwnerRepositoryImpl(database.ownerQueries)
        patientRepo = PatientRepositoryImpl(database)
        consultationRepo = ConsultationRepositoryImpl(database)
        val anamneseRepo = AnamneseRepositoryImpl(database)
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

        registry =
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
        sut = SyncEngineImpl(api, metadataRepository, changeTracker, registry, database)
    }

    private fun seedOwner(
        name: String,
        updatedAt: Instant,
    ): Long =
        ownerRepo.insertOwner(
            Owner(
                id = 0L,
                name = name,
                email = null,
                phone = null,
                address = "Somewhere",
                isActive = true,
                createdAt = epoch,
                updatedAt = updatedAt,
            ),
        )

    private fun seedPatient(
        name: String,
        updatedAt: Instant,
        ownerId: Long,
    ): Long =
        patientRepo.insertPatient(
            Patient(
                id = 0L,
                name = name,
                ownerId = ownerId,
                createdAt = epoch,
                updatedAt = updatedAt,
            ),
        )

    private fun seedConsultation(
        patientId: Long,
        updatedAt: Instant,
    ): Long =
        consultationRepo.insert(
            Consultation(
                id = 0L,
                patientId = patientId,
                date = LocalDate(2024, 5, 1),
                subjective = "local",
                objective = "local",
                assessment = "local",
                plan = "local",
                isActive = true,
                createdAt = epoch,
                updatedAt = updatedAt,
            ),
        )

    private fun remoteOwnerRecord(
        serverId: String,
        updatedAt: Instant,
        name: String,
        isActive: Boolean = true,
    ): SyncRecord =
        SyncRecord(
            type = SyncEntityType.OWNER.wireName,
            serverId = serverId,
            updatedAt = updatedAt,
            isActive = isActive,
            payload =
                SyncJson
                    .encodeToJsonElement(
                        OwnerPayload.serializer(),
                        OwnerPayload(name = name, address = "Somewhere", createdAt = epoch),
                    ).jsonObject,
        )

    private fun remotePatientRecord(
        serverId: String,
        updatedAt: Instant,
        name: String,
        ownerServerId: String,
    ): SyncRecord =
        SyncRecord(
            type = SyncEntityType.PATIENT.wireName,
            serverId = serverId,
            updatedAt = updatedAt,
            parentServerIds = mapOf("ownerId" to ownerServerId),
            payload =
                SyncJson
                    .encodeToJsonElement(
                        PatientPayload.serializer(),
                        PatientPayload(name = name, createdAt = epoch),
                    ).jsonObject,
        )

    private fun remoteConsultationRecord(
        serverId: String,
        updatedAt: Instant,
        subjective: String,
        patientServerId: String,
    ): SyncRecord =
        SyncRecord(
            type = SyncEntityType.CONSULTATION.wireName,
            serverId = serverId,
            updatedAt = updatedAt,
            parentServerIds = mapOf("patientId" to patientServerId),
            payload =
                SyncJson
                    .encodeToJsonElement(
                        ConsultationPayload.serializer(),
                        ConsultationPayload(
                            date = LocalDate(2024, 5, 1),
                            subjective = subjective,
                            objective = "objective",
                            assessment = "assessment",
                            plan = "plan",
                            createdAt = epoch,
                        ),
                    ).jsonObject,
        )

    @Test
    fun `when first sync then assigns server ids to all local rows`() =
        runTest {
            val ownerId = seedOwner("Alice", Instant.fromEpochMilliseconds(100))
            val patientId = seedPatient("Bella", Instant.fromEpochMilliseconds(100), ownerId)
            val consultationId = seedConsultation(patientId, Instant.fromEpochMilliseconds(100))

            val result = sut.sync()

            assertTrue(result.success)
            assertEquals(3, result.pushedCount)
            assertEquals(0, result.rejectedCount)
            assertEquals("srv-Owner-$ownerId", ownerServerIdOf(ownerId))
            assertEquals("srv-Patient-$patientId", patientServerIdOf(patientId))
            assertEquals("srv-Consultation-$consultationId", consultationServerIdOf(consultationId))
        }

    @Test
    fun `when first sync then pushes owner before patient before consultation`() =
        runTest {
            val ownerId = seedOwner("Alice", Instant.fromEpochMilliseconds(100))
            val patientId = seedPatient("Bella", Instant.fromEpochMilliseconds(100), ownerId)
            seedConsultation(patientId, Instant.fromEpochMilliseconds(100))

            val result = sut.sync()

            assertTrue(result.success)
            assertEquals(
                listOf("Owner", "Patient", "Consultation"),
                api.storedRecords().map { it.type },
            )
        }

    @Test
    fun `when remote records exist then pull applies them in dependency order`() =
        runTest {
            api.seed(remoteOwnerRecord("o-1", Instant.fromEpochMilliseconds(1000), "Remote Owner"))
            api.seed(remotePatientRecord("p-1", Instant.fromEpochMilliseconds(1100), "Remote Bella", ownerServerId = "o-1"))
            api.seed(remoteConsultationRecord("c-1", Instant.fromEpochMilliseconds(1200), "Remote SOAP", patientServerId = "p-1"))

            val result = sut.sync()

            assertTrue(result.success)
            assertEquals(3, result.pulledCount)
            val owner =
                database.ownerQueries
                    .selectAll()
                    .executeAsList()
                    .single()
            assertEquals("Remote Owner", owner.name)
            assertEquals("o-1", owner.serverId)
            val patient =
                database.patientQueries
                    .selectAll()
                    .executeAsList()
                    .single()
            assertEquals("Remote Bella", patient.name)
            assertEquals("p-1", patient.serverId)
            assertEquals(owner.id, patient.ownerId)
            val consultation =
                database.consultationQueries
                    .selectAll()
                    .executeAsList()
                    .single()
            assertEquals("Remote SOAP", consultation.subjective)
            assertEquals("c-1", consultation.serverId)
            assertEquals(patient.id, consultation.patientId)
        }

    @Test
    fun `when remote record is newer then replaces local row`() =
        runTest {
            val id = seedOwner("Alice", Instant.fromEpochMilliseconds(100))
            database.ownerQueries.setServerId("o-1", Instant.fromEpochMilliseconds(100), id)
            api.seed(remoteOwnerRecord("o-1", Instant.fromEpochMilliseconds(200), "Alice Remote"))

            val result = sut.sync()

            assertTrue(result.success)
            assertEquals(1, result.rejectedCount)
            assertEquals(1, result.pulledCount)
            val row =
                database.ownerQueries
                    .selectAllRows()
                    .executeAsList()
                    .single()
            assertEquals("Alice Remote", row.name)
            assertEquals(Instant.fromEpochMilliseconds(200), row.updatedAt)
        }

    @Test
    fun `when local record is newer then keeps local change`() =
        runTest {
            val id = seedOwner("Alice", Instant.fromEpochMilliseconds(200))
            database.ownerQueries.setServerId("o-1", Instant.fromEpochMilliseconds(200), id)
            api.seed(remoteOwnerRecord("o-1", Instant.fromEpochMilliseconds(100), "Stale Remote"))

            val result = sut.sync()

            assertTrue(result.success)
            assertEquals(0, result.rejectedCount)
            val row =
                database.ownerQueries
                    .selectAllRows()
                    .executeAsList()
                    .single()
            assertEquals("Alice", row.name)
            assertEquals(
                "Alice",
                api
                    .storedRecords()
                    .single()
                    .payload["name"]
                    ?.jsonPrimitive
                    ?.content,
            )
        }

    @Test
    fun `when remote record has equal updatedAt then remote wins`() =
        runTest {
            val patientId =
                patientRepo.insertPatient(
                    Patient(id = 0L, name = "Bella", createdAt = epoch, updatedAt = epoch),
                )
            val tie = Instant.fromEpochMilliseconds(100)
            val consultationId =
                consultationRepo.insert(
                    Consultation(
                        id = 0L,
                        patientId = patientId,
                        date = LocalDate(2024, 5, 1),
                        subjective = "local",
                        objective = "local",
                        assessment = "local",
                        plan = "local",
                        createdAt = epoch,
                        updatedAt = tie,
                    ),
                )
            database.consultationQueries.setServerId("c-srv", tie, consultationId)
            api.seed(remoteConsultationRecord("c-srv", tie, "remote", patientServerId = "p-remote"))

            val result = sut.sync()

            assertTrue(result.success)
            assertEquals(1, result.deferredCount)
            assertEquals(1, result.pulledCount)
            val row =
                database.consultationQueries
                    .selectAll()
                    .executeAsList()
                    .single()
            assertEquals("remote", row.subjective)
            assertEquals(tie, row.updatedAt)
        }

    @Test
    fun `when pull receives soft deleted record then deactivates local row`() =
        runTest {
            val id = seedOwner("Alice", Instant.fromEpochMilliseconds(50))
            database.ownerQueries.setServerId("o-1", Instant.fromEpochMilliseconds(50), id)
            api.seed(remoteOwnerRecord("o-1", Instant.fromEpochMilliseconds(100), "Alice", isActive = false))

            val result = sut.sync()

            assertTrue(result.success)
            assertEquals(1, result.rejectedCount)
            val row =
                database.ownerQueries
                    .selectAllRows()
                    .executeAsList()
                    .single()
            assertFalse(row.isActive)
            assertEquals(Instant.fromEpochMilliseconds(100), row.updatedAt)
            assertFalse(
                api
                    .storedRecords()
                    .single()
                    .isActive,
            )
        }

    @Test
    fun `when api fails then reports failure and keeps last sync at`() =
        runTest {
            val engine =
                SyncEngineImpl(
                    ThrowingSyncApi(),
                    metadataRepository,
                    changeTracker,
                    registry,
                    database,
                )

            val result = engine.sync()

            assertFalse(result.success)
            assertEquals("network down", result.errorMessage)
            assertEquals(epoch, metadataRepository.getOrCreateLastSyncAt(""))
        }

    @Test
    fun `when syncing twice then second run pushes nothing new`() =
        runTest {
            seedOwner("Alice", Instant.fromEpochMilliseconds(100))

            val first = sut.sync()
            val second = sut.sync()

            assertTrue(first.success)
            assertEquals(1, first.pushedCount)
            assertTrue(second.success)
            assertEquals(0, second.pushedCount)
            assertEquals(0, second.pulledCount)
            assertEquals(1, api.storedRecords().size)
        }

    private fun ownerServerIdOf(id: Long): String? =
        database.ownerQueries
            .selectById(id)
            .executeAsOneOrNull()
            ?.serverId

    private fun patientServerIdOf(id: Long): String? =
        database.patientQueries
            .selectById(id)
            .executeAsOneOrNull()
            ?.serverId

    private fun consultationServerIdOf(id: Long): String? =
        database.consultationQueries
            .selectById(id)
            .executeAsOneOrNull()
            ?.serverId

    private class ThrowingSyncApi : SyncApi {
        override suspend fun pull(since: Instant): SyncPullResponse = throw IllegalStateException("network down")

        override suspend fun push(request: SyncPushRequest): SyncPushResponse = throw IllegalStateException("network down")
    }
}
