package com.github.rodrigotimoteo.animally.data.sync

import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.data.anamnese.AnamneseRepositoryImpl
import com.github.rodrigotimoteo.animally.data.owner.OwnerRepositoryImpl
import com.github.rodrigotimoteo.animally.data.patient.PatientRepositoryImpl
import com.github.rodrigotimoteo.animally.data.vaccination.VaccinationRepositoryImpl
import com.github.rodrigotimoteo.animally.di.database.createTestDatabase
import com.github.rodrigotimoteo.animally.domain.anamnese.model.Anamnese
import com.github.rodrigotimoteo.animally.domain.owner.model.Owner
import com.github.rodrigotimoteo.animally.domain.patient.model.Patient
import com.github.rodrigotimoteo.animally.domain.vaccination.model.Vaccination
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class SyncChangeTrackerImplTest {
    private lateinit var database: AnimallyDatabase
    private lateinit var sut: SyncChangeTrackerImpl
    private lateinit var ownerRepo: OwnerRepositoryImpl
    private lateinit var patientRepo: PatientRepositoryImpl
    private lateinit var vaccinationRepo: VaccinationRepositoryImpl
    private lateinit var anamneseRepo: AnamneseRepositoryImpl

    private val epoch = Instant.fromEpochMilliseconds(0)

    @BeforeTest
    fun setup() {
        database = createTestDatabase()
        sut = SyncChangeTrackerImpl(database)
        ownerRepo = OwnerRepositoryImpl(database.ownerQueries, database)
        patientRepo = PatientRepositoryImpl(database)
        vaccinationRepo = VaccinationRepositoryImpl(database)
        anamneseRepo = AnamneseRepositoryImpl(database)
    }

    private fun seedOwner(
        name: String,
        updatedAt: Instant,
        isActive: Boolean = true,
    ): Long =
        ownerRepo.insertOwner(
            Owner(
                id = 0L,
                name = name,
                email = null,
                phone = null,
                address = null,
                isActive = isActive,
                createdAt = epoch,
                updatedAt = updatedAt,
            ),
        )

    private fun seedPatient(
        name: String,
        updatedAt: Instant,
    ): Long =
        patientRepo.insertPatient(
            Patient(
                id = 0L,
                name = name,
                createdAt = epoch,
                updatedAt = updatedAt,
            ),
        )

    private fun seedVaccination(
        patientId: Long,
        updatedAt: Instant,
    ): Long =
        vaccinationRepo.insert(
            Vaccination(
                id = 0L,
                patientId = patientId,
                vaccineName = "Tetanus",
                dateAdministered = LocalDate(2024, 5, 1),
                createdAt = epoch,
                updatedAt = updatedAt,
            ),
        )

    private fun seedAnamnese(
        patientId: Long,
        updatedAt: Instant,
    ): Long =
        anamneseRepo.save(
            Anamnese(
                id = 0L,
                patientId = patientId,
                generalHistory = "healthy",
                chronicConditions = "none",
                allergies = "none",
                createdAt = epoch,
                updatedAt = updatedAt,
            ),
        )

    @Test
    fun `when database is empty then returns empty list`() =
        runTest {
            assertEquals(emptyList(), sut.recordsChangedSince(epoch))
        }

    @Test
    fun `when records changed since epoch then returns all seeded records`() =
        runTest {
            val ownerId = seedOwner("Alice", Instant.fromEpochMilliseconds(epochMilliseconds = 100))
            val patientId = seedPatient("Bella", Instant.fromEpochMilliseconds(epochMilliseconds = 200))
            val vaccinationId =
                seedVaccination(patientId, Instant.fromEpochMilliseconds(epochMilliseconds = 300))

            val result = sut.recordsChangedSince(epoch)

            assertEquals(3, result.size)
            assertEquals(
                setOf("Owner", "Patient", "Vaccination"),
                result.map { it.entityType }.toSet(),
            )
            result.forEach { assertTrue(it.isActive) }
            result.forEach { assertNull(it.serverId) }
            assertEquals(ownerId, result.first { it.entityType == "Owner" }.id)
            assertEquals(patientId, result.first { it.entityType == "Patient" }.id)
            assertEquals(vaccinationId, result.first { it.entityType == "Vaccination" }.id)
        }

    @Test
    fun `when records changed after instant then returns only newer`() =
        runTest {
            seedOwner("Alice", Instant.fromEpochMilliseconds(epochMilliseconds = 1000))
            seedPatient("Bella", Instant.fromEpochMilliseconds(epochMilliseconds = 500))

            val result =
                sut.recordsChangedSince(Instant.fromEpochMilliseconds(epochMilliseconds = 900))

            assertEquals(listOf("Owner"), result.map { it.entityType })
        }

    @Test
    fun `when soft deleted record changed then included with isActive false`() =
        runTest {
            seedOwner("Alice", Instant.fromEpochMilliseconds(epochMilliseconds = 100), isActive = false)

            val result = sut.recordsChangedSince(epoch)

            assertEquals(1, result.size)
            assertEquals("Owner", result.single().entityType)
            assertFalse(result.single().isActive)
        }

    @Test
    fun `when anamnese changed then included with isActive true`() =
        runTest {
            val patientId = seedPatient("Bella", Instant.fromEpochMilliseconds(epochMilliseconds = 100))
            seedAnamnese(patientId, Instant.fromEpochMilliseconds(epochMilliseconds = 200))

            val result = sut.recordsChangedSince(epoch)

            val anamnese = result.first { it.entityType == "Anamnese" }
            assertTrue(anamnese.isActive)
            assertNull(anamnese.serverId)
        }
}
