package com.github.rodrigotimoteo.animally.llm

import com.github.rodrigotimoteo.animally.domain.search.ISearchRepository
import com.github.rodrigotimoteo.animally.domain.search.model.SearchResult
import com.github.rodrigotimoteo.animally.domain.search.usecase.SearchUseCase
import dev.mokkery.MockMode
import dev.mokkery.answering.returns
import dev.mokkery.answering.sequentiallyReturns
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

    private fun sut(
        config: RagConfig = RagConfig.DEFAULT,
        strings: AssistantStrings = EnAssistantStrings,
        orSearch: RagOrSearch? =
            RagOrSearch { ftsQuery -> searchRepositoryMock.search(ftsQuery, null, null, null) },
    ) = GenerateRagResponseUseCase(
        SearchUseCase(searchRepositoryMock),
        engine,
        config,
        strings,
        orSearch,
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
            // Citation enforcement may append a final sources snapshot, so the
            // model text itself is the FIRST emission.
            val text = output.first()
            assertTrue(text.contains("She is pregnant with a due date of May 2025. See Vaccination #1."))
            assertTrue("**" !in text && "`" !in text && "__" !in text && "http" !in text)
        }

    // --- sanitize() cases, exercised through the public flow ---
    // The sanitized model text is the first emission; citation enforcement
    // may append a final snapshot with the retrieved source headers.

    @Test
    fun `sanitize removes bold marker pairs`() =
        runTest {
            every { searchRepositoryMock.search(any(), any(), any(), any()) } returns listOf(result())
            engine.nextChunkOverride = "**Answer** here"

            val output = sut()(QUERY).toList()

            assertEquals("Answer here", output.first())
        }

    @Test
    fun `sanitize converts markdown links to their text`() =
        runTest {
            every { searchRepositoryMock.search(any(), any(), any(), any()) } returns listOf(result())
            engine.nextChunkOverride = "See [Vaccination #123](https://vet.example.com/x) for details"

            val output = sut()(QUERY).toList()

            assertEquals("See Vaccination #123 for details", output.first())
        }

    @Test
    fun `sanitize leaves plain text untouched`() =
        runTest {
            every { searchRepositoryMock.search(any(), any(), any(), any()) } returns listOf(result())
            engine.nextChunkOverride = "- Plain line, 2 doses, due 2025-05-01."

            val output = sut()(QUERY).toList()

            assertEquals("- Plain line, 2 doses, due 2025-05-01.", output.first())
        }

    @Test
    fun `sanitize handles mixed markdown in one chunk`() =
        runTest {
            every { searchRepositoryMock.search(any(), any(), any(), any()) } returns listOf(result())
            engine.nextChunkOverride = "**Pregnant** — see [Ultrasound #9](https://x.co/y) and `notes` __here__"

            val output = sut()(QUERY).toList()

            assertEquals("Pregnant — see Ultrasound #9 and notes here", output.first())
        }

    @Test
    fun `given greeting question when invoked then greeting emitted and engine never called`() =
        runTest {
            every { searchRepositoryMock.search(any(), any(), any(), any()) } returns emptyList()

            val output = sut()("Hi").toList()

            assertEquals(AssistantPrompts.greetingReply("Hi"), output.single())
            assertEquals(0, engine.calls)
        }

    @Test
    fun `given AND query misses but OR retry matches when invoked then OR results feed the engine`() =
        runTest {
            // Call 1: the AND query from SearchUseCase ("patients* AND belong*
            // AND Daniela*"). Call 2: the FTS-safe OR expression hitting the
            // repository directly via RagOrSearch.
            every { searchRepositoryMock.search(any(), any(), any(), any()) } sequentiallyReturns
                listOf(
                    emptyList(),
                    listOf(result()),
                )

            val output = sut()("Which patients belong to Daniela").toList()

            assertEquals(1, engine.calls)
            assertTrue(engine.lastPrompt.orEmpty().contains("[VACCINATION #123] Thunder"))
            assertTrue(output.last().isNotEmpty())
        }

    @Test
    fun `given history when invoked then recent conversation block is in the prompt`() =
        runTest {
            every { searchRepositoryMock.search(any(), any(), any(), any()) } returns listOf(result())

            val history =
                listOf(
                    RagHistoryEntry("Tell me about Thunder", "Thunder is a 7 year old Thoroughbred."),
                    RagHistoryEntry("What vaccinations did he have?", "Tetanus booster on 2024-05-01."),
                )
            sut()(QUERY, history).toList()

            val prompt = engine.lastPrompt.orEmpty()
            assertTrue(prompt.contains("Recent conversation:"), "history block missing")
            assertTrue(prompt.contains("User: Tell me about Thunder"))
            assertTrue(prompt.contains("Assistant: Thunder is a 7 year old Thoroughbred."))
            assertTrue(prompt.indexOf("Recent conversation:") < prompt.indexOf("Context:"))
        }

    @Test
    fun `given long history when invoked then entries are capped and truncated`() =
        runTest {
            every { searchRepositoryMock.search(any(), any(), any(), any()) } returns listOf(result())

            val longAnswer = "y".repeat(500)
            val history =
                (1..5).map { turn -> RagHistoryEntry("question $turn " + "x".repeat(300), longAnswer) }
            sut()(QUERY, history).toList()

            val prompt = engine.lastPrompt.orEmpty()
            assertTrue(!prompt.contains("question 1 "), "oldest entries beyond cap must be dropped")
            assertTrue(prompt.contains("question 5"), "most recent entry must be kept")
            assertTrue(!prompt.contains(longAnswer), "answers must be truncated to 200 chars")
        }

    @Test
    fun `given empty retrieval but non-empty history when invoked then model still called with conversation context`() =
        runTest {
            every { searchRepositoryMock.search(any(), any(), any(), any()) } returns emptyList()

            val history = listOf(RagHistoryEntry("Tell me about Thunder", "Thunder is a 7 year old mare."))
            val output = sut()("How old is she?", history).toList()

            assertEquals(1, engine.calls, "follow-up must reach the model with conversation context")
            assertTrue(engine.lastPrompt.orEmpty().contains("Recent conversation:"))
            assertTrue(output.last().contains("pregnant")) // default fake chunk passes through sanitize
        }

    @Test
    fun `given PT strings when retrieval empty then PT fallback emitted`() =
        runTest {
            every { searchRepositoryMock.search(any(), any(), any(), any()) } returns emptyList()

            val output = sut(strings = PtAssistantStrings)(QUERY).toList()

            assertEquals(PtAssistantStrings.noResultsFallback, output.single())
            assertTrue(output.single().startsWith("Não encontrei"))
        }

    @Test
    fun `given PT question on EN device when retrieval empty then PT fallback emitted`() =
        runTest {
            every { searchRepositoryMock.search(any(), any(), any(), any()) } returns emptyList()

            val output = sut()("Quantos pacientes tenho?").toList()

            assertTrue(output.single().startsWith("Não encontrei"), "PT question must get a PT turn: ${output.single()}")
        }

    @Test
    fun `given records in context when prompted then citation is mandated`() =
        runTest {
            every { searchRepositoryMock.search(any(), any(), any(), any()) } returns listOf(result())

            sut()(QUERY).toList()

            val instructions = engine.lastInstructions.orEmpty()
            assertTrue(instructions.contains("MUST INCLUDE AT LEAST ONE BRACKETED HEADER"))
        }

    @Test
    fun `given model reply without citation when records were used then retrieved headers appended`() =
        runTest {
            every { searchRepositoryMock.search(any(), any(), any(), any()) } returns listOf(result())
            engine.nextChunkOverride = "Thunder is a horse."

            val output = sut()(QUERY).toList()

            val final = output.last()
            assertTrue(final.contains("[VACCINATION #123]"), "citation must be enforced: $final")
            assertTrue(final.contains("Thunder is a horse."))
        }

    @Test
    fun `given model reply already citing when records were used then no extra sources appended`() =
        runTest {
            every { searchRepositoryMock.search(any(), any(), any(), any()) } returns listOf(result())
            engine.nextChunkOverride = "Tetanus booster recorded in [VACCINATION #123] Thunder."

            val output = sut()(QUERY).toList()

            assertEquals(1, output.count { it.contains("[") }, "cited reply must not gain a duplicate source block")
        }

    private companion object {
        const val QUERY = "She is pregnant"
        const val FALLBACK_TEXT =
            "I couldn't find anything about that in your records. Try asking " +
                "about a horse by name, a treatment, vaccination, or a date."
    }
}
