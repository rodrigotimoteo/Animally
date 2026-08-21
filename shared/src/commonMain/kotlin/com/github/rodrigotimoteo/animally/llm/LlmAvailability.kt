package com.github.rodrigotimoteo.animally.llm

import kotlinx.serialization.Serializable

@Serializable
sealed interface LlmAvailability {
    @Serializable
    data object Available : LlmAvailability

    @Serializable
    data class Unavailable(
        val reason: UnavailableReason,
    ) : LlmAvailability

    @Serializable
    data class Loading(
        val engine: EngineType,
    ) : LlmAvailability
}

@Serializable
enum class UnavailableReason {
    DEVICE_NOT_ELIGIBLE,
    APPLE_INTELLIGENCE_NOT_ENABLED,
    MODEL_NOT_READY,
    NO_LOCAL_MODEL,
    UNKNOWN,
}

@Serializable
enum class EngineType {
    FOUNDATION_MODELS,
    LLAMA_CPP,
}
