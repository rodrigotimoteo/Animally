package com.github.rodrigotimoteo.animally.llm

import com.github.rodrigotimoteo.animally.domain.patient.IPatientRepository
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
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
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
    private val patientRepositoryMock: IPatientRepository = mock(MockMode.autoUnit)
    private val engine = FakeRagLlmEngine()

    private fun sut(
        config: RagConfig = RagConfig.DEFAULT,
        strings: AssistantStrings = EnAssistantStrings,
        recordSearch: RagRecordSearch? =
            RagRecordSearch { ftsQuery -> searchRepositoryMock.search(ftsQuery, null, null, null) },
        analysisContextBuilder: AnalysisContextBuilder? = null,
        today: LocalDate = LocalDate(2026, 8, 24),
        patientRepository: IPatientRepository? = null,
    ) = GenerateRagResponseUseCase(
        SearchUseCase(searchRepositoryMock),
        engine,
        config,
        strings,
        recordSearch,
        patientRepository = patientRepository,
        analysisContextBuilder = analysisContextBuilder,
        today = today,
    )

    private fun result(
        snippet: String = "tetanus booster",
        recordId: Long = 123L,
        patientName: String = "Thunder",
    ) = SearchResult(
        patientId = 7L,
        patientName = patientName,
        breed = "Thoroughbred",
        microchipId = null,
        recordType = "VACCINATION",
        recordId = recordId,
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
    fun `given empty search results when invoked then placeholder then fallback emitted and engine never called`() =
        runTest {
            every { searchRepositoryMock.search(any(), any(), any(), any()) } returns emptyList()

            val output = sut()(QUERY).chunks()

            assertEquals(listOf(PLACEHOLDER, FALLBACK_TEXT), output)
            assertEquals(0, engine.calls)
        }

    @Test
    fun `given all chunks exceed budget when invoked then fallback emitted and engine never called`() =
        runTest {
            every { searchRepositoryMock.search(any(), any(), any(), any()) } returns listOf(result(snippet = "x".repeat(10_000)))
            val tinyBudget = RagConfig(maxContextTokens = 100)

            val output = sut(tinyBudget)(QUERY).chunks()

            assertEquals(listOf(PLACEHOLDER, FALLBACK_TEXT), output)
            assertEquals(0, engine.calls)
        }

    @Test
    fun `given oversized first chunk when invoked then smaller later chunks still selected`() =
        runTest {
            // Regression: selectWithinBudget used to break on the first chunk
            // that did not fit, starving every smaller relevant record behind
            // it. The oversized lead must be skipped (and capped), not a
            // stopping point.
            val oversized = result().copy(recordId = 1L, snippet = "y".repeat(10_000))
            val smallA = result().copy(recordId = 2L, snippet = "colic treated")
            val smallB = result().copy(recordId = 3L, snippet = "hoof abscess")
            every { searchRepositoryMock.search(any(), any(), any(), any()) } returns listOf(oversized, smallA, smallB)
            // Budget sits between the capped oversized chunk (~300 tokens) and
            // its raw size (~2500 tokens): with the old break nothing survives.
            val config = RagConfig(maxContextTokens = 3000)

            val output = sut(config)(QUERY).answers()

            assertEquals(1, engine.calls, "small chunks after the oversized one must unlock the model call")
            val prompt = engine.lastPrompt.orEmpty()
            assertTrue(prompt.contains("[VACCINATION #2]"), "small chunk A missing from context")
            assertTrue(prompt.contains("[VACCINATION #3]"), "small chunk B missing from context")
            assertTrue(prompt.contains("[VACCINATION #1]"), "oversized chunk is capped, not dropped")
            assertFalse(
                prompt.contains("y".repeat(RagConfig.DEFAULT.chunkCharCap + 1)),
                "oversized snippet must be truncated to the char cap",
            )
            assertTrue(output.first().contains("pregnant"))
        }

    @Test
    fun `given snippet longer than char cap when invoked then prompt carries capped snippet`() =
        runTest {
            every { searchRepositoryMock.search(any(), any(), any(), any()) } returns listOf(result(snippet = "z".repeat(5000)))

            sut()(QUERY).toList()

            val prompt = engine.lastPrompt.orEmpty()
            assertTrue(prompt.contains("zzzzzzzzzz"), "capped snippet content must survive")
            assertFalse(prompt.contains("z".repeat(RagConfig.DEFAULT.chunkCharCap + 1)), "snippet must be truncated to chunkCharCap")
        }

    @Test
    fun `given any query when invoked then prompt leads with today line and humanized chunk date`() =
        runTest {
            // "Is the Coggins still valid?" is unanswerable unless the model
            // knows today's date; raw ISO dates in chunks read as noise.
            every { searchRepositoryMock.search(any(), any(), any(), any()) } returns listOf(result())

            sut()(QUERY).toList()

            val prompt = engine.lastPrompt.orEmpty()
            assertTrue(prompt.startsWith("TODAY IS 24 Aug 2026."), "today line must lead the user turn: ${prompt.take(60)}")
            assertTrue(prompt.contains("(Thoroughbred, 1 May 2024)"), "chunk date must be humanized: $prompt")
            assertFalse(prompt.contains("2024-05-01"), "raw ISO dates must not leak into the context")
            // Today line lives in the user turn, not the system prompt, so the
            // reserve budget stays stable.
            assertEquals(AssistantPrompts.SYSTEM_PROMPT, engine.lastInstructions)
        }

    @Test
    fun `given retrieval turn when invoked then searching placeholder leads and is replaced`() =
        runTest {
            every { searchRepositoryMock.search(any(), any(), any(), any()) } returns listOf(result())

            val output = sut()(QUERY).chunks()

            assertEquals(PLACEHOLDER, output.first(), "placeholder must give immediate feedback")
            assertTrue(output.size > 1, "real answer must replace the placeholder (buffer semantics)")
            assertTrue(output.drop(1).none { it == PLACEHOLDER })
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
            // The searching placeholder leads the stream; the model text is
            // the first answer chunk (citation enforcement may append a final
            // sources snapshot after it).
            val text = events.map { (it as? RagStreamEvent.Chunk)?.text }.filterNotNull().first { it != PLACEHOLDER }
            assertTrue(text.contains("She is pregnant with a due date of May 2025. See Vaccination #1."))
            assertTrue("**" !in text && "`" !in text && "__" !in text && "http" !in text)
        }

    // --- sanitize() cases, exercised through the public flow ---
    // The sanitized model text is the first answer chunk (after the searching
    // placeholder); citation enforcement may append a final snapshot with the
    // retrieved source headers.

    @Test
    fun `sanitize removes bold marker pairs`() =
        runTest {
            every { searchRepositoryMock.search(any(), any(), any(), any()) } returns listOf(result())
            engine.nextChunkOverride = "**Answer** here"

            val output = sut()(QUERY).answers()

            assertEquals("Answer here", output.first())
        }

    @Test
    fun `sanitize converts markdown links to their text`() =
        runTest {
            every { searchRepositoryMock.search(any(), any(), any(), any()) } returns listOf(result())
            engine.nextChunkOverride = "See [Vaccination #123](https://vet.example.com/x) for details"

            val output = sut()(QUERY).answers()

            assertEquals("See Vaccination #123 for details", output.first())
        }

    @Test
    fun `sanitize leaves plain text untouched`() =
        runTest {
            every { searchRepositoryMock.search(any(), any(), any(), any()) } returns listOf(result())
            engine.nextChunkOverride = "- Plain line, 2 doses, due 2025-05-01."

            val output = sut()(QUERY).answers()

            assertEquals("- Plain line, 2 doses, due 2025-05-01.", output.first())
        }

    @Test
    fun `sanitize handles mixed markdown in one chunk`() =
        runTest {
            every { searchRepositoryMock.search(any(), any(), any(), any()) } returns listOf(result())
            engine.nextChunkOverride = "**Pregnant** — see [Ultrasound #9](https://x.co/y) and `notes` __here__"

            val output = sut()(QUERY).answers()

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

            val output = sut()("Which patients belong to Daniela").answers()

            assertEquals(1, engine.calls)
            assertTrue(engine.lastPrompt.orEmpty().contains("[VACCINATION #123] Thunder"))
            assertTrue(output.last().isNotEmpty())
        }

    @Test
    fun `given weak AND leg when OR retry recovers records then patient scoping ranks them first`() =
        runTest {
            // Call 1: the AND query - ONE weak hit, on the WRONG patient
            // (pre-threshold behavior would have returned it unscooped).
            // Call 2: the OR retry recovering both patients' records.
            val andLegComet = result(recordId = 201, patientName = "Comet")
            val retryBella = result(recordId = 202, patientName = "Bella")
            val retryComet = result(recordId = 203, patientName = "Comet")
            every { searchRepositoryMock.search(any(), any(), any(), any()) } sequentiallyReturns
                listOf(
                    listOf(andLegComet),
                    listOf(retryBella, retryComet),
                )
            every { patientRepositoryMock.patientNames() } returns listOf("Bella", "Comet")

            sut(patientRepository = patientRepositoryMock)("colic Bella").answers()

            val prompt = engine.lastPrompt.orEmpty()
            assertTrue(prompt.contains("[VACCINATION #202] Bella"))
            val bellaIndex = prompt.indexOf("[VACCINATION #202] Bella")
            val firstCometIndex = prompt.indexOf("[VACCINATION #201] Comet")
            assertTrue(
                bellaIndex in 0 until firstCometIndex,
                "scoped-patient record recovered by the OR retry must rank ahead of the weak AND-leg hit",
            )
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
            val output = sut()("How old is she?", history).answers()

            assertEquals(1, engine.calls, "follow-up must reach the model with conversation context")
            assertTrue(engine.lastPrompt.orEmpty().contains("Recent conversation:"))
            assertTrue(output.last().contains("pregnant")) // default fake chunk passes through sanitize
        }

    @Test
    fun `given PT strings when retrieval empty then PT fallback emitted`() =
        runTest {
            every { searchRepositoryMock.search(any(), any(), any(), any()) } returns emptyList()

            val output = sut(strings = PtAssistantStrings)(QUERY).chunks()

            assertEquals(
                listOf(PtAssistantStrings.searchingPlaceholder, PtAssistantStrings.noResultsFallback),
                output,
            )
            assertTrue(output.last().startsWith("Não encontrei"))
        }

    @Test
    fun `given PT question on EN device when retrieval empty then PT fallback emitted`() =
        runTest {
            every { searchRepositoryMock.search(any(), any(), any(), any()) } returns emptyList()

            val output = sut()("Quantos pacientes tenho?").chunks()

            assertTrue(output.last().startsWith("Não encontrei"), "PT question must get a PT turn: ${output.last()}")
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

            val output = sut()(QUERY).answers()

            val final = output.last()
            assertTrue(final.contains("[VACCINATION #123]"), "citation must be enforced: $final")
            assertTrue(final.contains("Thunder is a horse."))
        }

    @Test
    fun `given many selected records and no model citations when enforced then exactly top-3 headers appended`() =
        runTest {
            // Regression: enforcement used to append EVERY selected header -
            // a ten-record answer gained ten noise lines. Cap is top-3 by rank.
            val five =
                (1L..5L).map { id -> result().copy(recordId = id, snippet = "note $id") }
            every { searchRepositoryMock.search(any(), any(), any(), any()) } returns five
            engine.nextChunkOverride = "Thunder is a horse."

            val output = sut()(QUERY).answers()

            val final = output.last()
            val headerMatches = Regex("\\[[A-Z_]+ #\\d+]").findAll(final).toList()
            assertEquals(3, headerMatches.size, "exactly three headers must be appended: $final")
            assertEquals(
                listOf("[VACCINATION #1]", "[VACCINATION #2]", "[VACCINATION #3]"),
                headerMatches.map { it.value },
                "appended headers must be the top-3 by retrieval rank",
            )
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
    fun `given fabricated citation when not in context then real headers enforced and only real source emitted`() =
        runTest {
            // A fabricated header ([GESTATION #999] - wrong type and id)
            // satisfies the eye but maps to no source card, which left
            // completed answers without a Sources event (dead follow-up
            // chips). Enforcement now keys on MAPPED citations: the real
            // retrieved headers are appended, and only the real record is
            // carded - never the fabricated one.
            every { searchRepositoryMock.search(any(), any(), any(), any()) } returns listOf(result())
            engine.nextChunkOverride = "See [GESTATION #999] Ghost for details."

            val events = sut()(QUERY).toList()

            val final = events.filterIsInstance<RagStreamEvent.Chunk>().last().text
            assertTrue(final.contains("[VACCINATION #123]"), "real header must be enforced: $final")
            val sources = events.filterIsInstance<RagStreamEvent.Sources>().single()
            assertEquals(
                listOf("VACCINATION#123"),
                sources.sources.map { "${it.recordType}#${it.recordId}" },
                "only the retrieved record may become a source card",
            )
        }

    @Test
    fun `given possessive farrier query when AND leg misses and retry recovers record then header appended and sources emitted`() =
        runTest {
            // Regression shape for the farrier UI test: the possessive cleans
            // to "Thunders", the strict AND leg misses, and the weak-retry OR
            // leg recovers the farrier visit. The citation-less model reply
            // must still gain the retrieved header and a Sources event.
            val farrier =
                result().copy(recordType = "FARRIER_VISIT", recordId = 301L, snippet = "Full set steel shoes")
            every { searchRepositoryMock.search(any(), any(), any(), any()) } sequentiallyReturns
                listOf(emptyList(), listOf(farrier))
            engine.nextChunkOverride = "Thunder's last farrier visit was 24 Aug 2026."

            val events = sut()("When was Thunder's last farrier visit?").toList()

            val final = events.filterIsInstance<RagStreamEvent.Chunk>().last().text
            assertTrue(final.contains("[FARRIER_VISIT #301]"), "citation must be enforced: $final")
            val sources = events.filterIsInstance<RagStreamEvent.Sources>().single()
            assertEquals("FARRIER_VISIT", sources.sources.single().recordType)
        }

    @Test
    fun `given zero retrieval and care summary when model answers uncited then Summary citation appended`() =
        runTest {
            // Regression: fix-18's deterministic summary answers recency
            // questions even when retrieval is empty (sparse-indexed rows).
            // With no selected records the record-header enforcement never
            // fired, so the reply shipped with no citation at all and no
            // Sources event. The summary path must enforce [Summary] too.
            every { searchRepositoryMock.search(any(), any(), any(), any()) } returns emptyList()
            val repos = FakeAnalysisRepos()
            repos.patients.patients = listOf(testPatient(1, "Thunder"))
            repos.farrierVisits.entries =
                listOf(testFarrierVisit(id = 301, patientId = 1, date = LocalDate(2026, 8, 24)))
            engine.nextChunkOverride = "Thunder's last farrier visit was 2026-08-24."

            val events =
                sut(analysisContextBuilder = repos.builder)("When was Thunder's last farrier visit?").toList()

            val final = events.filterIsInstance<RagStreamEvent.Chunk>().last().text
            assertTrue(final.contains("[Summary]"), "summary-only answers must carry a citation: $final")
            assertTrue(
                events.filterIsInstance<RagStreamEvent.Sources>().isEmpty(),
                "no record was retrieved - no source cards may be invented",
            )
        }

    @Test
    fun `given care summary when built then last-done dates are humanized not ISO`() {
        val repos = FakeAnalysisRepos()
        repos.patients.patients = listOf(testPatient(1, "Thunder"))
        repos.farrierVisits.entries =
            listOf(testFarrierVisit(id = 301, patientId = 1, date = LocalDate(2026, 8, 24)))

        val summary = repos.builder.build("When was Thunder's last farrier visit?")

        assertNotNull(summary)
        assertTrue(summary.contains("last 24 Aug 2026"), "ISO date leaked into summary: $summary")
        assertFalse(summary.contains("2026-08-24"), "raw ISO date must not reach the prompt: $summary")
    }

    // --- Dosage guardrail: deterministic refusal, no model call ---

    @Test
    fun `given dosage question and no medication records when invoked then refusal emitted and engine never called`() =
        runTest {
            every { searchRepositoryMock.search(any(), any(), any(), any()) } returns listOf(result())

            val output = sut()("How much vaccine should I administer?").chunks()

            assertEquals(listOf(PLACEHOLDER, EnAssistantStrings.dosageRefusal), output)
            assertEquals(0, engine.calls, "dosage refusal must be deterministic - no model call")
        }

    @Test
    fun `given dosage question with medication record retrieved when invoked then normal grounded answer`() =
        runTest {
            every { searchRepositoryMock.search(any(), any(), any(), any()) } returns listOf(medicationResult())

            val output = sut()("How much metronidazole was given?").answers()

            assertEquals(1, engine.calls, "grounded dosage question must reach the model")
            assertTrue(output.first().contains("pregnant")) // default fake chunk passes through sanitize
        }

    @Test
    fun `given weight phrasing when invoked then guardrail does not fire`() =
        runTest {
            every { searchRepositoryMock.search(any(), any(), any(), any()) } returns listOf(result())

            val output = sut()("How much does she weigh?").answers()

            assertEquals(1, engine.calls, "weight questions are not dosage intents")
            assertTrue(!output.first().contains("dosages"))
        }

    @Test
    fun `given PT dosage question without medication records when invoked then PT refusal emitted`() =
        runTest {
            every { searchRepositoryMock.search(any(), any(), any(), any()) } returns emptyList()

            val output = sut()("Quantos ml de detomidine administrar?").chunks()

            assertEquals(PtAssistantStrings.dosageRefusal, output.last())
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

    // --- Analysis mode: deterministic summaries reach the prompt ---

    @Test
    fun `given analysis query when invoked then deterministic summary reaches prompt and engine called`() =
        runTest {
            every { searchRepositoryMock.search(any(), any(), any(), any()) } returns emptyList()
            val repos = FakeAnalysisRepos()
            repos.patients.patients = listOf(testPatient(1, "Thunder"))

            val output = sut(analysisContextBuilder = repos.builder)("How many patients do I have?").answers()

            assertEquals(1, engine.calls, "summary alone must unlock the model call - unlike the fallback paths")
            val prompt = engine.lastPrompt.orEmpty()
            assertTrue(prompt.contains(AnalysisContextBuilder.SUMMARY_HEADER))
            assertTrue(prompt.contains("PATIENT CENSUS: 1 active patients: Thunder."))
            assertTrue(prompt.indexOf("DETERMINISTIC SUMMARY") < prompt.indexOf("Context:"), "summary precedes Context")
            assertTrue(engine.lastInstructions.orEmpty().contains("DETERMINISTIC SUMMARY LINES ARE COMPUTED FACTS"))
            assertTrue(output.first().contains("pregnant")) // default fake chunk passes through sanitize
        }

    @Test
    fun `given tiny budget when invoked then summary kept and chunks dropped`() =
        runTest {
            every { searchRepositoryMock.search(any(), any(), any(), any()) } returns listOf(result(snippet = "x".repeat(4000)))
            val repos = FakeAnalysisRepos()
            repos.patients.patients = listOf(testPatient(1, "Thunder"))
            val config = RagConfig(maxContextTokens = 1200)

            sut(config, analysisContextBuilder = repos.builder)("How many patients do I have?").chunks()

            assertEquals(1, engine.calls)
            val prompt = engine.lastPrompt.orEmpty()
            assertTrue(prompt.contains(AnalysisContextBuilder.SUMMARY_HEADER), "computed summary must survive the budget")
            assertFalse(prompt.contains("[VACCINATION #123]"), "chunk must lose to the reserved summary budget")
        }

    @Test
    fun `given builder wired and non-analysis query when invoked then prompt has no summary`() =
        runTest {
            every { searchRepositoryMock.search(any(), any(), any(), any()) } returns listOf(result())
            val repos = FakeAnalysisRepos()
            repos.patients.patients = listOf(testPatient(1, "Thunder"))

            sut(analysisContextBuilder = repos.builder)("What treatment did Thunder receive for colic?").chunks()

            assertEquals(1, engine.calls)
            assertFalse(engine.lastPrompt.orEmpty().contains("DETERMINISTIC SUMMARY"), "retrieval-only turns skip the scan")
        }

    private companion object {
        const val QUERY = "She is pregnant"
        const val FALLBACK_TEXT =
            "I couldn't find anything about that in your records. Try asking " +
                "about a horse by name, a treatment, vaccination, or a date."
        const val PLACEHOLDER = "Searching your records…"
    }
}

/** Chunk texts in emission order - the user-visible answer snapshots. */
private suspend fun Flow<RagStreamEvent>.chunks(): List<String> = toList().filterIsInstance<RagStreamEvent.Chunk>().map { it.text }

/**
 * Answer snapshots excluding the leading searching placeholder (emitted
 * before retrieval and replaced by consumers' buffer semantics).
 */
private suspend fun Flow<RagStreamEvent>.answers(): List<String> = chunks().filter { it != EnAssistantStrings.searchingPlaceholder }
