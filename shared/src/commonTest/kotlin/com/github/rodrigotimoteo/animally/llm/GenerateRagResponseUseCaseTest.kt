package com.github.rodrigotimoteo.animally.llm

import com.github.rodrigotimoteo.animally.domain.search.ISearchRepository
import com.github.rodrigotimoteo.animally.domain.search.model.SearchResult
import com.github.rodrigotimoteo.animally.domain.search.usecase.SearchUseCase
import dev.mokkery.MockMode
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.matcher.any
import dev.mokkery.mock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Hand-rolled fake: LlmEngine is an expect class and cannot be faked from common code. */
private class FakeRagLlmEngine : RagLlmEngine {
    var calls: Int = 0
    var lastPrompt: String? = null
    var lastInstructions: String? = null

    /** When set, emitted instead of the default markdown-laden chunk (used by sanitizer cases). */
    var nextChunkOverride: String? = null

    override fun generate(
        prompt: String,
        instructions: String,
    ): Flow<String> =
        flow {
            calls++
            lastPrompt = prompt
            lastInstructions = instructions
            emit(
                nextChunkOverride
                    ?: "She is **pregnant** with a `due date` of __May 2025__. See [Vaccination #1](https://example.com/fake).",
            )
        }
}

class GenerateRagResponseUseCaseTest {
    private val searchRepositoryMock: ISearchRepository = mock(MockMode.autoUnit)
    private val engine = FakeRagLlmEngine()

    private fun sut(config: RagConfig = RagConfig.DEFAULT) =
        GenerateRagResponseUseCase(
            SearchUseCase(searchRepositoryMock),
            engine,
            config,
        )

    private fun result(snippet: String = "tetanus booster") =
        SearchResult(
            patientId = 7L,
            patientName = "Thunder",
            breed = "Thoroughbred",
            microchipId = null,
            recordType = "VACCINATION",
            recordId = 123L,
            date = LocalDate(2024, 5, 1),
            snippet = snippet,
        )

    @Test
    fun `given empty search results when invoked then fallback emitted and engine never called`() =
        runTest {
            every { searchRepositoryMock.search(any(), any(), any(), any()) } returns emptyList()

            val output = sut()(QUERY).toList()

            assertEquals(listOf(FALLBACK_TEXT), output)
            assertEquals(0, engine.calls)
        }

    @Test
    fun `given all chunks exceed budget when invoked then fallback emitted and engine never called`() =
        runTest {
            every { searchRepositoryMock.search(any(), any(), any(), any()) } returns listOf(result(snippet = "x".repeat(10_000)))
            val tinyBudget = RagConfig(maxContextTokens = 100)

            val output = sut(tinyBudget)(QUERY).toList()

            assertEquals(listOf(FALLBACK_TEXT), output)
            assertEquals(0, engine.calls)
        }

    @Test
    fun `given search results when invoked then engine called once with context containing chunk headers`() =
        runTest {
            every { searchRepositoryMock.search(any(), any(), any(), any()) } returns listOf(result())

            val output = sut()(QUERY).toList()

            assertEquals(1, engine.calls)
            assertTrue(engine.lastPrompt.orEmpty().contains("[VACCINATION #123] Thunder"), "context must carry the citable header")
            assertTrue(engine.lastPrompt.orEmpty().contains("Question: She is pregnant"))
            assertEquals(AssistantPrompts.SYSTEM_PROMPT, engine.lastInstructions)
            // Model output passes through sanitize(): no markdown survives.
            val text = output.single()
            assertTrue(text.contains("She is pregnant with a due date of May 2025. See Vaccination #1."))
            assertTrue("**" !in text && "`" !in text && "__" !in text && "http" !in text)
        }

    // --- sanitize() cases, exercised through the public flow ---

    @Test
    fun `sanitize removes bold marker pairs`() =
        runTest {
            every { searchRepositoryMock.search(any(), any(), any(), any()) } returns listOf(result())
            engine.nextChunkOverride = "**Answer** here"

            val output = sut()(QUERY).toList()

            assertEquals("Answer here", output.single())
        }

    @Test
    fun `sanitize converts markdown links to their text`() =
        runTest {
            every { searchRepositoryMock.search(any(), any(), any(), any()) } returns listOf(result())
            engine.nextChunkOverride = "See [Vaccination #123](https://vet.example.com/x) for details"

            val output = sut()(QUERY).toList()

            assertEquals("See Vaccination #123 for details", output.single())
        }

    @Test
    fun `sanitize leaves plain text untouched`() =
        runTest {
            every { searchRepositoryMock.search(any(), any(), any(), any()) } returns listOf(result())
            engine.nextChunkOverride = "- Plain line, 2 doses, due 2025-05-01."

            val output = sut()(QUERY).toList()

            assertEquals("- Plain line, 2 doses, due 2025-05-01.", output.single())
        }

    @Test
    fun `sanitize handles mixed markdown in one chunk`() =
        runTest {
            every { searchRepositoryMock.search(any(), any(), any(), any()) } returns listOf(result())
            engine.nextChunkOverride = "**Pregnant** — see [Ultrasound #9](https://x.co/y) and `notes` __here__"

            val output = sut()(QUERY).toList()

            assertEquals("Pregnant — see Ultrasound #9 and notes here", output.single())
        }

    private companion object {
        const val QUERY = "She is pregnant"
        const val FALLBACK_TEXT =
            "I couldn't find anything about that in your records. Try asking " +
                "about a horse by name, a treatment, vaccination, or a date."
    }
}
