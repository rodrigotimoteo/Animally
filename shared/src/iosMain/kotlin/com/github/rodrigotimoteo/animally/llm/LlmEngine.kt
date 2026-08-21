package com.github.rodrigotimoteo.animally.llm

import com.github.rodrigotimoteo.animally.llm.fm.FmLlmShim
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

    actual fun generate(prompt: String): Flow<String> =
        flow {
            val result =
                suspendCancellableCoroutine { cont ->
                    shim.generate(prompt) { text, error ->
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
