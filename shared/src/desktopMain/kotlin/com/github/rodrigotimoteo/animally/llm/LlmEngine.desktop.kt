package com.github.rodrigotimoteo.animally.llm

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Desktop (JVM) actual of [LlmEngine]. The on-device LLM feature is iOS-first (Apple
 * Foundation Models); no desktop backend exists, so the engine reports itself unavailable
 * and [generate] yields a clear, non-crashing message.
 */
actual class LlmEngine actual constructor(
    config: LlmConfig,
) {
    actual fun generate(prompt: String): Flow<String> =
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

    actual suspend fun availability(): LlmAvailability = LlmAvailability.Unavailable(UnavailableReason.NO_LOCAL_MODEL)
}
