package com.github.rodrigotimoteo.animally.llm

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Desktop (JVM) actual of [LlmEngine]. The on-device LLM feature is iOS-first (Apple
 * Foundation Models); no desktop backend exists, so the engine reports itself unavailable
 * and [generate] yields a clear, non-crashing message.
 *
 * [generateStreaming] emits a short canned multi-chunk sequence so the streaming UI path
 * is exercisable on platforms without Foundation Models.
 */
actual class LlmEngine actual constructor(
    config: LlmConfig,
) {
    actual fun generate(prompt: String): Flow<String> =
        flow {
            emit("LLM assistant is not available on desktop.")
        }

    actual fun generate(
        prompt: String,
        instructions: String,
    ): Flow<String> =
        flow {
            emit("LLM assistant is not available on desktop.")
        }

    actual fun generateStructured(
        prompt: String,
        schema: String,
    ): Flow<String> =
        flow {
            emit("LLM assistant is not available on desktop.")
        }

    actual fun generateStreaming(
        prompt: String,
        instructions: String,
    ): Flow<String> =
        flow {
            var cumulative = ""
            for (part in CANNED_PARTS) {
                delay(CHUNK_DELAY_MS)
                cumulative += part
                emit(cumulative)
            }
        }

    actual suspend fun availability(): LlmAvailability = LlmAvailability.Unavailable(UnavailableReason.NO_LOCAL_MODEL)

    private companion object {
        const val CHUNK_DELAY_MS = 120L

        /** Cumulative chunks simulating a progressive answer about a vaccination record. */
        val CANNED_PARTS =
            listOf(
                "Looking at your records: Thunder received his tetanus booster on May 1, 2024.",
                " The next dose is due May 1, 2025.",
                " No adverse reactions were recorded after the last shot.",
            )
    }
}
