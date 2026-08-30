package com.github.rodrigotimoteo.animally.llm.cloud

import com.github.rodrigotimoteo.animally.llm.RagChatMessage
import com.github.rodrigotimoteo.animally.llm.RagLlmEngine
import com.github.rodrigotimoteo.animally.llm.RagQueryPolicy
import com.github.rodrigotimoteo.animally.llm.RagToolCallingEngine
import com.github.rodrigotimoteo.animally.llm.RagToolDefinition
import com.github.rodrigotimoteo.animally.llm.RagToolStreamEvent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ChannelResult
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/** Which engine produced an assistant answer. Drives the "Answered by cloud model" badge. */
enum class EngineSource {
    ON_DEVICE,
    CLOUD,
}

/** Result of streaming the primary engine: why the fallback must take over, if at all. */
private data class PrimaryOutcome(
    val failure: Throwable?,
    val completed: Boolean,
)

/**
 * Delegating [RagLlmEngine]: uses the configured cloud [fallback] first and
 * keeps the on-device [primary] available as a compatibility fallback path.
 * GenerateRagResponseUseCase stays untouched — it only ever sees a RagLlmEngine.
 *
 * Failure detection per request for the local-first compatibility path:
 * - upfront: [isPrimaryAvailable] returns false -> straight to fallback;
 * - stall: no first emission within [firstEmissionTimeout] -> fallback;
 * - error: primary flow throws before completing -> fallback, even mid-stream
 *   (consumers replace their buffer with each cumulative emission, so re-streaming
 *   from the fallback cleanly overwrites any partial on-device text).
 *
 * Every resolution is announced on [sourceEvents] BEFORE the chosen engine's first
 * chunk flows downstream, so UIs can badge the answer as cloud-sourced or on-device.
 * User cancellation always propagates untouched. When the primary fails while
 * [isFallbackEligible] is false (cloud disabled / no key), the original failure is
 * rethrown so the caller's normal interrupted/error path handles it.
 */
class FmFirstRagLlmEngine(
    private val primary: RagLlmEngine,
    private val fallback: RagLlmEngine,
    private val firstEmissionTimeout: Duration = DEFAULT_FIRST_EMISSION_TIMEOUT,
    private val isFallbackEligible: () -> Boolean = { true },
    private val isPrimaryAvailable: suspend () -> Boolean = { true },
) : RagLlmEngine,
    RagToolCallingEngine {
    private val _sourceEvents = MutableSharedFlow<EngineSource>(replay = 1, extraBufferCapacity = 16)

    /** Emits [EngineSource] once per request, before that engine's first chunk. */
    val sourceEvents: SharedFlow<EngineSource> = _sourceEvents

    /** Tool calls intentionally select the cloud engine; Foundation Models have no tool seam. */
    override val supportsToolCalling: Boolean
        get() = isFallbackEligible() && (fallback as? RagToolCallingEngine)?.supportsToolCalling == true

    override fun generateStreamingWithTools(
        messages: List<RagChatMessage>,
        tools: List<RagToolDefinition>,
    ): Flow<RagToolStreamEvent> =
        flow {
            val toolEngine = fallback as? RagToolCallingEngine
            if (!isFallbackEligible() || toolEngine == null || !toolEngine.supportsToolCalling) {
                error("Cloud tool calling is not configured for this assistant.")
            }
            signal(EngineSource.CLOUD)
            toolEngine.generateStreamingWithTools(messages, tools).collect { emit(it) }
        }

    /**
     * Reports the policy the next assistant turn should use. A configured cloud
     * provider is deliberately selected for every assistant question, including
     * grounded record lookups; grounding gates still prevent unsupported patient
     * facts from reaching either model.
     */
    suspend fun queryPolicy(): RagQueryPolicy =
        if (isFallbackEligible()) {
            RagQueryPolicy.CLOUD
        } else {
            RagQueryPolicy.ON_DEVICE
        }

    /**
     * The selected cloud provider is used for every assistant question while it
     * is configured. The query parameter remains part of the public seam for
     * callers that classify questions before routing, but routing is no longer
     * dependent on the wording of the question.
     */
    @Suppress("UNUSED_PARAMETER")
    suspend fun queryPolicy(query: String): RagQueryPolicy = queryPolicy()

    /**
     * Non-streaming variant with the same routing semantics. Delegates to
     * [generateStreaming] because both engines' streaming flows emit cumulative
     * text whose final value IS the full response.
     */
    override fun generate(
        prompt: String,
        instructions: String,
    ): Flow<String> = generateStreaming(prompt, instructions)

    override fun generateStreaming(
        prompt: String,
        instructions: String,
    ): Flow<String> =
        flow {
            var usePrimary = isPrimaryAvailable()
            var primaryFailure: Throwable? = null
            if (usePrimary) {
                val outcome = streamPrimary(prompt, instructions)
                primaryFailure = outcome.failure
                usePrimary = outcome.completed
            }
            if (!usePrimary) {
                if (!isFallbackEligible()) {
                    primaryFailure?.let { throw it }
                    error("On-device generation returned no answer")
                }
                signal(EngineSource.CLOUD)
                fallback.generateStreaming(prompt, instructions).collect { emit(it) }
            }
        }

    /**
     * Bypasses Foundation Models for a general cloud turn. This matters when
     * the local model is technically available but the user selected a paid
     * cloud provider for questions outside the record corpus.
     */
    override fun generateCloudFirst(
        prompt: String,
        instructions: String,
    ): Flow<String> =
        flow {
            if (isFallbackEligible()) {
                signal(EngineSource.CLOUD)
                fallback.generateStreaming(prompt, instructions).collect { emit(it) }
            } else {
                generateStreaming(prompt, instructions).collect { emit(it) }
            }
        }

    /**
     * Streams [primary] into the downstream collector, announcing ON_DEVICE before
     * its first chunk. Returns the routing outcome: [PrimaryOutcome.completed] is
     * true only when the primary finished normally after at least one emission;
     * otherwise [PrimaryOutcome.failure] carries why the fallback must take over.
     */
    private suspend fun FlowCollector<String>.streamPrimary(
        prompt: String,
        instructions: String,
    ): PrimaryOutcome =
        coroutineScope {
            // Own channel + contained producer: a primary failure must reach the loop
            // below as a closed-channel result, NOT cancel this coroutineScope (a
            // failed produceIn child would tear the scope down before the fallback
            // decision could be made).
            val upstream = Channel<String>(Channel.UNLIMITED)
            val producer = launchPrimaryStream(prompt, instructions, upstream)
            try {
                collectPrimary(upstream)
            } finally {
                producer.cancel()
                upstream.close()
            }
        }

    private fun CoroutineScope.launchPrimaryStream(
        prompt: String,
        instructions: String,
        upstream: Channel<String>,
    ) = launch {
        try {
            primary.generateStreaming(prompt, instructions).collect { upstream.send(it) }
            upstream.close()
        } catch (ce: CancellationException) {
            throw ce
        } catch (t: Throwable) {
            upstream.close(t)
        }
    }

    private suspend fun FlowCollector<String>.collectPrimary(upstream: ReceiveChannel<String>): PrimaryOutcome {
        var emittedAny = false
        while (true) {
            val result = nextPrimaryResult(upstream, emittedAny) ?: return PrimaryOutcome(null, completed = false)
            if (!result.isSuccess) {
                return if (emittedAny && result.exceptionOrNull() == null) {
                    PrimaryOutcome(failure = null, completed = true)
                } else {
                    PrimaryOutcome(failure = result.exceptionOrNull(), completed = false)
                }
            }
            val text = result.getOrThrow()
            if (text.isNotBlank()) {
                if (!emittedAny) {
                    emittedAny = true
                    signal(EngineSource.ON_DEVICE)
                }
                emit(text)
            }
        }
    }

    private suspend fun nextPrimaryResult(
        upstream: ReceiveChannel<String>,
        emittedAny: Boolean,
    ): ChannelResult<String>? = if (emittedAny) upstream.receiveCatching() else awaitFirstEmission(upstream)

    /**
     * Awaits the primary's first delivery under [firstEmissionTimeout]; null means the
     * timeout fired first. The channel itself is left open for the caller to cancel.
     */
    private suspend fun awaitFirstEmission(upstream: ReceiveChannel<String>): ChannelResult<String>? =
        withTimeoutOrNull(firstEmissionTimeout) { upstream.receiveCatching() }

    private fun signal(source: EngineSource) {
        _sourceEvents.tryEmit(source)
    }

    private companion object {
        val DEFAULT_FIRST_EMISSION_TIMEOUT = 30.seconds
    }
}
