package com.github.rodrigotimoteo.animally.llm.cloud

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

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
}
