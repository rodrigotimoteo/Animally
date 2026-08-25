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
    fun `cloud request omits local-only max_tokens`() {
        val request = buildChatCompletionRequest(config, "q", "i")
        val wire =
            Json {
                explicitNulls = false
            }.encodeToString(ChatCompletionRequest.serializer(), request)
        assertTrue(!wire.contains("max_tokens"))
        assertNull(request.maxTokens)
    }

    @Test
    fun `local request carries the configured max_tokens budget`() {
        val localConfig = config.copy(maxTokens = CloudLlmConfig.DEFAULT_MAX_TOKENS)
        val request = buildChatCompletionRequest(localConfig, "q", "i")
        val wire = Json.encodeToString(ChatCompletionRequest.serializer(), request)
        assertTrue(wire.contains("\"max_tokens\":${CloudLlmConfig.DEFAULT_MAX_TOKENS}"))
        assertEquals(CloudLlmConfig.DEFAULT_MAX_TOKENS, request.maxTokens)
    }

    @Test
    fun `reasoning deltas and malformed frames never kill the stream`() {
        val engine = engine()
        val cumulative = StringBuilder()

        // Reasoning models emit hidden chain-of-thought deltas with no content.
        assertNull(engine.appendSseDelta("data: {\"choices\":[{\"delta\":{\"reasoning_content\":\"thinking\"}}]}", cumulative))
        assertEquals("", cumulative.toString())

        // Usage-only tail chunk (no choices content) is equally inert.
        assertNull(engine.appendSseDelta("data: {\"choices\":[],\"usage\":{\"total_tokens\":42}}", cumulative))

        // A malformed payload is skipped, not fatal.
        assertNull(engine.appendSseDelta("data: not-json", cumulative))
        assertEquals("", cumulative.toString())
    }

    @Test
    fun `snake_case wire fields decode into typed chunk fields`() {
        val chunk =
            Json
                .decodeFromString(
                    ChatCompletionChunk.serializer(),
                    """{"choices":[{"delta":{"reasoning_content":"hmm"},"finish_reason":null}]}""",
                )
        val reasoningDelta = chunk.choices.first().delta
        assertEquals("hmm", reasoningDelta?.reasoningContent)
        assertNull(chunk.choices.first().finishReason)

        val terminal =
            Json
                .decodeFromString(
                    ChatCompletionChunk.serializer(),
                    """{"choices":[{"delta":{},"finish_reason":"length"}]}""",
                )
        assertEquals("length", terminal.choices.first().finishReason)
    }

    @Test
    fun `stream end validation distinguishes clean done from truncation`() {
        assertNull(validateStreamEnd(sawDone = true, finishReason = null, contentLength = 10))
        assertNull(validateStreamEnd(sawDone = false, finishReason = "stop", contentLength = 10))
        assertNull(validateStreamEnd(sawDone = true, finishReason = "length", contentLength = 42))

        // Connection dropped mid-answer: must surface as an error, not silence.
        assertEquals(
            "Cloud LLM stream ended before completion",
            validateStreamEnd(sawDone = false, finishReason = null, contentLength = 7),
        )
        // Budget consumed by reasoning with zero visible content: explicit message.
        assertEquals(
            "Cloud model spent its entire token budget on reasoning and returned no answer",
            validateStreamEnd(sawDone = true, finishReason = "length", contentLength = 0),
        )
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
