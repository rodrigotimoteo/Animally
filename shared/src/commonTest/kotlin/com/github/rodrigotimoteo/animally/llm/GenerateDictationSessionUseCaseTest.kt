package com.github.rodrigotimoteo.animally.llm

import com.github.rodrigotimoteo.animally.domain.dictation.dto.DictatedSessionDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

private class FakeDictationExtractionEngine(
    private val response: String?,
) : RagLlmEngine {
    var prompt: String? = null
    var instructions: String? = null

    override fun generate(
        prompt: String,
        instructions: String,
    ): Flow<String> =
        flow {
            this@FakeDictationExtractionEngine.prompt = prompt
            this@FakeDictationExtractionEngine.instructions = instructions
            response?.let { emit(it) }
        }
}

class GenerateDictationSessionUseCaseTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `given prose wrapped fenced object then only session json is returned`() =
        runTest {
            val engine =
                FakeDictationExtractionEngine(
                    """
                    I found one explicit record:
                    ```json
                    {"records":[{"recordType":"weight","patientName":"Lua","date":"2026-08-26","weightKg":512.0}]}
                    ```
                    """.trimIndent(),
                )

            val result = GenerateDictationSessionUseCase(engine)("Lua weighed 512 kilos", "english")
            val session = json.decodeFromString<DictatedSessionDto>(result)

            assertEquals(1, session.records.size)
            assertEquals("weight", session.records.single().recordType)
            assertEquals("Lua", session.records.single().patientName)
            assertTrue(engine.prompt.orEmpty().contains("Lua weighed 512 kilos"))
            assertTrue(engine.instructions.orEmpty().contains("conservative veterinary dictation extractor"))
        }

    @Test
    fun `given bare record array then it is normalized to the shared envelope`() =
        runTest {
            val engine =
                FakeDictationExtractionEngine(
                    """
                    <think>I should add a plausible diagnosis.</think>
                    [{"recordType":"deworming","patientName":"Lua","drugName":"Ivermectin"}]
                    """.trimIndent(),
                )

            val result = GenerateDictationSessionUseCase(engine)("Lua received Ivermectin", "english")
            val session = json.decodeFromString<DictatedSessionDto>(result)

            assertEquals(1, session.records.size)
            assertEquals("deworming", session.records.single().recordType)
            assertEquals(null, session.records.single().notes)
        }

    @Test
    fun `given malformed model output then extraction fails instead of inventing records`() =
        runTest {
            val engine = FakeDictationExtractionEngine("The horse is probably pregnant.")

            val error =
                assertFailsWith<IllegalStateException> {
                    GenerateDictationSessionUseCase(engine)("Tell me about Lua", "english")
                }

            assertTrue(error.message.orEmpty().contains("invalid structured data"))
        }

    @Test
    fun `given no model output then extraction fails explicitly`() =
        runTest {
            val engine = FakeDictationExtractionEngine(null)

            val error =
                assertFailsWith<IllegalStateException> {
                    GenerateDictationSessionUseCase(engine)("Lua was seen today", "portuguese")
                }

            assertTrue(error.message.orEmpty().contains("returned no structured records"))
        }
}
