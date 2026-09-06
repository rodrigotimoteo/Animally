package com.github.rodrigotimoteo.animally.domain.sync

import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.data.follicle.FollicleRepositoryImpl
import com.github.rodrigotimoteo.animally.data.sync.handlers.FolliclePayload
import com.github.rodrigotimoteo.animally.data.sync.handlers.FollicleSyncHandler
import com.github.rodrigotimoteo.animally.di.database.createTestDatabase
import com.github.rodrigotimoteo.animally.domain.follicle.model.Follicle
import com.github.rodrigotimoteo.animally.domain.sync.handlers.SyncJson
import com.github.rodrigotimoteo.animally.domain.sync.handlers.decode
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.jsonObject
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.time.Instant

class FollicleSyncHandlerTest {
    private lateinit var database: AnimallyDatabase
    private lateinit var handler: FollicleSyncHandler
    private var ultrasoundId: Long = 0L
    private var ultrasoundServerId = "server-ultrasound-1"

    @BeforeTest
    fun setup() {
        database = createTestDatabase()
        handler = FollicleSyncHandler(FollicleRepositoryImpl(database), database)
        database.patientQueries.insertWithId(
            id = 1L,
            name = "Luna",
            species = "Equine",
            breed = null,
            dateOfBirth = null,
            gender = null,
            microchipId = null,
            ueln = null,
            registrationNumber = null,
            stableLocation = null,
            photoUri = null,
            notes = null,
            ownerId = null,
            isActive = true,
            createdAt = TS_BASE,
            updatedAt = TS_BASE,
            cogginsTestDate = null,
            cogginsResult = null,
            cogginsExpiryDate = null,
        )
        database.ultrasoundQueries.insertWithId(
            id = 10L,
            patientId = 1L,
            date = LocalDate(2026, 9, 1),
            ovaryStatus = null,
            uterineStatus = null,
            follicleSizeMm = null,
            leftOvaryStatus = "developing",
            rightOvaryStatus = "quiet",
            leftFollicleSizeMm = 32.0,
            rightFollicleSizeMm = null,
            uterineEdema = null,
            uterineLiquid = null,
            uterineLiquidDescription = null,
            uterusDescription = null,
            findings = "follow-up",
            imageUris = null,
            vetName = null,
            notes = null,
            isActive = true,
            createdAt = TS_BASE,
            updatedAt = TS_BASE,
        )
        ultrasoundId = 10L
        database.ultrasoundQueries.setServerId(ultrasoundServerId, TS_BASE, ultrasoundId).value
    }

    @Test
    fun `build includes the ultrasound relationship and follicle fields`() =
        runTest {
            val follicleId =
                FollicleRepositoryImpl(database).insert(
                    Follicle(
                        id = 0L,
                        ultrasoundId = ultrasoundId,
                        side = Follicle.SIDE_LEFT,
                        sizeMm = 36.5,
                        description = "dominant",
                        createdAt = TS_BASE,
                        updatedAt = TS_BASE,
                    ),
                )
            database.follicleQueries.setServerId("server-follicle-1", TS_BASE, follicleId).value

            val record = handler.buildRecord(follicleId)
            val payload = record.decode(FolliclePayload.serializer())

            assertEquals(SyncEntityType.FOLLICLE, handler.entityType)
            assertEquals(mapOf("ultrasoundId" to ultrasoundServerId), record.parentServerIds)
            assertEquals(Follicle.SIDE_LEFT, payload.side)
            assertEquals(36.5, payload.sizeMm)
            assertEquals("dominant", payload.description)
        }

    @Test
    fun `pull applies a follicle only after its ultrasound parent resolves`() =
        runTest {
            val payload =
                SyncJson
                    .encodeToJsonElement(
                        FolliclePayload.serializer(),
                        FolliclePayload(Follicle.SIDE_RIGHT, 24.25, "small", TS_REMOTE),
                    ).jsonObject
            val record =
                SyncRecord(
                    type = SyncEntityType.FOLLICLE.wireName,
                    serverId = "server-follicle-remote",
                    updatedAt = TS_REMOTE,
                    parentServerIds = mapOf("ultrasoundId" to ultrasoundServerId),
                    payload = payload,
                )

            val localId = handler.applyRecord(record)

            assertNotEquals(ENTITY_NOT_APPLIED, localId)
            val row = database.follicleQueries.selectRowById(localId).executeAsOne()
            assertEquals(ultrasoundId, row.ultrasoundId)
            assertEquals(Follicle.SIDE_RIGHT, row.side)
            assertEquals(24.25, row.sizeMm)
            assertEquals("small", row.description)
            assertEquals(TS_REMOTE, row.updatedAt)
            assertEquals("server-follicle-remote", handler.serverIdOf(localId))

            val unresolved =
                record.copy(
                    serverId = "server-follicle-unresolved",
                    parentServerIds = mapOf("ultrasoundId" to "missing"),
                )
            assertEquals(ENTITY_NOT_APPLIED, handler.applyRecord(unresolved))
        }

    @Test
    fun `soft-deleted follicle still serializes as a tombstone`() =
        runTest {
            val follicleId =
                FollicleRepositoryImpl(database).insert(
                    Follicle(
                        id = 0L,
                        ultrasoundId = ultrasoundId,
                        side = Follicle.SIDE_LEFT,
                        sizeMm = 30.0,
                        createdAt = TS_BASE,
                        updatedAt = TS_BASE,
                    ),
                )
            database.follicleQueries.setServerId("server-follicle-delete", TS_BASE, follicleId).value
            database.follicleQueries.setInactive(TS_REMOTE, follicleId).value

            val record = handler.buildRecord(follicleId)

            assertFalse(record.isActive)
            assertEquals("server-follicle-delete", record.serverId)
            assertEquals(TS_REMOTE, record.updatedAt)
        }

    private companion object {
        val TS_BASE = Instant.fromEpochMilliseconds(1_000L)
        val TS_REMOTE = Instant.fromEpochMilliseconds(2_000L)
    }
}
