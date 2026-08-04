package com.github.rodrigotimoteo.animally.domain.sync

import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.data.owner.OwnerRepositoryImpl
import com.github.rodrigotimoteo.animally.data.patient.PatientRepositoryImpl
import com.github.rodrigotimoteo.animally.di.database.createTestDatabase
import com.github.rodrigotimoteo.animally.domain.owner.model.Owner
import com.github.rodrigotimoteo.animally.domain.patient.model.Patient
import com.github.rodrigotimoteo.animally.domain.sync.handlers.PatientPayload
import com.github.rodrigotimoteo.animally.domain.sync.handlers.PatientSyncHandler
import com.github.rodrigotimoteo.animally.domain.sync.handlers.SyncJson
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Instant

class SyncPatientHandlerTest {
    private lateinit var database: AnimallyDatabase
    private lateinit var ownerRepo: OwnerRepositoryImpl
    private lateinit var patientRepo: PatientRepositoryImpl
    private lateinit var sut: PatientSyncHandler

    private val epoch = Instant.fromEpochMilliseconds(0)

    @BeforeTest
    fun setup() {
        database = createTestDatabase()
        ownerRepo = OwnerRepositoryImpl(database.ownerQueries)
        patientRepo = PatientRepositoryImpl(database)
        sut = PatientSyncHandler(patientRepo, database)
    }

    private fun seedOwner(name: String): Long =
        ownerRepo.insertOwner(
            Owner(id = 0L, name = name, email = null, phone = null, address = null, isActive = true, createdAt = epoch, updatedAt = epoch),
        )

    private fun seedPatient(
        name: String,
        ownerId: Long?,
        updatedAt: Instant = epoch,
    ): Long =
        patientRepo.insertPatient(
            Patient(id = 0L, name = name, ownerId = ownerId, createdAt = epoch, updatedAt = updatedAt),
        )

    private fun remoteRecord(
        serverId: String,
        updatedAt: Instant,
        name: String,
        ownerServerId: String?,
    ) = SyncRecord(
        type = SyncEntityType.PATIENT.wireName,
        serverId = serverId,
        updatedAt = updatedAt,
        isActive = true,
        parentServerIds = mapOf("ownerId" to ownerServerId),
        payload =
            SyncJson
                .encodeToJsonElement(
                    PatientPayload.serializer(),
                    PatientPayload(name = name, species = "Equine", breed = null, createdAt = epoch),
                ).jsonObject,
    )

    @Test
    fun `when building record then parent owner serverId is resolved`() =
        runTest {
            val ownerId = seedOwner("Alice")
            database.ownerQueries.setServerId("owner-svc-1", epoch, ownerId)
            val patientId = seedPatient("Bella", ownerId)

            val record = sut.buildRecord(patientId)

            assertEquals("owner-svc-1", record.parentServerIds["ownerId"])
            assertEquals("Bella", record.payload["name"]?.jsonPrimitive?.content)
            assertEquals(patientId, record.clientId)
        }

    @Test
    fun `when building record with unsynced owner then parent serverId is null`() =
        runTest {
            val ownerId = seedOwner("Alice")
            val patientId = seedPatient("Bella", ownerId)

            val record = sut.buildRecord(patientId)

            assertNull(record.parentServerIds["ownerId"])
        }

    @Test
    fun `when applying remote record then creates row with mapped ownerId and serverId`() =
        runTest {
            val ownerId = seedOwner("Alice")
            database.ownerQueries.setServerId("owner-svc-1", epoch, ownerId)

            val newId =
                sut.applyRecord(
                    remoteRecord(serverId = "patient-svc-1", updatedAt = Instant.fromEpochMilliseconds(200), name = "Coco", ownerServerId = "owner-svc-1"),
                )

            assertNotEquals(ENTITY_NOT_APPLIED, newId)
            val row = patientRepo.getPatientById(newId)
            assertNotNull(row)
            assertEquals("Coco", row.name)
            assertEquals(ownerId, row.ownerId)
            assertEquals(
                "patient-svc-1",
                database.patientQueries
                    .selectById(newId)
                    .executeAsOneOrNull()
                    ?.serverId,
            )
            assertEquals(newId, sut.localIdFor("patient-svc-1"))
        }

    @Test
    fun `when applying remote record with unresolvable owner then inserts with null owner`() =
        runTest {
            val newId =
                sut.applyRecord(
                    remoteRecord(serverId = "patient-svc-2", updatedAt = Instant.fromEpochMilliseconds(200), name = "Dolly", ownerServerId = "missing-owner"),
                )

            assertNotEquals(ENTITY_NOT_APPLIED, newId)
            assertNull(patientRepo.getPatientById(newId)?.ownerId)
        }

    @Test
    fun `when applying remote record with newer updatedAt then updates fields`() =
        runTest {
            val patientId = seedPatient("Bella", null, updatedAt = Instant.fromEpochMilliseconds(100))
            database.patientQueries.setServerId("patient-svc-3", Instant.fromEpochMilliseconds(100), patientId)

            val result =
                sut.applyRecord(
                    remoteRecord(serverId = "patient-svc-3", updatedAt = Instant.fromEpochMilliseconds(300), name = "Bella Renamed", ownerServerId = null),
                )

            assertEquals(patientId, result)
            assertEquals("Bella Renamed", patientRepo.getPatientById(patientId)?.name)
            assertEquals(Instant.fromEpochMilliseconds(300), patientRepo.getPatientById(patientId)?.updatedAt)
        }

    @Test
    fun `when applying remote record with older updatedAt then keeps local`() =
        runTest {
            val patientId = seedPatient("Bella", null, updatedAt = Instant.fromEpochMilliseconds(400))
            database.patientQueries.setServerId("patient-svc-4", Instant.fromEpochMilliseconds(400), patientId)

            val result =
                sut.applyRecord(
                    remoteRecord(serverId = "patient-svc-4", updatedAt = Instant.fromEpochMilliseconds(300), name = "Stale Remote", ownerServerId = null),
                )

            assertEquals(patientId, result)
            assertEquals("Bella", patientRepo.getPatientById(patientId)?.name)
        }
}
