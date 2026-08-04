package com.github.rodrigotimoteo.animally.sync

import com.github.rodrigotimoteo.animally.domain.sync.SyncEntityType
import com.github.rodrigotimoteo.animally.domain.sync.SyncPushRequest
import com.github.rodrigotimoteo.animally.domain.sync.SyncRecord
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class InMemorySyncApiTest {
    private val serverNow = Instant.fromEpochMilliseconds(5_000_000L)
    private val api = InMemorySyncApi(serverClock = { serverNow })

    private fun instant(ms: Long): Instant = Instant.fromEpochMilliseconds(ms)

    private fun record(
        clientId: Long,
        updatedAt: Instant,
        serverId: String? = null,
    ): SyncRecord =
        SyncRecord(
            type = SyncEntityType.PATIENT.wireName,
            serverId = serverId,
            clientId = clientId,
            updatedAt = updatedAt,
        )

    @Test
    fun `whenPushingNewRecordWithoutServerId thenAssignsServerIdAndAccepts`() =
        runTest {
            val response = api.push(SyncPushRequest(deviceId = "device-1", records = listOf(record(clientId = 42L, updatedAt = instant(1L)))))

            assertEquals(1, response.accepted.size)
            assertTrue(response.rejected.isEmpty())
            assertEquals(42L, response.accepted.single().clientId)
            assertNotNull(response.accepted.single().serverId)
            assertEquals("srv-Patient-42", response.accepted.single().serverId)
            assertEquals(serverNow, response.serverTimestamp)
        }

    @Test
    fun `whenPushingNewRecordWithoutClientId thenAssignsIncrementingServerId`() =
        runTest {
            val response =
                api.push(
                    SyncPushRequest(
                        deviceId = "device-1",
                        records =
                            listOf(
                                record(clientId = 7L, updatedAt = instant(1L)),
                                SyncRecord(type = SyncEntityType.OWNER.wireName, updatedAt = instant(2L)),
                            ),
                    ),
                )

            assertEquals(listOf("srv-Patient-7", "srv-Owner-1"), response.accepted.map { it.serverId })
        }

    @Test
    fun `whenPushingRecordWithKnownServerIdAndNewerUpdatedAt thenOverwritesStoredRecord`() =
        runTest {
            api.seed(record(clientId = 1L, updatedAt = instant(100L), serverId = "srv-1"))
            val newer = record(clientId = 1L, updatedAt = instant(200L), serverId = "srv-1")

            val response = api.push(SyncPushRequest(deviceId = "device-1", records = listOf(newer)))

            assertTrue(response.rejected.isEmpty())
            assertEquals("srv-1", response.accepted.single().serverId)
            assertEquals(instant(200L), api.storedRecords().single().updatedAt)
        }

    @Test
    fun `whenPushingStaleRecord thenRejectsWithStaleReason`() =
        runTest {
            api.seed(record(clientId = 1L, updatedAt = instant(200L), serverId = "srv-1"))
            val stale = record(clientId = 1L, updatedAt = instant(100L), serverId = "srv-1")

            val response = api.push(SyncPushRequest(deviceId = "device-1", records = listOf(stale)))

            assertTrue(response.accepted.isEmpty())
            assertEquals(1, response.rejected.size)
            assertEquals("stale", response.rejected.single().reason)
            assertEquals(1L, response.rejected.single().clientId)
            assertEquals(instant(200L), api.storedRecords().single().updatedAt)
        }

    @Test
    fun `whenPushingRecordWithEqualUpdatedAt thenServerWinsAndReplaces`() =
        runTest {
            val stored = record(clientId = 1L, updatedAt = instant(200L), serverId = "srv-1")
            api.seed(stored)
            val tie = record(clientId = 1L, updatedAt = instant(200L), serverId = "srv-1")

            val response = api.push(SyncPushRequest(deviceId = "device-1", records = listOf(tie)))

            assertTrue(response.rejected.isEmpty())
            assertEquals(1, response.accepted.size)
            assertEquals("srv-1", api.storedRecords().single().serverId)
        }

    @Test
    fun `whenPulling thenReturnsOnlyRecordsNewerThanSince`() =
        runTest {
            api.seed(record(clientId = 1L, updatedAt = instant(100L), serverId = "srv-1"))
            api.seed(record(clientId = 2L, updatedAt = instant(200L), serverId = "srv-2"))
            api.seed(record(clientId = 3L, updatedAt = instant(300L), serverId = "srv-3"))

            val response = api.pull(since = instant(150L))

            assertEquals(listOf("srv-2", "srv-3"), response.records.map { it.serverId })
            assertEquals(serverNow, response.serverTimestamp)
        }

    @Test
    fun `whenPulling thenReturnsRecordsInStoredOrder`() =
        runTest {
            api.seed(record(clientId = 1L, updatedAt = instant(100L), serverId = "srv-1"))
            api.seed(record(clientId = 2L, updatedAt = instant(200L), serverId = "srv-2"))
            api.seed(record(clientId = 3L, updatedAt = instant(300L), serverId = "srv-3"))

            val response = api.pull(since = instant(0L))

            assertEquals(listOf("srv-1", "srv-2", "srv-3"), response.records.map { it.serverId })
        }
}
