package com.github.rodrigotimoteo.animally.domain.sync

import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.data.patient.PatientRepositoryImpl
import com.github.rodrigotimoteo.animally.data.vaccination.VaccinationRepositoryImpl
import com.github.rodrigotimoteo.animally.di.database.createTestDatabase
import com.github.rodrigotimoteo.animally.domain.patient.model.Patient
import com.github.rodrigotimoteo.animally.domain.sync.handlers.SyncJson
import com.github.rodrigotimoteo.animally.domain.sync.handlers.VaccinationPayload
import com.github.rodrigotimoteo.animally.domain.sync.handlers.VaccinationSyncHandler
import com.github.rodrigotimoteo.animally.domain.vaccination.model.Vaccination
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Instant

class SyncVaccinationHandlerTest {
    private lateinit var database: AnimallyDatabase
    private lateinit var patientRepo: PatientRepositoryImpl
    private lateinit var vaccinationRepo: VaccinationRepositoryImpl
    private lateinit var sut: VaccinationSyncHandler

    private val epoch = Instant.fromEpochMilliseconds(0)

    @BeforeTest
    fun setup() {
        database = createTestDatabase()
        patientRepo = PatientRepositoryImpl(database)
        vaccinationRepo = VaccinationRepositoryImpl(database)
        sut = VaccinationSyncHandler(vaccinationRepo, patientRepo, database)
    }

    private fun seedPatient(name: String): Long = patientRepo.insertPatient(Patient(id = 0L, name = name, createdAt = epoch, updatedAt = epoch))

    private fun seedVaccination(
        patientId: Long,
        updatedAt: Instant,
        vaccineName: String = "Tetanus",
    ): Long =
        vaccinationRepo.insert(
            Vaccination(
                id = 0L,
                patientId = patientId,
                vaccineName = vaccineName,
                dateAdministered = LocalDate(2024, 5, 1),
                createdAt = epoch,
                updatedAt = updatedAt,
            ),
        )

    private fun remoteRecord(
        serverId: String,
        updatedAt: Instant,
        patientServerId: String,
        vaccineName: String,
    ) = SyncRecord(
        type = SyncEntityType.VACCINATION.wireName,
        serverId = serverId,
        updatedAt = updatedAt,
        isActive = true,
        parentServerIds = mapOf("patientId" to patientServerId),
        payload =
            SyncJson
                .encodeToJsonElement(
                    VaccinationPayload.serializer(),
                    VaccinationPayload(
                        vaccineName = vaccineName,
                        dateAdministered = LocalDate(2024, 6, 1),
                        vetName = "Dr. Vet",
                        createdAt = epoch,
                    ),
                ).jsonObject,
    )

    @Test
    fun `when building record then parent patient serverId is resolved`() =
        runTest {
            val patientId = seedPatient("Bella")
            database.patientQueries.setServerId("patient-svc-1", epoch, patientId)
            val vaccinationId = seedVaccination(patientId, Instant.fromEpochMilliseconds(100))

            val record = sut.buildRecord(vaccinationId)

            assertEquals("patient-svc-1", record.parentServerIds["patientId"])
            assertEquals("Tetanus", record.payload["vaccineName"]?.jsonPrimitive?.content)
            assertEquals(vaccinationId, record.clientId)
            assertEquals(Instant.fromEpochMilliseconds(100), record.updatedAt)
        }

    @Test
    fun `when applying remote record then creates row with mapped patientId and serverId`() =
        runTest {
            val patientId = seedPatient("Bella")
            database.patientQueries.setServerId("patient-svc-1", epoch, patientId)

            val newId =
                sut.applyRecord(
                    remoteRecord(
                        serverId = "vaccination-svc-1",
                        updatedAt = Instant.fromEpochMilliseconds(200),
                        patientServerId = "patient-svc-1",
                        vaccineName = "Flu",
                    ),
                )

            assertNotEquals(ENTITY_NOT_APPLIED, newId)
            val row = vaccinationRepo.getById(newId)
            assertNotNull(row)
            assertEquals("Flu", row.vaccineName)
            assertEquals(patientId, row.patientId)
            assertEquals(
                "vaccination-svc-1",
                database.vaccinationQueries
                    .selectById(newId)
                    .executeAsOneOrNull()
                    ?.serverId,
            )
            assertEquals(newId, sut.localIdFor("vaccination-svc-1"))
        }

    @Test
    fun `when applying remote record with unresolved patient then not applied`() =
        runTest {
            val result =
                sut.applyRecord(
                    remoteRecord(
                        serverId = "vaccination-svc-2",
                        updatedAt = Instant.fromEpochMilliseconds(200),
                        patientServerId = "missing-patient",
                        vaccineName = "Flu",
                    ),
                )

            assertEquals(ENTITY_NOT_APPLIED, result)
            assertNull(sut.localIdFor("vaccination-svc-2"))
        }

    @Test
    fun `when applying remote record with newer updatedAt then updates fields`() =
        runTest {
            val patientId = seedPatient("Bella")
            database.patientQueries.setServerId("patient-svc-1", epoch, patientId)
            val vaccinationId = seedVaccination(patientId, Instant.fromEpochMilliseconds(100))
            database.vaccinationQueries.setServerId("vaccination-svc-3", Instant.fromEpochMilliseconds(100), vaccinationId)

            val result =
                sut.applyRecord(
                    remoteRecord(
                        serverId = "vaccination-svc-3",
                        updatedAt = Instant.fromEpochMilliseconds(300),
                        patientServerId = "patient-svc-1",
                        vaccineName = "Tetanus Booster",
                    ),
                )

            assertEquals(vaccinationId, result)
            assertEquals("Tetanus Booster", vaccinationRepo.getById(vaccinationId)?.vaccineName)
            assertEquals(Instant.fromEpochMilliseconds(300), vaccinationRepo.getById(vaccinationId)?.updatedAt)
        }

    @Test
    fun `when applying remote record with older updatedAt then keeps local`() =
        runTest {
            val patientId = seedPatient("Bella")
            database.patientQueries.setServerId("patient-svc-1", epoch, patientId)
            val vaccinationId = seedVaccination(patientId, Instant.fromEpochMilliseconds(400))
            database.vaccinationQueries.setServerId("vaccination-svc-4", Instant.fromEpochMilliseconds(400), vaccinationId)

            val result =
                sut.applyRecord(
                    remoteRecord(
                        serverId = "vaccination-svc-4",
                        updatedAt = Instant.fromEpochMilliseconds(300),
                        patientServerId = "patient-svc-1",
                        vaccineName = "Stale Remote",
                    ),
                )

            assertEquals(vaccinationId, result)
            assertEquals("Tetanus", vaccinationRepo.getById(vaccinationId)?.vaccineName)
        }
}
