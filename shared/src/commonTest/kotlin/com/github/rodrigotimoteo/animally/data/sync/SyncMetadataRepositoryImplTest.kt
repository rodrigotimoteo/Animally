package com.github.rodrigotimoteo.animally.data.sync

import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.di.database.createTestDatabase
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class SyncMetadataRepositoryImplTest {
    private lateinit var database: AnimallyDatabase
    private lateinit var sut: SyncMetadataRepositoryImpl

    @BeforeTest
    fun setup() {
        database = createTestDatabase()
        sut = SyncMetadataRepositoryImpl(database)
    }

    @Test
    fun `when no row exists then returns epoch zero and inserts row`() =
        runTest {
            val result = sut.getOrCreateLastSyncAt("dev-1")

            assertEquals(Instant.fromEpochMilliseconds(0), result)
            assertEquals("dev-1", sut.getDeviceId())
        }

    @Test
    fun `when no row exists then getDeviceId returns empty string`() =
        runTest {
            assertEquals("", sut.getDeviceId())
        }

    @Test
    fun `when updating last sync at then persists value`() =
        runTest {
            sut.getOrCreateLastSyncAt("dev-1")
            val later = Instant.fromEpochMilliseconds(epochMilliseconds = 1000)

            sut.updateLastSyncAt(later)

            assertEquals(later, sut.getOrCreateLastSyncAt("dev-1"))
        }

    @Test
    fun `when updating last sync at without existing row then creates row`() =
        runTest {
            val later = Instant.fromEpochMilliseconds(epochMilliseconds = 500)

            sut.updateLastSyncAt(later)

            assertEquals(later, sut.getOrCreateLastSyncAt("dev-1"))
        }

    @Test
    fun `when saving device id then persists and preserves last sync at`() =
        runTest {
            sut.getOrCreateLastSyncAt("dev-1")
            val later = Instant.fromEpochMilliseconds(epochMilliseconds = 1000)
            sut.updateLastSyncAt(later)

            sut.saveDeviceId("dev-2")

            assertEquals("dev-2", sut.getDeviceId())
            assertEquals(later, sut.getOrCreateLastSyncAt("dev-1"))
        }

    @Test
    fun `when calling getOrCreate twice then returns stored value`() =
        runTest {
            assertEquals(Instant.fromEpochMilliseconds(0), sut.getOrCreateLastSyncAt("dev-1"))
            val later = Instant.fromEpochMilliseconds(epochMilliseconds = 1000)
            sut.updateLastSyncAt(later)

            assertEquals(later, sut.getOrCreateLastSyncAt("dev-1"))
        }
}
