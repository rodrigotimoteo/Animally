package com.github.rodrigotimoteo.animally.llm

import com.github.rodrigotimoteo.animally.llm.fm.FmLlmShim
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * iOS actual of [LlmEngine], routing to Apple Foundation Models through the Swift
 * @objc(FmLlmShim) shim bound via header-only cinterop. FM requires iOS 26+ with Apple
 * Intelligence; on unsupported devices [availability] reports the concrete reason.
 */
@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
actual class LlmEngine actual constructor(
    config: LlmConfig,
) {
    private val shim = FmLlmShim()

    actual fun generate(prompt: String): Flow<String> = generate(prompt, instructions = "")

    actual fun generate(
        prompt: String,
        instructions: String,
    ): Flow<String> =
        flow {
            val result =
                suspendCancellableCoroutine { cont ->
                    shim.generateWithInstructions(prompt, instructions) { text, error ->
                        cont.resume(if (error != null) null else text)
                    }
                }
            result?.let { emit(it) }
        }

    actual fun generateStructured(
        prompt: String,
        schema: String,
    ): Flow<String> =
        flow {
            val result =
                suspendCancellableCoroutine { cont ->
                    shim.generateJson(prompt, schema) { text, error ->
                        cont.resume(if (error != null) null else text)
                    }
                }
            result?.let { emit(it) }
        }

    /**
     * Streams cumulative snapshots from the shim: every onChunk carries the
     * full text so far, and the CONFLATED channel guarantees a slow collector
     * always sees the latest snapshot instead of queuing stale ones.
     * Collector cancellation unwinds the iteration and the finally block
     * calls shim.cancelStream(), which cancels the Swift task. The shim
     * allows one active stream per instance; this engine holds exactly one.
     */
    actual fun generateStreaming(
        prompt: String,
        instructions: String,
    ): Flow<String> =
        flow {
            val channel = Channel<String>(Channel.CONFLATED)
            try {
                shim.streamResponseWithInstructions(
                    prompt,
                    instructions,
                    onChunk = { chunk -> chunk?.let { channel.trySend(it) } },
                    onComplete = { finalText, error ->
                        when {
                            error != null -> channel.close(Throwable(error))
                            else -> {
                                // Re-emit final for zero-chunk short answers;
                                // duplicate of the last snapshot is harmless.
                                finalText?.let { channel.trySend(it) }
                                channel.close()
                            }
                        }
                    },
                )
                for (chunk in channel) emit(chunk)
            } finally {
                shim.cancelStream()
                channel.close()
            }
        }

    actual suspend fun availability(): LlmAvailability = parseAvailability(shim.availability())

    private fun parseAvailability(raw: String?): LlmAvailability =
        when {
            raw == "available" -> LlmAvailability.Available
            raw == null -> LlmAvailability.Unavailable(UnavailableReason.UNKNOWN)
            raw.contains("deviceNotEligible") ->
                LlmAvailability.Unavailable(UnavailableReason.DEVICE_NOT_ELIGIBLE)
            raw.contains("appleIntelligenceNotEnabled") ->
                LlmAvailability.Unavailable(UnavailableReason.APPLE_INTELLIGENCE_NOT_ENABLED)
            raw.contains("modelNotReady") ->
                LlmAvailability.Unavailable(UnavailableReason.MODEL_NOT_READY)
            else -> LlmAvailability.Unavailable(UnavailableReason.UNKNOWN)
        }
}
