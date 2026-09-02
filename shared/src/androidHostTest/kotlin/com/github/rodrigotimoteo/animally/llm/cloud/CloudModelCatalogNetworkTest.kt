package com.github.rodrigotimoteo.animally.llm.cloud

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CloudModelCatalogNetworkTest {
    @Test
    fun `model discovery returns a bounded failure when the endpoint never responds`() =
        runTest {
            val client =
                HttpClient(
                    MockEngine {
                        awaitCancellation()
                    },
                )
            try {
                assertEquals(
                    CloudModelsResult.Failure("Request timed out"),
                    CloudModelCatalog(client).fetch("https://example.test/v1", "test-key"),
                )
            } finally {
                client.close()
            }
        }

    @Test
    fun `insecure non-loopback endpoint is rejected before request`() =
        runTest {
            var requestCount = 0
            val client =
                HttpClient(
                    MockEngine {
                        requestCount += 1
                        error("Request should not be sent")
                    },
                )
            try {
                assertEquals(
                    CloudModelsResult.Failure(
                        "Endpoint must use HTTPS; HTTP is limited to explicitly configured local runtimes",
                    ),
                    CloudModelCatalog(client).fetch("http://example.test/v1", "test-key"),
                )
                assertEquals(0, requestCount)
            } finally {
                client.close()
            }
        }

    @Test
    fun `local model discovery omits a stale hosted authorization key`() =
        runBlocking {
            var authorization: String? = null
            val client =
                HttpClient(
                    MockEngine { request ->
                        authorization = request.headers[HttpHeaders.Authorization]
                        respond(
                            content = "{\"data\":[]}",
                            headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                        )
                    },
                )
            try {
                assertEquals(
                    CloudModelsResult.Success(emptyList()),
                    CloudModelCatalog(client).fetch(
                        baseUrl = "http://127.0.0.1:11434/v1",
                        apiKey = "stale-hosted-key",
                        allowInsecureLocalEndpoint = true,
                    ),
                )
                assertNull(authorization)
            } finally {
                client.close()
            }
        }
}
