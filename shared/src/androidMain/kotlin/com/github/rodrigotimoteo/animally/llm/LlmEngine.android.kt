package com.github.rodrigotimoteo.animally.llm

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Android actual of [LlmEngine]. The on-device LLM feature is iOS-first (Apple Foundation
 * Models); no Android backend exists yet, so the engine reports itself unavailable and
 * [generate] yields a clear, non-crashing message.
 */
actual class LlmEngine actual constructor(
    config: LlmConfig,
) {
    actual fun generate(prompt: String): Flow<String> =
        flow {
            emit("LLM assistant is not available on Android.")
        }

    actual fun generate(
        prompt: String,
        instructions: String,
    ): Flow<String> =
        flow {
            emit("LLM assistant is not available on Android.")
        }

    actual fun generateStructured(
        prompt: String,
        schema: String,
    ): Flow<String> =
        flow {
            emit("LLM assistant is not available on Android.")
        }

    actual suspend fun availability(): LlmAvailability = LlmAvailability.Unavailable(UnavailableReason.NO_LOCAL_MODEL)
}
