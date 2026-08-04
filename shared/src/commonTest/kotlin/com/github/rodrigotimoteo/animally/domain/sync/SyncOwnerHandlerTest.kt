package com.github.rodrigotimoteo.animally.domain.sync

import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.data.owner.OwnerRepositoryImpl
import com.github.rodrigotimoteo.animally.di.database.createTestDatabase
import com.github.rodrigotimoteo.animally.domain.owner.model.Owner
import com.github.rodrigotimoteo.animally.domain.sync.handlers.OwnerPayload
import com.github.rodrigotimoteo.animally.domain.sync.handlers.OwnerSyncHandler
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

class SyncOwnerHandlerTest {
    private lateinit var database: AnimallyDatabase
    private lateinit var ownerRepo: OwnerRepositoryImpl
    private lateinit var sut: OwnerSyncHandler

    private val epoch = Instant.fromEpochMilliseconds(0)

    @BeforeTest
    fun setup() {
        database = createTestDatabase()
        ownerRepo = OwnerRepositoryImpl(database.ownerQueries)
        sut = OwnerSyncHandler(ownerRepo, database)
    }

    private fun seedOwner(
        name: String,
        updatedAt: Instant,
        email: String? = null,
    ): Long =
        ownerRepo.insertOwner(
            Owner(
                id = 0L,
                name = name,
                email = email,
                phone = null,
                address = "Somewhere",
                isActive = true,
                createdAt = epoch,
                updatedAt = updatedAt,
            ),
        )

    private fun remoteRecord(
        serverId: String,
        updatedAt: Instant,
        name: String,
        email: String? = null,
    ) = SyncRecord(
        type = SyncEntityType.OWNER.wireName,
        serverId = serverId,
        updatedAt = updatedAt,
        isActive = true,
        payload =
            SyncJson
                .encodeToJsonElement(
                    OwnerPayload.serializer(),
                    OwnerPayload(name = name, email = email, address = "Somewhere", createdAt = epoch),
                ).jsonObject,
    )

    @Test
    fun `when building record then serializes owner fields`() =
        runTest {
            val id = seedOwner("Alice", Instant.fromEpochMilliseconds(100), email = "alice@x.com")

            val record = sut.buildRecord(id)

            assertEquals(SyncEntityType.OWNER.wireName, record.type)
            assertEquals(id, record.clientId)
            assertNull(record.serverId)
            assertEquals(Instant.fromEpochMilliseconds(100), record.updatedAt)
            assertNotNull(record.payload["name"])
            assertEquals("Alice", record.payload["name"]?.jsonPrimitive?.content)
            assertEquals("alice@x.com", record.payload["email"]?.jsonPrimitive?.content)
        }

    @Test
    fun `when owner already has serverId then record carries it`() =
        runTest {
            val id = seedOwner("Alice", Instant.fromEpochMilliseconds(100))
            database.ownerQueries.setServerId("owner-svc-1", Instant.fromEpochMilliseconds(100), id)

            val record = sut.buildRecord(id)

            assertEquals("owner-svc-1", record.serverId)
            assertEquals("owner-svc-1", sut.serverIdOf(id))
        }

    @Test
    fun `when applying remote record then creates row and stamps serverId`() =
        runTest {
            val newId = sut.applyRecord(remoteRecord(serverId = "owner-svc-2", updatedAt = Instant.fromEpochMilliseconds(200), name = "Bob"))

            assertNotEquals(ENTITY_NOT_APPLIED, newId)
            val row = ownerRepo.getOwnerById(newId)
            assertNotNull(row)
            assertEquals("Bob", row.name)
            assertEquals(
                "owner-svc-2",
                database.ownerQueries
                    .selectById(newId)
                    .executeAsOneOrNull()
                    ?.serverId,
            )
            assertEquals(Instant.fromEpochMilliseconds(200), row.updatedAt)
            assertEquals(newId, sut.localIdFor("owner-svc-2"))
        }

    @Test
    fun `when applying remote record with newer updatedAt then updates fields`() =
        runTest {
            val id = seedOwner("Alice", Instant.fromEpochMilliseconds(100))
            database.ownerQueries.setServerId("owner-svc-3", Instant.fromEpochMilliseconds(100), id)

            val result =
                sut.applyRecord(
                    remoteRecord(serverId = "owner-svc-3", updatedAt = Instant.fromEpochMilliseconds(300), name = "Alice Updated"),
                )

            assertEquals(id, result)
            assertEquals("Alice Updated", ownerRepo.getOwnerById(id)?.name)
            assertEquals(Instant.fromEpochMilliseconds(300), ownerRepo.getOwnerById(id)?.updatedAt)
        }

    @Test
    fun `when applying remote record with older updatedAt then keeps local`() =
        runTest {
            val id = seedOwner("Alice", Instant.fromEpochMilliseconds(400))
            database.ownerQueries.setServerId("owner-svc-4", Instant.fromEpochMilliseconds(400), id)

            val result =
                sut.applyRecord(
                    remoteRecord(serverId = "owner-svc-4", updatedAt = Instant.fromEpochMilliseconds(300), name = "Stale Remote"),
                )

            assertEquals(id, result)
            assertEquals("Alice", ownerRepo.getOwnerById(id)?.name)
            assertEquals(Instant.fromEpochMilliseconds(400), ownerRepo.getOwnerById(id)?.updatedAt)
        }

    @Test
    fun `when applying remote record with equal updatedAt then remote wins`() =
        runTest {
            val id = seedOwner("Alice", Instant.fromEpochMilliseconds(500))
            database.ownerQueries.setServerId("owner-svc-5", Instant.fromEpochMilliseconds(500), id)

            val result =
                sut.applyRecord(
                    remoteRecord(serverId = "owner-svc-5", updatedAt = Instant.fromEpochMilliseconds(500), name = "Tie Remote"),
                )

            assertEquals(id, result)
            assertEquals("Tie Remote", ownerRepo.getOwnerById(id)?.name)
        }

    @Test
    fun `when remote record has no serverId then not applied`() =
        runTest {
            val record =
                SyncRecord(
                    type = SyncEntityType.OWNER.wireName,
                    updatedAt = Instant.fromEpochMilliseconds(100),
                    payload = SyncJson.encodeToJsonElement(OwnerPayload.serializer(), OwnerPayload(name = "Ghost")).jsonObject,
                )

            assertEquals(ENTITY_NOT_APPLIED, sut.applyRecord(record))
        }
}
