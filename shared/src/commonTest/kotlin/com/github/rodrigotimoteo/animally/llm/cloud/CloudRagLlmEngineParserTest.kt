package com.github.rodrigotimoteo.animally.llm.cloud

import io.ktor.client.HttpClient
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CloudRagLlmEngineParserTest {
    private val httpClient = HttpClient()
    private val engine = CloudRagLlmEngine(httpClient) { CloudLlmConfig(apiKey = "test") }

    @AfterTest
    fun closeClient() {
        httpClient.close()
    }

    @Test
    fun splitInlineThinkingMarkersNeverReachVisibleAnswer() {
        val cumulative = StringBuilder()
        val filter = ThinkingBlockFilter()

        assertEquals(
            "",
            engine
                .appendSseDelta(
                    """data: {"choices":[{"delta":{"content":"<thi"}}]}""",
                    cumulative,
                    filter,
                ).orEmpty(),
        )
        assertEquals(
            "Answer",
            engine
                .appendSseDelta(
                    """data: {"choices":[{"delta":{"content":"nk>private</think>Answer"}}]}""",
                    cumulative,
                    filter,
                ).orEmpty(),
        )
        assertEquals("Answer", cumulative.toString())
    }

    @Test
    fun structuredReasoningContentAndThinkingBlocksAreOmitted() {
        val cumulative = StringBuilder()
        val line =
            """data: {"choices":[{"delta":{"reasoning_content":"private","content":[{"type":"thinking","text":"also private"},{"type":"text","text":"Visible"}]}}]}"""

        assertEquals("Visible", engine.appendSseDelta(line, cumulative).orEmpty())
        assertEquals("Visible", cumulative.toString())
    }

    @Test
    fun unfinishedHiddenReasoningIsDiscardedAtEof() {
        val cumulative = StringBuilder()
        val filter = ThinkingBlockFilter()

        assertEquals(
            "",
            engine
                .appendSseDelta(
                    """data: {"choices":[{"delta":{"content":"<thinking>private"}}]}""",
                    cumulative,
                    filter,
                ).orEmpty(),
        )
        assertEquals("", filter.finish())
        assertEquals("", cumulative.toString())
    }

    @Test
    fun ordinaryCompletionJsonAndLegacyTextCompletionAreAccepted() {
        val completion =
            """{"choices":[{"message":{"content":"Completed"},"finish_reason":"stop"}]}"""
        val legacy = """{"choices":[{"text":"Legacy","finish_reason":"eos"}]}"""

        assertEquals("Completed", engine.appendSseDelta(completion, StringBuilder()).orEmpty())
        assertTrue(engine.isTerminalSseFrame(completion))
        assertEquals("Legacy", engine.appendSseDelta(legacy, StringBuilder()).orEmpty())
        assertTrue(engine.isTerminalSseFrame(legacy))
    }

    @Test
    fun providerTerminalMarkersIncludeOpenCodeCostFrameAndResponseEvents() {
        assertTrue(engine.isTerminalSseFrame("data: [DONE]"))
        assertTrue(engine.isTerminalSseFrame("""data: {"choices":[],"cost":"0"}"""))
        assertTrue(engine.isTerminalSseFrame("event: response.completed"))
        assertFalse(engine.isTerminalSseFrame("""data: {"choices":[],"usage":{"total_tokens":42}}"""))
        assertTrue(
            engine.isTerminalSseFrame(
                """data: {"choices":[],"usage":{"total_tokens":42}}""",
                hasActivity = true,
            ),
        )
        assertFalse(engine.isTerminalSseFrame("""data: {"choices":[]}"""))
    }

    @Test
    fun streamEndValidationAcceptsEosAndToolCallsButRejectsIncompleteOutput() {
        assertNull(validateStreamEnd(sawDone = true, finishReason = "eos", contentLength = 12))
        assertNull(
            validateStreamEnd(
                sawDone = true,
                finishReason = "tool_calls",
                contentLength = 0,
                hasToolCallActivity = true,
            ),
        )
        assertEquals(
            "Cloud LLM stream ended before completion",
            validateStreamEnd(sawDone = false, finishReason = null, contentLength = 12),
        )
        assertEquals(
            "Cloud model reached its output limit before completing the answer",
            validateStreamEnd(sawDone = true, finishReason = "length", contentLength = 12),
        )
        assertEquals(
            "Cloud model returned no visible answer",
            validateStreamEnd(sawDone = true, finishReason = "stop", contentLength = 0),
        )
    }
}
