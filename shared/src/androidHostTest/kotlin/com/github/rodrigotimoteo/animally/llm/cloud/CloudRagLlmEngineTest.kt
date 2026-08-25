package com.github.rodrigotimoteo.animally.llm.cloud

import io.ktor.client.HttpClient
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Contract tests for [CloudRagLlmEngine]'s wire shape and SSE parsing.
 *
 * The ktor client pipeline itself is exercised by the assistant UI suites on a real
 * device/simulator; here the request DTO builder and the SSE line parser are driven
 * directly (ktor-client-mock is a separate artifact not available to this source set).
 */
class CloudRagLlmEngineTest {
    private val config =
        CloudLlmConfig(
            baseUrl = "https://example.test/v1/chat/completions",
            model = "test-model",
            apiKey = "sk-test",
        )

    private fun engine() = CloudRagLlmEngine(HttpClient()) { config }

    @Test
    fun `request dto carries model stream flag and both roles`() {
        val request = buildChatCompletionRequest(config, "What treats?", "Be terse.")
        assertEquals("test-model", request.model)
        assertTrue(request.stream)
        assertEquals(2, request.messages.size)
        assertEquals("system", request.messages.first().role)
        assertEquals("Be terse.", request.messages.first().content)
        assertEquals("user", request.messages.last().role)
        assertEquals("What treats?", request.messages.last().content)

        val wire = Json.encodeToString(ChatCompletionRequest.serializer(), request)
        assertTrue(wire.contains("\"stream\":true"))
        assertTrue(wire.contains("\"model\":\"test-model\""))
    }

    @Test
    fun `sse deltas accumulate into cumulative snapshots`() {
        val engine = engine()
        val cumulative = StringBuilder()

        assertEquals("Hello", engine.appendSseDelta("data: {\"choices\":[{\"delta\":{\"content\":\"Hello\"}}]}", cumulative))
        assertEquals("Hello", cumulative.toString())

        assertEquals(" world", engine.appendSseDelta("data: {\"choices\":[{\"delta\":{\"content\":\" world\"}}]}", cumulative))
        assertEquals("Hello world", cumulative.toString())

        assertNull(engine.appendSseDelta("data: [DONE]", cumulative))
        assertNull(engine.appendSseDelta(": keep-alive comment", cumulative))
        assertNull(engine.appendSseDelta("", cumulative))
        assertEquals("Hello world", cumulative.toString())
    }
}
