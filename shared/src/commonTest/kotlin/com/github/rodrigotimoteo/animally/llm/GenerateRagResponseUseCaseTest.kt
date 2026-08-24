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
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/** Hand-rolled fake: LlmEngine is an expect class and cannot be faked from common code. */
private class FakeRagLlmEngine : RagLlmEngine {
    var calls: Int = 0
    var lastPrompt: String? = null
    var lastInstructions: String? = null

    /** When set, emitted instead of the default markdown-laden chunk (used by sanitizer cases). */
    var nextChunkOverride: String? = null

    /** When set, the streaming flow emits its normal chunk then fails with this error. */
    var streamingError: Throwable? = null

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
            streamingError?.let { throw it }
        }
}

class GenerateRagResponseUseCaseTest {
    private val searchRepositoryMock: ISearchRepository = mock(MockMode.autoUnit)
    private val engine = FakeRagLlmEngine()

    private fun sut(
        config: RagConfig = RagConfig.DEFAULT,
        strings: AssistantStrings = EnAssistantStrings,
        recordSearch: RagRecordSearch? =
            RagRecordSearch { ftsQuery -> searchRepositoryMock.search(ftsQuery, null, null, null) },
    ) = GenerateRagResponseUseCase(
        SearchUseCase(searchRepositoryMock),
        engine,
        config,
        strings,
        recordSearch,
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

    private fun medicationResult() =
        SearchResult(
            patientId = 7L,
            patientName = "Thunder",
            breed = "Thoroughbred",
            microchipId = null,
            recordType = "MEDICATION",
            recordId = 555L,
            date = LocalDate(2024, 6, 1),
            snippet = "Metronidazole 500 mg twice daily",
        )

    @Test
    fun `given empty search results when invoked then fallback emitted and engine never called`() =
        runTest {
            every { searchRepositoryMock.search(any(), any(), any(), any()) } returns emptyList()

            val output = sut()(QUERY).chunks()

            assertEquals(listOf(FALLBACK_TEXT), output)
            assertEquals(0, engine.calls)
        }

    @Test
    fun `given all chunks exceed budget when invoked then fallback emitted and engine never called`() =
        runTest {
            every { searchRepositoryMock.search(any(), any(), any(), any()) } returns listOf(result(snippet = "x".repeat(10_000)))
            val tinyBudget = RagConfig(maxContextTokens = 100)

            val output = sut(tinyBudget)(QUERY).chunks()

            assertEquals(listOf(FALLBACK_TEXT), output)
            assertEquals(0, engine.calls)
        }

    @Test
    fun `given search results when invoked then engine called once with context containing chunk headers`() =
        runTest {
            every { searchRepositoryMock.search(any(), any(), any(), any()) } returns listOf(result())

            val events = sut()(QUERY).toList()

            assertEquals(1, engine.calls)
            assertTrue(engine.lastPrompt.orEmpty().contains("[VACCINATION #123] Thunder"), "context must carry the citable header")
            assertTrue(engine.lastPrompt.orEmpty().contains("Question: She is pregnant"))
            assertEquals(AssistantPrompts.SYSTEM_PROMPT, engine.lastInstructions)
            // Model output passes through sanitize(): no markdown survives.
            // Citation enforcement may append a final sources snapshot, so the
            // model text itself is the FIRST chunk.
            val text = events.filterIsInstance<RagStreamEvent.Chunk>().first().text
            assertTrue(text.contains("She is pregnant with a due date of May 2025. See Vaccination #1."))
            assertTrue("**" !in text && "`" !in text && "__" !in text && "http" !in text)
        }

    // --- sanitize() cases, exercised through the public flow ---
    // The sanitized model text is the first chunk; citation enforcement
    // may append a final snapshot with the retrieved source headers.

    @Test
    fun `sanitize removes bold marker pairs`() =
        runTest {
            every { searchRepositoryMock.search(any(), any(), any(), any()) } returns listOf(result())
            engine.nextChunkOverride = "**Answer** here"

            val output = sut()(QUERY).chunks()

            assertEquals("Answer here", output.first())
        }

    @Test
    fun `sanitize converts markdown links to their text`() =
        runTest {
            every { searchRepositoryMock.search(any(), any(), any(), any()) } returns listOf(result())
            engine.nextChunkOverride = "See [Vaccination #123](https://vet.example.com/x) for details"

            val output = sut()(QUERY).chunks()

            assertEquals("See Vaccination #123 for details", output.first())
        }

    @Test
    fun `sanitize leaves plain text untouched`() =
        runTest {
            every { searchRepositoryMock.search(any(), any(), any(), any()) } returns listOf(result())
            engine.nextChunkOverride = "- Plain line, 2 doses, due 2025-05-01."

            val output = sut()(QUERY).chunks()

            assertEquals("- Plain line, 2 doses, due 2025-05-01.", output.first())
        }

    @Test
    fun `sanitize handles mixed markdown in one chunk`() =
        runTest {
            every { searchRepositoryMock.search(any(), any(), any(), any()) } returns listOf(result())
            engine.nextChunkOverride = "**Pregnant** — see [Ultrasound #9](https://x.co/y) and `notes` __here__"

            val output = sut()(QUERY).chunks()

            assertEquals("Pregnant — see Ultrasound #9 and notes here", output.first())
        }

    @Test
    fun `given greeting question when invoked then greeting emitted and engine never called`() =
        runTest {
            every { searchRepositoryMock.search(any(), any(), any(), any()) } returns emptyList()

            val output = sut()("Hi").chunks()

            assertEquals(AssistantPrompts.greetingReply("Hi"), output.single())
            assertEquals(0, engine.calls)
        }

    @Test
    fun `given AND query misses but OR retry matches when invoked then OR results feed the engine`() =
        runTest {
            // Call 1: the AND query built from the enriched question. Call 2:
            // the FTS-safe OR expression hitting the repository directly via
            // RagRecordSearch.
            every { searchRepositoryMock.search(any(), any(), any(), any()) } sequentiallyReturns
                listOf(
                    emptyList(),
                    listOf(result()),
                )

            val output = sut()("Which patients belong to Daniela").chunks()

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
            sut()(QUERY, history).chunks()

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
            val output = sut()("How old is she?", history).chunks()

            assertEquals(1, engine.calls, "follow-up must reach the model with conversation context")
            assertTrue(engine.lastPrompt.orEmpty().contains("Recent conversation:"))
            assertTrue(output.last().contains("pregnant")) // default fake chunk passes through sanitize
        }

    @Test
    fun `given PT strings when retrieval empty then PT fallback emitted`() =
        runTest {
            every { searchRepositoryMock.search(any(), any(), any(), any()) } returns emptyList()

            val output = sut(strings = PtAssistantStrings)(QUERY).chunks()

            assertEquals(PtAssistantStrings.noResultsFallback, output.single())
            assertTrue(output.single().startsWith("Não encontrei"))
        }

    @Test
    fun `given PT question on EN device when retrieval empty then PT fallback emitted`() =
        runTest {
            every { searchRepositoryMock.search(any(), any(), any(), any()) } returns emptyList()

            val output = sut()("Quantos pacientes tenho?").chunks()

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

            val output = sut()(QUERY).chunks()

            val final = output.last()
            assertTrue(final.contains("[VACCINATION #123]"), "citation must be enforced: $final")
            assertTrue(final.contains("Thunder is a horse."))
        }

    @Test
    fun `given model reply already citing when records were used then no extra sources appended`() =
        runTest {
            every { searchRepositoryMock.search(any(), any(), any(), any()) } returns listOf(result())
            engine.nextChunkOverride = "Tetanus booster recorded in [VACCINATION #123] Thunder."

            val output = sut()(QUERY).chunks()

            val citedChunks = output.count { it.contains("[") }
            assertEquals(1, citedChunks, "cited reply must not gain a duplicate source block")
        }

    // --- Sources event: cited records exposed for source-card chips ---

    @Test
    fun `given cited reply when completed then sources event carries the cited record`() =
        runTest {
            every { searchRepositoryMock.search(any(), any(), any(), any()) } returns listOf(result())
            engine.nextChunkOverride = "Tetanus booster recorded in [VACCINATION #123] Thunder."

            val events = sut()(QUERY).toList()

            val sources = events.filterIsInstance<RagStreamEvent.Sources>().single()
            assertEquals(listOf("VACCINATION#123"), sources.sources.map { "${it.recordType}#${it.recordId}" })
        }

    @Test
    fun `given uncited reply when enforcement appends headers then sources event still emitted`() =
        runTest {
            every { searchRepositoryMock.search(any(), any(), any(), any()) } returns listOf(result())
            engine.nextChunkOverride = "Thunder is a horse."

            val events = sut()(QUERY).toList()

            // Enforcement appends "[VACCINATION #123] ..." so the citation IS
            // present in the final text and maps back to the retrieved record.
            val sources = events.filterIsInstance<RagStreamEvent.Sources>().single()
            assertEquals("VACCINATION", sources.sources.single().recordType)
        }

    @Test
    fun `given fabricated citation when not in context then no source emitted for it`() =
        runTest {
            every { searchRepositoryMock.search(any(), any(), any(), any()) } returns listOf(result())
            engine.nextChunkOverride = "See [GESTATION #999] Ghost for details."

            val events = sut()(QUERY).toList()

            assertTrue(
                events.filterIsInstance<RagStreamEvent.Sources>().isEmpty(),
                "citations outside the selected context must not become source cards",
            )
        }

    // --- Dosage guardrail: deterministic refusal, no model call ---

    @Test
    fun `given dosage question and no medication records when invoked then refusal emitted and engine never called`() =
        runTest {
            every { searchRepositoryMock.search(any(), any(), any(), any()) } returns listOf(result())

            val output = sut()("How much vaccine should I administer?").chunks()

            assertEquals(listOf(EnAssistantStrings.dosageRefusal), output)
            assertEquals(0, engine.calls, "dosage refusal must be deterministic - no model call")
        }

    @Test
    fun `given dosage question with medication record retrieved when invoked then normal grounded answer`() =
        runTest {
            every { searchRepositoryMock.search(any(), any(), any(), any()) } returns listOf(medicationResult())

            val output = sut()("How much metronidazole was given?").chunks()

            assertEquals(1, engine.calls, "grounded dosage question must reach the model")
            assertTrue(output.first().contains("pregnant")) // default fake chunk passes through sanitize
        }

    @Test
    fun `given weight phrasing when invoked then guardrail does not fire`() =
        runTest {
            every { searchRepositoryMock.search(any(), any(), any(), any()) } returns listOf(result())

            val output = sut()("How much does she weigh?").chunks()

            assertEquals(1, engine.calls, "weight questions are not dosage intents")
            assertTrue(!output.first().contains("dosages"))
        }

    @Test
    fun `given PT dosage question without medication records when invoked then PT refusal emitted`() =
        runTest {
            every { searchRepositoryMock.search(any(), any(), any(), any()) } returns emptyList()

            val output = sut()("Quantos ml de detomidine administrar?").chunks()

            assertEquals(PtAssistantStrings.dosageRefusal, output.single())
            assertEquals(0, engine.calls)
        }

    // --- Stream interruption: typed marker preserving partial text ---

    @Test
    fun `given engine failure mid-stream when invoked then interrupted event carries partial text`() =
        runTest {
            every { searchRepositoryMock.search(any(), any(), any(), any()) } returns listOf(result())
            engine.nextChunkOverride = "Partial answer so far"
            engine.streamingError = RuntimeException("engine exploded")

            val events = sut()(QUERY).toList()

            val interrupted = events.filterIsInstance<RagStreamEvent.Interrupted>().single()
            assertEquals("Partial answer so far", interrupted.partialText)
            assertEquals("engine exploded", interrupted.error)
            assertTrue(events.none { it is RagStreamEvent.Sources }, "interrupted turn has no completed citations")
        }

    @Test
    fun `given cancellation mid-stream when invoked then exception propagates`() =
        runTest {
            every { searchRepositoryMock.search(any(), any(), any(), any()) } returns listOf(result())
            engine.streamingError = kotlinx.coroutines.CancellationException("user cancelled")

            assertFailsWith<kotlinx.coroutines.CancellationException> {
                sut()(QUERY).toList()
            }
        }

    private companion object {
        const val QUERY = "She is pregnant"
        const val FALLBACK_TEXT =
            "I couldn't find anything about that in your records. Try asking " +
                "about a horse by name, a treatment, vaccination, or a date."
    }
}

/** Chunk texts in emission order - the user-visible answer snapshots. */
private suspend fun Flow<RagStreamEvent>.chunks(): List<String> = toList().filterIsInstance<RagStreamEvent.Chunk>().map { it.text }
