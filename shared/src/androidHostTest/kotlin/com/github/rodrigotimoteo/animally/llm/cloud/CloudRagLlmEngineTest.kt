package com.github.rodrigotimoteo.animally.llm.cloud

import com.github.rodrigotimoteo.animally.llm.RagChatMessage
import com.github.rodrigotimoteo.animally.llm.RagChatRole
import com.github.rodrigotimoteo.animally.llm.RagToolCall
import com.github.rodrigotimoteo.animally.llm.RagToolDefinition
import com.github.rodrigotimoteo.animally.llm.RagToolStreamEvent
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Contract tests for [CloudRagLlmEngine]'s wire shape and SSE parsing.
 *
 * The test doubles exercise the full Ktor request/response pipeline without a network
 * connection, while the lower-level cases keep malformed and provider-specific frames
 * easy to diagnose.
 */
class CloudRagLlmEngineTest {
    private val wireJson = Json { explicitNulls = false }
    private val config =
        CloudLlmConfig(
            baseUrl = "https://example.test/v1/chat/completions",
            model = "test-model",
            apiKey = "test-key",
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
        val wire = wireJson.encodeToString(ChatCompletionRequest.serializer(), request)
        assertTrue(!wire.contains("max_tokens"))
        assertNull(request.maxTokens)
    }

    @Test
    fun `tool request carries schemas and replayable assistant and tool messages`() {
        val tool =
            RagToolDefinition(
                name = "weight_summary",
                description = "Summarize weights.",
                parameters = Json.parseToJsonElement("""{"type":"object","properties":{}}""").jsonObject,
            )
        val request =
            buildToolChatCompletionRequest(
                config = config,
                messages =
                    listOf(
                        RagChatMessage(RagChatRole.SYSTEM, "instructions"),
                        RagChatMessage(RagChatRole.USER, "Analyze weights"),
                        RagChatMessage(
                            role = RagChatRole.ASSISTANT,
                            toolCalls = listOf(RagToolCall("call-1", "weight_summary", "{}")),
                        ),
                        RagChatMessage(
                            role = RagChatRole.TOOL,
                            content = """{"average_kg":505.0}""",
                            toolCallId = "call-1",
                            name = "weight_summary",
                        ),
                    ),
                tools = listOf(tool),
            )
        val wire = wireJson.encodeToString(ChatCompletionRequest.serializer(), request)

        assertEquals("auto", request.toolChoice)
        assertEquals(1, request.tools?.size)
        assertTrue(wire.contains("\"tools\""))
        assertTrue(wire.contains("\"tool_calls\""))
        assertTrue(wire.contains("\"tool_call_id\":\"call-1\""))
        assertTrue(wire.contains("\"role\":\"tool\""))
        assertTrue(!wire.contains("\"content\":null"))
    }

    @Test
    fun `tool call fragments decode with indexes and argument fragments`() {
        val chunk =
            Json.decodeFromString(
                ChatCompletionChunk.serializer(),
                """
                {"choices":[{"delta":{"tool_calls":[
                  {"index":0,"id":"call-1","type":"function","function":{"name":"weight_summary","arguments":"{\"patient_name\":"}},
                  {"index":1,"id":"call-2","type":"function","function":{"name":"care_summary","arguments":"{}"}}
                ]}}]}
                """.trimIndent(),
            )

        val delta = requireNotNull(chunk.choices.single().delta)
        val calls = requireNotNull(delta.toolCalls)
        assertEquals(2, calls.size)
        assertEquals(0, calls[0].index)
        assertEquals("weight_summary", calls[0].function?.name)
        assertEquals("{\"patient_name\":", calls[0].function?.arguments)
        assertEquals(1, calls[1].index)
        assertEquals("care_summary", calls[1].function?.name)
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
    fun `repeated malformed data frames fail instead of returning a truncated answer`() =
        runTest {
            val client =
                mockClient(
                    """
                    data: {not-json}
                    data: {still-not-json}
                    """.trimIndent(),
                )
            try {
                val streamingEngine = CloudRagLlmEngine(client) { config }

                val failure =
                    assertFailsWith<IllegalStateException> {
                        streamingEngine.generateStreaming("question", "instructions").toList()
                    }

                assertTrue(failure.message.orEmpty().contains("malformed data"))
            } finally {
                client.close()
            }
        }

    @Test
    fun `visible content combines compatible fields without duplicating cumulative text`() {
        val engine = engine()
        val cumulative = StringBuilder()

        assertEquals(
            "Hello world",
            engine.appendSseDelta(
                """data: {"choices":[{"delta":{"content":"Hello"},"message":{"content":" world"}}]}""",
                cumulative,
            ),
        )
        assertEquals("Hello world", cumulative.toString())

        assertEquals(
            "Complete answer",
            engine.appendSseDelta(
                """data: {"choices":[{"delta":{"content":"Complete"},"message":{"content":"Complete answer"}}]}""",
                cumulative = StringBuilder(),
            ),
        )
    }

    @Test
    fun `recognizes OpenCode cost trailer as a terminal frame`() {
        val engine = engine()

        assertTrue(engine.isTerminalSseFrame("data: {\"choices\":[],\"cost\":\"0\"}"))
        assertTrue(engine.isTerminalSseFrame("data: [DONE]"))
        assertTrue(engine.isTerminalSseFrame("event: done"))
        assertTrue(engine.isTerminalSseFrame("data: {\"type\":\"message_stop\"}"))
        assertTrue(engine.isTerminalSseFrame("data: {\"done\":true}"))
        assertTrue(engine.isTerminalSseFrame("data: {\"done\":\"true\"}"))
        assertTrue(engine.isTerminalSseFrame("data: {\"event\":\"response.done\"}"))
        assertTrue(engine.isTerminalSseFrame("data: {\"status\":\"completed\"}"))
        assertTrue(engine.isTerminalSseFrame("data: {\"finish_reason\":\"stop\"}"))
        assertTrue(
            engine.isTerminalSseFrame(
                "data: {\"choices\":[{\"delta\":{},\"finish_reason\":\"stop\"}]} ",
            ),
        )
        assertTrue(!engine.isTerminalSseFrame("data: {\"choices\":[],\"usage\":{\"total_tokens\":42}}"))
        assertTrue(
            engine.isTerminalSseFrame(
                "data: {\"choices\":[],\"usage\":{\"total_tokens\":42}}",
                hasActivity = true,
            ),
        )
        assertTrue(!engine.isTerminalSseFrame(": keep-alive"))
    }

    @Test
    fun `filters structured and inline reasoning including split tags`() {
        val engine = engine()
        val cumulative = StringBuilder()
        val filter = ThinkingBlockFilter()

        assertNull(
            engine.appendSseDelta(
                "data: {\"choices\":[{\"delta\":{\"reasoning_content\":{\"tokens\":[\"private\"]}}}]}",
                cumulative,
                filter,
            ),
        )
        assertNull(
            engine.appendSseDelta(
                "data: {\"choices\":[{\"delta\":{\"content\":[{\"type\":\"reasoning_details\",\"text\":\"also private\"}]}}]}",
                cumulative,
                filter,
            ),
        )
        assertNull(
            engine.appendSseDelta(
                "data: {\"choices\":[{\"delta\":{\"content\":[{\"type\":\"thinking_details\",\"text\":\"still private\"}]}}]}",
                cumulative,
                filter,
            ),
        )
        assertEquals("", cumulative.toString())

        assertNull(
            engine.appendSseDelta(
                "data: {\"choices\":[{\"delta\":{\"content\":\"<thi\"}}]}",
                cumulative,
                filter,
            ),
        )
        assertNull(
            engine.appendSseDelta(
                "data: {\"choices\":[{\"delta\":{\"content\":\"nk>private thought</thi\"}}]}",
                cumulative,
                filter,
            ),
        )
        assertEquals("", cumulative.toString())

        assertEquals(
            "Visible answer",
            engine.appendSseDelta(
                "data: {\"choices\":[{\"delta\":{\"content\":\"nk>Visible answer\"}}]}",
                cumulative,
                filter,
            ),
        )
        assertEquals("Visible answer", cumulative.toString())

        assertEquals(
            " and more",
            engine.appendSseDelta(
                "data: {\"choices\":[{\"delta\":{\"content\":[{\"type\":\"reasoning\",\"text\":\"ignored\"},{\"type\":\"text\",\"text\":\" and more\"}]}}]}",
                cumulative,
                filter,
            ),
        )
        assertEquals("Visible answer and more", cumulative.toString())
    }

    @Test
    fun `filters chat template analysis blocks including split start markers`() {
        val engine = engine()
        val cumulative = StringBuilder()
        val filter = ThinkingBlockFilter()

        assertNull(
            engine.appendSseDelta(
                "data: {\"choices\":[{\"delta\":{\"content\":\"<|start|>ana\"}}]}",
                cumulative,
                filter,
            ),
        )
        assertNull(
            engine.appendSseDelta(
                "data: {\"choices\":[{\"delta\":{\"content\":\"lysisprivate reasoning<|end_of_analysis|>\"}}]}",
                cumulative,
                filter,
            ),
        )
        assertEquals(
            "Visible after analysis",
            engine.appendSseDelta(
                "data: {\"choices\":[{\"delta\":{\"content\":\"Visible after analysis\"}}]}",
                cumulative,
                filter,
            ),
        )
        assertEquals("Visible after analysis", cumulative.toString())
    }

    @Test
    fun `filters uppercase inline reasoning markers`() {
        val engine = engine()
        val cumulative = StringBuilder()
        val filter = ThinkingBlockFilter()

        assertNull(
            engine.appendSseDelta(
                "data: {\"choices\":[{\"delta\":{\"content\":\"[THINK]private\"}}]}",
                cumulative,
                filter,
            ),
        )
        assertEquals(
            "Visible after thinking",
            engine.appendSseDelta(
                "data: {\"choices\":[{\"delta\":{\"content\":\"[/THINK]Visible after thinking\"}}]}",
                cumulative,
                filter,
            ),
        )
        assertEquals("Visible after thinking", cumulative.toString())
    }

    @Test
    fun `does not leak an unfinished thinking marker at end of stream`() {
        val engine = engine()
        val cumulative = StringBuilder()
        val filter = ThinkingBlockFilter()

        assertEquals(
            "Visible answer",
            engine.appendSseDelta(
                "data: {\"choices\":[{\"delta\":{\"content\":\"Visible answer<thi\"}}]}",
                cumulative,
                filter,
            ),
        )
        assertEquals("", filter.finish())
        assertEquals("Visible answer", cumulative.toString())
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
        assertEquals("hmm", reasoningDelta?.reasoningContent?.jsonPrimitive?.content)
        assertNull(chunk.choices.first().finishReason)

        val terminal =
            Json
                .decodeFromString(
                    ChatCompletionChunk.serializer(),
                    """{"choices":[{"delta":{},"finish_reason":"length"}]}""",
                )
        assertEquals("length", terminal.choices.first().finishReason)

        val completed =
            Json.decodeFromString<ChatCompletionChunk>(
                """{"choices":[{"message":{"content":"answer","reasoning_content":"private"}}]}""",
            )
        val completedMessage = completed.choices.first().message
        assertEquals("answer", completedMessage?.content?.jsonPrimitive?.content)
        assertEquals("private", completedMessage?.reasoningContent?.jsonPrimitive?.content)
    }

    @Test
    fun `stream end validation distinguishes clean done from truncation`() {
        assertNull(validateStreamEnd(sawDone = true, finishReason = null, contentLength = 10))
        assertNull(validateStreamEnd(sawDone = false, finishReason = "stop", contentLength = 10))
        assertEquals(
            "Cloud model returned no visible answer",
            validateStreamEnd(sawDone = true, finishReason = "stop", contentLength = 0),
        )
        assertNull(
            validateStreamEnd(
                sawDone = true,
                finishReason = "tool_calls",
                contentLength = 0,
                hasToolCallActivity = true,
            ),
        )
        assertEquals(
            "Cloud model reached its output limit before completing the answer",
            validateStreamEnd(sawDone = true, finishReason = "length", contentLength = 42),
        )

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
        assertEquals(
            "Cloud model ended the answer with finish reason 'content_filter'.",
            validateStreamEnd(sawDone = true, finishReason = "content_filter", contentLength = 10),
        )
        assertEquals(
            "Cloud model ended the answer with finish reason 'error'.",
            validateStreamEnd(sawDone = true, finishReason = "error", contentLength = 10),
        )
        assertEquals(
            "Cloud model ended with an unrecognized finish reason 'mystery'.",
            validateStreamEnd(sawDone = true, finishReason = "mystery", contentLength = 10),
        )
    }

    @Test
    fun `successful empty stream is surfaced instead of becoming a blank answer`() =
        runTest {
            val client =
                mockClient(
                    """
                    data: {"choices":[{"delta":{},"finish_reason":"stop"}]}
                    data: [DONE]
                    """.trimIndent(),
                )
            try {
                val streamingEngine = CloudRagLlmEngine(client) { config }

                val failure =
                    assertFailsWith<IllegalStateException> {
                        streamingEngine.generateStreaming("question", "instructions").toList()
                    }

                assertEquals("Cloud model returned no visible answer", failure.message)
            } finally {
                client.close()
            }
        }

    @Test
    fun `usage-only trailer closes a completed streaming response`() =
        runTest {
            val client =
                mockClient(
                    """
                    data: {"choices":[{"delta":{"content":"Answer"}}]}
                    data: {"choices":[],"usage":{"total_tokens":42}}
                    """.trimIndent(),
                )
            try {
                val streamingEngine = CloudRagLlmEngine(client) { config }

                assertEquals(
                    listOf("Answer"),
                    streamingEngine.generateStreaming("question", "instructions").toList(),
                )
            } finally {
                client.close()
            }
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

    @Test
    fun `accepts a bare non-streaming chat completion body`() {
        val engine = engine()
        val cumulative = StringBuilder()
        val body = """{"choices":[{"message":{"role":"assistant","content":"A complete answer."}}]}"""

        assertEquals("A complete answer.", engine.appendSseDelta(body, cumulative))
        assertEquals("A complete answer.", cumulative.toString())
        assertTrue(engine.isTerminalSseFrame(body))
        assertNull(validateStreamEnd(sawDone = true, finishReason = null, contentLength = cumulative.length))
    }

    @Test
    fun `accepts legacy text completion content`() {
        val engine = engine()
        val cumulative = StringBuilder()
        val body = """{"choices":[{"text":"Legacy answer"}]}"""

        assertEquals("Legacy answer", engine.appendSseDelta(body, cumulative))
        assertEquals("Legacy answer", cumulative.toString())
        assertTrue(engine.isTerminalSseFrame(body))
    }

    @Test
    fun `streaming request emits cumulative snapshots from an sse response`() =
        runTest {
            val client = mockClient(SSE_CONTENT_RESPONSE)
            try {
                val streamingEngine = CloudRagLlmEngine(client) { config }

                assertEquals(
                    listOf("Hello", "Hello world"),
                    streamingEngine.generateStreaming("question", "instructions").toList(),
                )
            } finally {
                client.close()
            }
        }

    @Test
    fun `local streaming request omits a stale hosted authorization key`() =
        runTest {
            var authorization: String? = null
            val client =
                HttpClient(
                    MockEngine { request ->
                        authorization = request.headers[HttpHeaders.Authorization]
                        respond(
                            content = SSE_CONTENT_RESPONSE,
                            headers = headersOf(HttpHeaders.ContentType, ContentType.Text.EventStream.toString()),
                        )
                    },
                ) {
                    install(ContentNegotiation) {
                        json(
                            Json {
                                explicitNulls = false
                            },
                        )
                    }
                }
            try {
                val localConfig =
                    config.copy(
                        baseUrl = "http://127.0.0.1:11434/v1",
                        allowInsecureLocalEndpoint = true,
                    )
                val streamingEngine = CloudRagLlmEngine(client) { localConfig }

                assertEquals(
                    listOf("Hello", "Hello world"),
                    streamingEngine.generateStreaming("question", "instructions").toList(),
                )
                assertNull(authorization)
            } finally {
                client.close()
            }
        }

    @Test
    fun `tool streaming request assembles fragmented calls`() =
        runTest {
            val client = mockClient(toolCallResponse)
            try {
                val streamingEngine = CloudRagLlmEngine(client) { config }
                val tool =
                    RagToolDefinition(
                        name = "weight_summary",
                        description = "Summarize weights.",
                        parameters = Json.parseToJsonElement("""{"type":"object","properties":{}}""").jsonObject,
                    )

                assertEquals(
                    listOf(
                        RagToolStreamEvent.ToolCalls(
                            listOf(
                                RagToolCall(
                                    id = "call-1",
                                    name = "weight_summary",
                                    arguments = "{\"patient_name\":\"Descarada\"}",
                                ),
                            ),
                        ),
                    ),
                    streamingEngine
                        .generateStreamingWithTools(
                            messages = listOf(RagChatMessage(RagChatRole.USER, "Analyze weights")),
                            tools = listOf(tool),
                        ).toList(),
                )
            } finally {
                client.close()
            }
        }

    @Test
    fun `malformed fragmented tool arguments are not forwarded to the registry`() =
        runTest {
            val client =
                mockClient(
                    """
                    data: {"choices":[{"delta":{"tool_calls":[{"index":0,"id":"call-1","type":"function","function":{"name":"weight_summary","arguments":"{\"patient_name\":"}}]}}]}
                    data: {"choices":[{"delta":{},"finish_reason":"tool_calls"}]}
                    data: [DONE]
                    """.trimIndent(),
                )
            try {
                val streamingEngine = CloudRagLlmEngine(client) { config }
                val tool =
                    RagToolDefinition(
                        name = "weight_summary",
                        description = "Summarize weights.",
                        parameters = Json.parseToJsonElement("""{"type":"object","properties":{}}""").jsonObject,
                    )

                assertTrue(
                    streamingEngine
                        .generateStreamingWithTools(
                            messages = listOf(RagChatMessage(RagChatRole.USER, "Analyze weights")),
                            tools = listOf(tool),
                        ).toList()
                        .isEmpty(),
                )
            } finally {
                client.close()
            }
        }

    @Test
    fun `http failure is surfaced instead of becoming an empty answer`() =
        runTest {
            val client = mockClient("quota exceeded", HttpStatusCode.TooManyRequests)
            try {
                val streamingEngine = CloudRagLlmEngine(client) { config }

                val failure =
                    assertFailsWith<IllegalStateException> {
                        streamingEngine.generateStreaming("question", "instructions").toList()
                    }

                assertTrue(failure.message.orEmpty().contains("HTTP 429"))
                assertTrue(failure.message.orEmpty().contains("quota exceeded"))
            } finally {
                client.close()
            }
        }

    private fun mockClient(
        body: String,
        status: HttpStatusCode = HttpStatusCode.OK,
    ): HttpClient =
        HttpClient(
            MockEngine {
                respond(
                    content = body,
                    status = status,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Text.EventStream.toString()),
                )
            },
        ) {
            install(ContentNegotiation) {
                json(
                    Json {
                        explicitNulls = false
                    },
                )
            }
        }

    private companion object {
        const val SSE_CONTENT_RESPONSE =
            "data: {\"choices\":[{\"delta\":{\"content\":\"Hello\"}}]}\n" +
                "data: {\"choices\":[{\"delta\":{\"content\":\" world\"}}]}\n" +
                "data: [DONE]\n"
        val toolCallResponse =
            """
            data: {"choices":[{"delta":{"tool_calls":[{"index":0,"id":"call-1","type":"function","function":{"name":"weight_summary","arguments":"{\"patient_name\":\"Descarada\""}}]}}]}
            data: {"choices":[{"delta":{"tool_calls":[{"index":0,"function":{"arguments":"}"}}]}}]}
            data: {"choices":[{"delta":{},"finish_reason":"tool_calls"}]}
            data: [DONE]
            """.trimIndent()
    }
}
