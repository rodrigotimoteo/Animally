package com.github.rodrigotimoteo.animally.llm.cloud

import com.github.rodrigotimoteo.animally.llm.RagLlmEngine
import com.github.rodrigotimoteo.animally.llm.RagToolCallingEngine
import com.github.rodrigotimoteo.animally.llm.RagToolDefinition
import com.github.rodrigotimoteo.animally.llm.RagToolStreamEvent
import io.ktor.client.HttpClient
import io.ktor.client.request.preparePost
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.utils.io.readUTF8Line
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow

/**
 * Facade RAG engine routing generation to an OpenAI-compatible chat
 * completions endpoint with `stream: true`. Delegates SSE parsing to
 * [CloudStreamParser], tool-call accumulation to [CloudToolDispatcher],
 * auth/header wiring to [CloudAuth] ([applyCloudLlmRequest]) and
 * transport validation to [CloudRetry].
 *
 * Preserves the cumulative streaming contract documented on
 * [RagLlmEngine] so [FmFirstRagLlmEngine] can keep its fallback logic
 * unchanged.
 */
@Suppress("TooManyFunctions")
class CloudRagLlmEngine(
    private val httpClient: HttpClient,
    private val configProvider: () -> CloudLlmConfig,
) : RagLlmEngine,
    RagToolCallingEngine {
    override fun generate(
        prompt: String,
        instructions: String,
    ): Flow<String> = generateStreaming(prompt, instructions)

    override fun generateStreaming(
        prompt: String,
        instructions: String,
    ): Flow<String> =
        flow {
            val config = configProvider()
            streamRequest(
                buildChatCompletionRequest(
                    config,
                    prompt,
                    instructions,
                ),
                config,
            ).collect { event ->
                if (event is RagToolStreamEvent.Text) emit(event.text)
            }
        }

    override val supportsToolCalling: Boolean = true

    override fun generateStreamingWithTools(
        messages: List<com.github.rodrigotimoteo.animally.llm.RagChatMessage>,
        tools: List<RagToolDefinition>,
    ): Flow<RagToolStreamEvent> {
        require(tools.isNotEmpty()) { "At least one tool definition is required." }
        val config = configProvider()
        return streamRequest(
            buildToolChatCompletionRequest(
                config = config,
                messages = messages,
                tools = tools,
            ),
            config,
        )
    }

    private fun streamRequest(
        request: ChatCompletionRequest,
        config: CloudLlmConfig,
    ): Flow<RagToolStreamEvent> =
        channelFlow {
            httpClient
                .preparePost(cloudChatCompletionsUrl(config.baseUrl)) {
                    applyCloudLlmRequest(this, config, request)
                }.execute { response ->
                    CloudRetry.ensureSuccessful(response)
                    streamResponse(response, ::send)
                }
        }

    private suspend fun streamResponse(
        response: HttpResponse,
        emit: suspend (RagToolStreamEvent) -> Unit,
    ) {
        val state = StreamState()
        val channel = response.bodyAsChannel()
        var shouldRead = true
        while (shouldRead && !channel.isClosedForRead) {
            val line = channel.readUTF8Line()
            if (line == null) {
                shouldRead = false
            } else {
                processSseLine(line, state, emit)
                shouldRead = !state.sawDone
            }
        }
        finishStream(state, emit)
    }

    private suspend fun processSseLine(
        line: String,
        state: StreamState,
        emit: suspend (RagToolStreamEvent) -> Unit,
    ) {
        CloudStreamParser.parseSseError(line)?.let { message -> error(message) }
        val payload = CloudStreamParser.dataPayload(line)
        if (payload?.startsWith("{") == true) {
            if (CloudStreamParser.decodeChunk(payload) == null) {
                state.malformedDataFrames++
                if (state.malformedDataFrames >= CloudStreamParser.MAX_CONSECUTIVE_MALFORMED_DATA_FRAMES) {
                    error("Cloud LLM stream returned malformed data")
                }
                return
            }
            state.malformedDataFrames = 0
        }
        if (CloudStreamParser.appendSseDelta(line, state.cumulative, state.thinkingFilter)?.isNotEmpty() == true) {
            emit(RagToolStreamEvent.Text(state.cumulative.toString()))
        }
        val toolDeltas = CloudToolDispatcher.extractToolCallDeltas(line)
        if (toolDeltas.isNotEmpty()) {
            CloudToolDispatcher.appendToolCallDeltas(state.toolCalls, toolDeltas)
        }
        if (CloudStreamParser.isTerminalSseFrame(line, hasActivity = state.hasActivity())) state.sawDone = true
        CloudStreamParser.parseFinishReason(line)?.let { state.finishReason = it }
    }

    private suspend fun finishStream(
        state: StreamState,
        emit: suspend (RagToolStreamEvent) -> Unit,
    ) {
        state.thinkingFilter
            .finish()
            .takeIf(String::isNotEmpty)
            ?.let { tail ->
                state.cumulative.append(tail)
                emit(RagToolStreamEvent.Text(state.cumulative.toString()))
            }
        val calls = CloudToolDispatcher.toRagToolCalls(state.toolCalls)
        val failure =
            validateStreamEnd(
                sawDone = state.sawDone,
                finishReason = state.finishReason,
                contentLength = state.cumulative.length,
                hasToolCallActivity = state.toolCalls.isNotEmpty(),
            )
        if (failure != null) error(failure)
        if (calls.isNotEmpty()) emit(RagToolStreamEvent.ToolCalls(calls))
    }

    private class StreamState {
        val cumulative = StringBuilder()
        val thinkingFilter = ThinkingBlockFilter()
        val toolCalls = linkedMapOf<Int, MutableCloudToolCall>()
        var sawDone = false
        var finishReason: String? = null
        var malformedDataFrames = 0

        fun hasActivity(): Boolean = cumulative.isNotEmpty() || toolCalls.isNotEmpty()
    }

    /**
     * Preserved for contract tests: delegates to [CloudStreamParser].
     */
    internal fun appendSseDelta(
        line: String,
        cumulative: StringBuilder,
        thinkingFilter: ThinkingBlockFilter = ThinkingBlockFilter(),
    ): String? = CloudStreamParser.appendSseDelta(line, cumulative, thinkingFilter)

    /**
     * Preserved for contract tests: delegates to [CloudStreamParser].
     */
    internal fun isTerminalSseFrame(
        line: String,
        hasActivity: Boolean = false,
    ): Boolean = CloudStreamParser.isTerminalSseFrame(line, hasActivity)
}
