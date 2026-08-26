package com.github.rodrigotimoteo.animally.llm.cloud

import app.cash.turbine.testIn
import app.cash.turbine.turbineScope
import com.github.rodrigotimoteo.animally.llm.RagLlmEngine
import com.github.rodrigotimoteo.animally.llm.RagQueryPolicy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Duration.Companion.milliseconds

class FmFirstRagLlmEngineTest {
    private class RecordingEngine(
        private val emissions: List<String>,
        private val errorAfter: Throwable? = null,
    ) : RagLlmEngine {
        var calls: Int = 0

        override fun generate(
            prompt: String,
            instructions: String,
        ): Flow<String> = generateStreaming(prompt, instructions)

        override fun generateStreaming(
            prompt: String,
            instructions: String,
        ): Flow<String> =
            flow {
                calls += 1
                emissions.forEach { emit(it) }
                errorAfter?.let { throw it }
            }
    }

    @Test
    fun `primary success streams through and signals on-device once`() =
        runTest {
            turbineScope {
                val fallback = RecordingEngine(emissions = listOf("SHOULD_NOT_APPEAR"))
                val engine =
                    FmFirstRagLlmEngine(
                        primary = RecordingEngine(emissions = listOf("A", "AB")),
                        fallback = fallback,
                    )
                val sources = engine.sourceEvents.testIn(this)
                val chunks = engine.generateStreaming("prompt", "instructions").toList()
                assertEquals(listOf("A", "AB"), chunks)
                assertEquals(EngineSource.ON_DEVICE, sources.awaitItem())
                sources.ensureAllEventsConsumed()
                sources.cancel()
                assertEquals(0, fallback.calls)
            }
        }

    @Test
    fun `primary error before any emission falls back to cloud`() =
        runTest {
            turbineScope {
                val primary = RecordingEngine(emissions = emptyList(), errorAfter = RuntimeException("fm down"))
                val fallback = RecordingEngine(emissions = listOf("CLOUD"))
                val engine = FmFirstRagLlmEngine(primary = primary, fallback = fallback)
                val sources = engine.sourceEvents.testIn(this)
                val chunks = engine.generateStreaming("prompt", "instructions").toList()
                assertEquals(listOf("CLOUD"), chunks)
                assertEquals(EngineSource.CLOUD, sources.awaitItem())
                sources.cancel()
            }
        }

    @Test
    fun `primary mid-stream error switches to fallback which restreams the answer`() =
        runTest {
            turbineScope {
                val primary = RecordingEngine(emissions = listOf("PARTIAL"), errorAfter = RuntimeException("dropped"))
                val fallback = RecordingEngine(emissions = listOf("CLOUD"))
                val engine = FmFirstRagLlmEngine(primary = primary, fallback = fallback)
                val sources = engine.sourceEvents.testIn(this)
                val chunks = engine.generateStreaming("prompt", "instructions").toList()
                assertEquals(listOf("PARTIAL", "CLOUD"), chunks)
                assertEquals(EngineSource.ON_DEVICE, sources.awaitItem())
                assertEquals(EngineSource.CLOUD, sources.awaitItem())
                sources.cancel()
            }
        }

    @Test
    fun `primary stall past first-emission timeout falls back`() =
        runTest {
            turbineScope {
                val stalledPrimary =
                    object : RagLlmEngine {
                        override fun generate(
                            prompt: String,
                            instructions: String,
                        ): Flow<String> = generateStreaming(prompt, instructions)

                        override fun generateStreaming(
                            prompt: String,
                            instructions: String,
                        ): Flow<String> =
                            flow {
                                kotlinx.coroutines.awaitCancellation()
                            }
                    }
                val fallback = RecordingEngine(emissions = listOf("CLOUD"))
                val engine =
                    FmFirstRagLlmEngine(
                        primary = stalledPrimary,
                        fallback = fallback,
                        firstEmissionTimeout = 10.milliseconds,
                    )
                val sources = engine.sourceEvents.testIn(this)
                val chunks = engine.generateStreaming("prompt", "instructions").toList()
                assertEquals(listOf("CLOUD"), chunks)
                assertEquals(EngineSource.CLOUD, sources.awaitItem())
                sources.cancel()
            }
        }

    @Test
    fun `no fallback when cloud is disabled - original failure propagates`() =
        runTest {
            turbineScope {
                val primary = RecordingEngine(emissions = emptyList(), errorAfter = RuntimeException("fm down"))
                val fallback = RecordingEngine(emissions = listOf("CLOUD"))
                val engine =
                    FmFirstRagLlmEngine(
                        primary = primary,
                        fallback = fallback,
                        isFallbackEligible = { false },
                    )
                val error =
                    assertFailsWith<RuntimeException> {
                        engine.generateStreaming("prompt", "instructions").toList()
                    }
                assertEquals("fm down", error.message)
                assertEquals(0, fallback.calls)
            }
        }

    @Test
    fun `unavailable primary skips straight to fallback without calling it`() =
        runTest {
            turbineScope {
                val primary = RecordingEngine(emissions = listOf("SHOULD_NOT_APPEAR"))
                val fallback = RecordingEngine(emissions = listOf("CLOUD"))
                val engine =
                    FmFirstRagLlmEngine(
                        primary = primary,
                        fallback = fallback,
                        isPrimaryAvailable = { false },
                    )
                val sources = engine.sourceEvents.testIn(this)
                val chunks = engine.generateStreaming("prompt", "instructions").toList()
                assertEquals(listOf("CLOUD"), chunks)
                assertEquals(EngineSource.CLOUD, sources.awaitItem())
                sources.cancel()
                assertEquals(0, primary.calls)
            }
        }

    @Test
    fun `query policy is flexible only when cloud fallback is selected`() =
        runTest {
            val cloudSelected =
                FmFirstRagLlmEngine(
                    primary = RecordingEngine(emptyList()),
                    fallback = RecordingEngine(emptyList()),
                    isFallbackEligible = { true },
                    isPrimaryAvailable = { false },
                )
            assertEquals(RagQueryPolicy.CLOUD, cloudSelected.queryPolicy())

            val foundationModelsSelected =
                FmFirstRagLlmEngine(
                    primary = RecordingEngine(emptyList()),
                    fallback = RecordingEngine(emptyList()),
                    isFallbackEligible = { true },
                    isPrimaryAvailable = { true },
                )
            assertEquals(RagQueryPolicy.ON_DEVICE, foundationModelsSelected.queryPolicy())
        }
}
