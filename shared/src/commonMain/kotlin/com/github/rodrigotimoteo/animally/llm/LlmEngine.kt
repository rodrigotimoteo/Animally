package com.github.rodrigotimoteo.animally.llm

import kotlinx.coroutines.flow.Flow

/**
 * Platform LLM engine contract. iOS routes to Apple Foundation Models via a Swift shim;
 * Android/desktop report unavailable until their backends exist. Retrieval-augmented
 * generation lives in [GenerateRagResponseUseCase], which owns the search dependency.
 */
expect class LlmEngine(
    config: LlmConfig = LlmConfig(),
) {
    fun generate(prompt: String): Flow<String>

    /** Generates a response for [prompt] grounded in [instructions] (system prompt). */
    fun generate(
        prompt: String,
        instructions: String,
    ): Flow<String>

    fun generateStructured(
        prompt: String,
        schema: String,
    ): Flow<String>

    suspend fun availability(): LlmAvailability
}
