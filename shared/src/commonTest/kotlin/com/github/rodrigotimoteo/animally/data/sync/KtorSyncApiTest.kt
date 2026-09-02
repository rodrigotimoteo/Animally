package com.github.rodrigotimoteo.animally.data.sync

import com.github.rodrigotimoteo.animally.domain.sync.SyncPushRequest
import io.ktor.client.HttpClient
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Instant

class KtorSyncApiTest {
    private val client = HttpClient()
    private val api = KtorSyncApi(client)

    @AfterTest
    fun closeClient() {
        client.close()
    }

    @Test
    fun `pull fails closed before using the HTTP client`() =
        runTest {
            val error =
                assertFailsWith<IllegalStateException> {
                    api.pull(Instant.fromEpochMilliseconds(0L))
                }

            assertEquals(SYNC_DISABLED_MESSAGE, error.message)
        }

    @Test
    fun `push fails closed before using the HTTP client`() =
        runTest {
            val error =
                assertFailsWith<IllegalStateException> {
                    api.push(SyncPushRequest(deviceId = "device-1", records = emptyList()))
                }

            assertEquals(SYNC_DISABLED_MESSAGE, error.message)
        }
}
