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
import dev.mokkery.verify
import dev.mokkery.verify.VerifyMode
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

private class DateAwareRagRecordSearch(
    private val rows: List<SearchResult>,
) : RagRecordSearch {
    var requestedRange: RagDateRange? = null

    override fun search(ftsQuery: String): List<SearchResult> = emptyList()

    override fun searchByDateRange(
        from: LocalDate,
        to: LocalDate,
    ): List<SearchResult> {
        requestedRange = RagDateRange(from, to)
        return rows
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
        queryPolicyProvider: suspend () -> RagQueryPolicy = { RagQueryPolicy.ON_DEVICE },
    ) = GenerateRagResponseUseCase(
        SearchUseCase(searchRepositoryMock),
        engine,
        config,
        strings,
        recordSearch,
        patientRepository = patientRepository,
        analysisContextBuilder = analysisContextBuilder,
        today = today,
        queryPolicyProvider = queryPolicyProvider,
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
    fun `given empty retrieval no summary and no history when invoked then honest fallback and zero engine calls`() =
        runTest {
            // Farrier regression guard: with NOTHING to ground on the model
            // must never be consulted - it freeballs plausible-sounding dates
            // ("last farrier visit was on 24 Aug") that neither citation
            // enforcement nor the honest fallback can catch.
            every { searchRepositoryMock.search(any(), any(), any(), any()) } returns emptyList()

            val output = sut()("When was Thunder's last farrier visit?").chunks()

            assertEquals(0, engine.calls, "engine must never run on empty grounding")
            assertEquals(listOf(PLACEHOLDER, FALLBACK_TEXT), output)
        }

    @Test
    fun `given cloud policy and empty retrieval when invoked then general question reaches model`() =
        runTest {
            every { searchRepositoryMock.search(any(), any(), any(), any()) } returns emptyList()

            val output =
                sut(queryPolicyProvider = { RagQueryPolicy.CLOUD })("Why is the sky blue?").answers()

            assertEquals(1, engine.calls, "cloud general questions must not hit the record-only fallback")
            assertTrue(output.first().contains("pregnant"))
            assertTrue(engine.lastInstructions.orEmpty().contains("general, educational, or casual questions"))
            assertTrue(!engine.lastInstructions.orEmpty().contains("ANSWER ONLY FROM THE CONTEXT BELOW"))
        }

    @Test
    fun `given current month activity question then only dated rows in current month are returned deterministically`() =
        runTest {
            val inMonth = result(recordId = 1L, snippet = "pregnancy confirmed").copy(date = LocalDate(2026, 8, 3))
            val today = result(recordId = 2L, snippet = "weight recorded").copy(date = LocalDate(2026, 8, 24))
            val outsideMonth = result(recordId = 3L, snippet = "colic").copy(date = LocalDate(2026, 7, 31))
            val dateSearch = DateAwareRagRecordSearch(listOf(inMonth, today, outsideMonth))

            val events =
                sut(
                    recordSearch = dateSearch,
                    today = LocalDate(2026, 8, 24),
                    queryPolicyProvider = { RagQueryPolicy.CLOUD },
                )("What happened this month?").toList()

            val answer = events.filterIsInstance<RagStreamEvent.Chunk>().last().text
            assertEquals(0, engine.calls, "simple activity is already a database projection")
            assertEquals(RagDateRange(LocalDate(2026, 8, 1), LocalDate(2026, 8, 24)), dateSearch.requestedRange)
            assertTrue(answer.contains("pregnancy confirmed"), answer)
            assertTrue(answer.contains("weight recorded"), answer)
            assertFalse(answer.contains("colic"), "a row outside the requested month must not leak: $answer")
            val sources = events.filterIsInstance<RagStreamEvent.Sources>().single()
            assertEquals(listOf(1L, 2L), sources.sources.map { it.recordId })
        }

    @Test
    fun `given cloud current month activity with no rows then honest fallback and no model call`() =
        runTest {
            val dateSearch = DateAwareRagRecordSearch(emptyList())

            val output =
                sut(
                    recordSearch = dateSearch,
                    queryPolicyProvider = { RagQueryPolicy.CLOUD },
                )("What happened this month?").chunks()

            assertEquals(listOf(PLACEHOLDER, FALLBACK_TEXT), output)
            assertEquals(0, engine.calls)
        }

    @Test
    fun `given explicit patient question then another patients row is excluded from prompt and sources`() =
        runTest {
            val thunder = result(recordId = 11L, patientName = "Thunder", snippet = "pregnancy confirmed")
            val bella = result(recordId = 12L, patientName = "Bella", snippet = "colic")
            val search =
                object : RagRecordSearch {
                    override fun search(ftsQuery: String): List<SearchResult> = listOf(thunder, bella)
                }
            every { patientRepositoryMock.patientNames() } returns listOf("Thunder", "Bella")

            val events =
                sut(
                    recordSearch = search,
                    patientRepository = patientRepositoryMock,
                )("What did Thunder receive?").toList()

            assertTrue(engine.lastPrompt.orEmpty().contains("[VACCINATION #11] Thunder"))
            assertFalse(engine.lastPrompt.orEmpty().contains("[VACCINATION #12] Bella"))
            val sources = events.filterIsInstance<RagStreamEvent.Sources>().single()
            assertEquals(listOf(11L), sources.sources.map { it.recordId })
        }

    @Test
    fun `given unknown named patient then other patients cannot ground a cloud answer`() =
        runTest {
            every { patientRepositoryMock.patientNames() } returns listOf("Thunder", "Bella")
            val search =
                object : RagRecordSearch {
                    override fun search(ftsQuery: String): List<SearchResult> =
                        listOf(
                            result(recordId = 11L, patientName = "Thunder"),
                            result(recordId = 12L, patientName = "Bella"),
                        )
                }

            val output =
                sut(
                    recordSearch = search,
                    patientRepository = patientRepositoryMock,
                    queryPolicyProvider = { RagQueryPolicy.CLOUD },
                )("Is Storm pregnant?").chunks()

            assertEquals(listOf(PLACEHOLDER, FALLBACK_TEXT), output)
            assertEquals(0, engine.calls, "unknown patient names must not borrow another patient's records")
        }

    @Test
    fun `given ambiguous patient prefix then records stay out of the cloud context`() =
        runTest {
            every { patientRepositoryMock.patientNames() } returns listOf("Thunder", "Thunderbird")
            val search =
                object : RagRecordSearch {
                    override fun search(ftsQuery: String): List<SearchResult> = listOf(result())
                }

            val output =
                sut(
                    recordSearch = search,
                    patientRepository = patientRepositoryMock,
                    queryPolicyProvider = { RagQueryPolicy.CLOUD },
                )("Is Thund pregnant?").chunks()

            assertEquals(listOf(PLACEHOLDER, FALLBACK_TEXT), output)
            assertEquals(0, engine.calls, "ambiguous patient prefixes must not select an arbitrary horse")
        }

    @Test
    fun `given pregnancy question with only a vaccination row then cloud still refuses to invent gestation`() =
        runTest {
            every { patientRepositoryMock.patientNames() } returns listOf("Thunder")
            val search =
                object : RagRecordSearch {
                    override fun search(ftsQuery: String): List<SearchResult> = listOf(result())
                }

            val output =
                sut(
                    recordSearch = search,
                    patientRepository = patientRepositoryMock,
                    queryPolicyProvider = { RagQueryPolicy.CLOUD },
                )("Is Thunder pregnant?").chunks()

            assertEquals(listOf(PLACEHOLDER, FALLBACK_TEXT), output)
            assertEquals(0, engine.calls)
        }

    @Test
    fun `given cloud policy and dosage question without medication records when invoked then refusal remains`() =
        runTest {
            every { searchRepositoryMock.search(any(), any(), any(), any()) } returns emptyList()

            val output =
                sut(queryPolicyProvider = { RagQueryPolicy.CLOUD })("How much metronidazole should I give?").chunks()

            assertEquals(listOf(PLACEHOLDER, EnAssistantStrings.dosageRefusal), output)
            assertEquals(0, engine.calls, "cloud flexibility must not bypass the dosage safety gate")
        }

    @Test
    fun `given empty retrieval and unrelated history when invoked then fallback emitted and engine never called`() =
        runTest {
            // History alone must not unlock the model call: only turns that
            // actually share subject matter with the question may.
            every { searchRepositoryMock.search(any(), any(), any(), any()) } returns emptyList()

            val history =
                listOf(
                    RagHistoryEntry(
                        question = "What dental work does Bella need?",
                        answer = "Bella has a floating appointment planned.",
                    ),
                )

            val output = sut()("When was Thunder's last farrier visit?", history).chunks()

            assertEquals(0, engine.calls, "unrelated history must not unlock the model")
            assertEquals(listOf(PLACEHOLDER, FALLBACK_TEXT), output)
        }

    @Test
    fun `given retrieval without the asked-for record type when invoked then fallback emitted and engine never called`() =
        runTest {
            // Sim regression: seeded patient data makes retrieval NON-empty
            // (the OR-retry matches Thunder's PATIENT identity record) while
            // zero farrier records exist - the model freeballed a visit date.
            val patientOnly =
                SearchResult(
                    patientId = 7L,
                    patientName = "Thunder",
                    breed = "Thoroughbred",
                    microchipId = null,
                    recordType = "PATIENT",
                    recordId = 7L,
                    date = null,
                    snippet = "Thunder - Thoroughbred gelding, owner Daniela",
                )
            every { searchRepositoryMock.search(any(), any(), any(), any()) } returns listOf(patientOnly)

            val output = sut()("When was Thunder's last farrier visit?").chunks()

            assertEquals(0, engine.calls, "patient-identity grounding must not unlock a farrier-date answer")
            assertEquals(listOf(PLACEHOLDER, FALLBACK_TEXT), output)
        }

    @Test
    fun `given retrieval containing the asked-for record type when invoked then engine called normally`() =
        runTest {
            every { searchRepositoryMock.search(any(), any(), any(), any()) } returns
                listOf(result().copy(recordType = "FARRIER_VISIT", recordId = 301L, snippet = "Shoeing, all four feet"))

            // Non-superlative phrasing: "last ... visit" questions take the
            // deterministic date path instead of the model.
            sut()("What did the farrier do for Thunder?").answers()

            assertEquals(1, engine.calls, "a real farrier record grounds the question")
        }

    @Test
    fun `given latest-record date question with typed records when invoked then deterministic answer without engine`() =
        runTest {
            // Live-FM regression: the model freeballed today-ish dates ("last
            // farrier visit was on 25 Aug") even with the real chunks in
            // context. Superlative-date questions must be answered in Kotlin.
            val older =
                result().copy(
                    recordType = "FARRIER_VISIT",
                    recordId = 301L,
                    date = LocalDate(2026, 7, 2),
                    snippet = "Trim, all four feet",
                )
            val newest =
                result().copy(
                    recordType = "FARRIER_VISIT",
                    recordId = 302L,
                    date = LocalDate(2026, 8, 22),
                    snippet = "Shoeing",
                )
            every { searchRepositoryMock.search(any(), any(), any(), any()) } returns listOf(older, newest)

            val events = sut()("When was Thunder's last farrier visit?").toList()
            val chunks = events.filterIsInstance<RagStreamEvent.Chunk>().map { it.text }

            assertEquals(0, engine.calls, "superlative-date answers are computed, never modeled")
            val answer = chunks.last()
            assertTrue(answer.contains("22 Aug 2026"), "answer must carry the actual latest date: $answer")
            assertTrue(answer.contains("[FARRIER_VISIT #302]"), "deterministic answer cites its source inline")
            val sources = events.filterIsInstance<RagStreamEvent.Sources>().single()
            assertEquals(302L, sources.sources.single().recordId)
        }

    @Test
    fun `given portuguese latest-record date question then deterministic answer is localized`() =
        runTest {
            val farrier =
                result(recordId = 302L, snippet = "Ferragem")
                    .copy(recordType = "FARRIER_VISIT", date = LocalDate(2026, 8, 22))
            every { searchRepositoryMock.search(any(), any(), any(), any()) } returns listOf(farrier)

            val events = sut()("Quando foi a última ferragem da Thunder?").toList()
            val answer = events.filterIsInstance<RagStreamEvent.Chunk>().last().text

            assertEquals(0, engine.calls, "Portuguese superlative-date answers are computed, never modeled")
            assertTrue(answer.contains("visita de ferragem"), "answer must be localized: $answer")
            assertTrue(answer.contains("22 Aug 2026"), "answer must carry the retrieved date: $answer")
            val sourceId =
                events
                    .filterIsInstance<RagStreamEvent.Sources>()
                    .single()
                    .sources
                    .single()
                    .recordId
            assertEquals(
                302L,
                sourceId,
            )
        }

    @Test
    fun `given latest-record date question without typed records when invoked then honest fallback and zero engine calls`() =
        runTest {
            every { searchRepositoryMock.search(any(), any(), any(), any()) } returns listOf(result()) // VACCINATION only

            val output = sut()("When was Thunder's last farrier visit?").chunks()

            assertEquals(0, engine.calls)
            assertEquals(FALLBACK_TEXT, output.last())
        }

    @Test
    fun `given empty retrieval but relevant history then strict record question still falls back`() =
        runTest {
            every { searchRepositoryMock.search(any(), any(), any(), any()) } returns emptyList()

            val history =
                listOf(
                    RagHistoryEntry(
                        question = "Who shoes Thunder?",
                        answer = "Thunder's farrier visits are tracked in the app.",
                    ),
                )

            val output = sut()("When was his last farrier visit?", history).chunks()

            assertEquals(0, engine.calls, "conversation text must not unlock an unsupported record fact")
            assertEquals(listOf(PLACEHOLDER, FALLBACK_TEXT), output)
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
    fun `given cloud policy when invoked then context budget exceeds foundation-model budget`() =
        runTest {
            val records =
                (1..20).map { id ->
                    result(
                        recordId = id.toLong(),
                        snippet = "record $id " + "x".repeat(1190),
                    )
                }
            every { searchRepositoryMock.search(any(), any(), any(), any()) } returns records

            sut(queryPolicyProvider = { RagQueryPolicy.CLOUD })(QUERY).toList()

            val prompt = engine.lastPrompt.orEmpty()
            assertTrue(prompt.contains("[VACCINATION #20]"), "cloud context should not stop at the 4096-token budget")
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
            assertTrue(engine.lastPrompt.orEmpty().contains("Question: $QUERY"))
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
    fun `given weak AND leg when OR retry recovers records then patient scoping excludes other patients`() =
        runTest {
            // Call 1: the AND query - ONE weak hit, on the WRONG patient.
            // Call 2: the OR retry recovering both patients' records. Patient
            // scope is an exclusion boundary, not only a ranking hint.
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
            assertFalse(prompt.contains("[VACCINATION #201] Comet"))
            assertFalse(prompt.contains("[VACCINATION #203] Comet"))
        }

    @Test
    fun `given exactly three AND hits when invoked then threshold not crossed and no OR retry fires`() =
        runTest {
            // Boundary: WEAK_RESULT_THRESHOLD = 3 means three hits are STRONG
            // (retry skipped - the lazy OR leg is never queried).
            val three = (1L..3L).map { id -> result().copy(recordId = id) }
            every { searchRepositoryMock.search(any(), any(), any(), any()) } returns three

            sut()(QUERY).answers()

            verify(VerifyMode.exactly(1)) { searchRepositoryMock.search(any(), any(), any(), any()) }
        }

    @Test
    fun `given two AND hits when invoked then weak leg crosses below threshold and OR retry fires`() =
        runTest {
            // Boundary: two hits are WEAK, so the broad OR retry must run.
            val two = (1L..2L).map { id -> result().copy(recordId = id) }
            every { searchRepositoryMock.search(any(), any(), any(), any()) } returns two

            sut()(QUERY).answers()

            verify(VerifyMode.exactly(2)) { searchRepositoryMock.search(any(), any(), any(), any()) }
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
    fun `given model reply without citation when records were used then sources enforced and bubble text stripped`() =
        runTest {
            // Enforcement appends the retrieved header, the Sources event
            // maps it to a chip, and the final bubble text carries NO
            // bracket - the citation lives in the chip, not the prose.
            every { searchRepositoryMock.search(any(), any(), any(), any()) } returns listOf(result())
            engine.nextChunkOverride = "Thunder is a horse."

            val events = sut()(QUERY).toList()

            val final = events.filterIsInstance<RagStreamEvent.Chunk>().last().text
            assertEquals("Thunder is a horse.", final)
            val sources = events.filterIsInstance<RagStreamEvent.Sources>().single()
            assertEquals("VACCINATION", sources.sources.single().recordType)
        }

    @Test
    fun `given many selected records and no model citations when enforced then top-3 become source chips`() =
        runTest {
            // Regression: enforcement used to append EVERY selected header -
            // a ten-record answer gained ten noise lines. Cap is top-3 by
            // rank, surfaced as Sources chips; the bubble text stays clean.
            val five =
                (1L..5L).map { id -> result().copy(recordId = id, snippet = "note $id") }
            every { searchRepositoryMock.search(any(), any(), any(), any()) } returns five
            engine.nextChunkOverride = "Thunder is a horse."

            val events = sut()(QUERY).toList()

            val final = events.filterIsInstance<RagStreamEvent.Chunk>().last().text
            assertFalse(final.contains("["), "enforced headers must not reach the bubble text: $final")
            val sources = events.filterIsInstance<RagStreamEvent.Sources>().single()
            assertEquals(
                listOf("VACCINATION#1", "VACCINATION#2", "VACCINATION#3"),
                sources.sources.map { "${it.recordType}#${it.recordId}" },
                "chips must carry the top-3 by retrieval rank",
            )
        }

    @Test
    fun `given model reply already citing when records were used then single sources event and clean text`() =
        runTest {
            every { searchRepositoryMock.search(any(), any(), any(), any()) } returns listOf(result())
            engine.nextChunkOverride = "Tetanus booster recorded in [VACCINATION #123] Thunder."

            val events = sut()(QUERY).toList()

            val final = events.filterIsInstance<RagStreamEvent.Chunk>().last().text
            assertEquals("Tetanus booster recorded in Thunder.", final)
            assertEquals(1, events.filterIsInstance<RagStreamEvent.Sources>().size, "cited reply must not gain a duplicate source block")
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
            assertEquals("See Ghost for details.", final)
            assertFalse(final.contains("["), "fabricated and enforced brackets must not reach the bubble: $final")
            val sources = events.filterIsInstance<RagStreamEvent.Sources>().single()
            assertEquals(
                listOf("VACCINATION#123"),
                sources.sources.map { "${it.recordType}#${it.recordId}" },
                "only the retrieved record may become a source card",
            )
        }

    @Test
    fun `given mixed mapped and fabricated citations when completed then only mapped becomes a chip and all brackets stripped`() =
        runTest {
            // Defect: fabricated brackets ([GROOMING #77] - no such record in
            // context) used to stay visible in the bubble even though they
            // produced no source card. Every bracket is stripped from the
            // display text; only the MAPPED one reaches the Sources channel.
            every { searchRepositoryMock.search(any(), any(), any(), any()) } returns listOf(result())
            engine.nextChunkOverride = "Booster given per [VACCINATION #123]; see also [GROOMING #77]."

            val events = sut()(QUERY).toList()

            val final = events.filterIsInstance<RagStreamEvent.Chunk>().last().text
            assertFalse(final.contains("["), "all citation brackets must be stripped: $final")
            assertFalse(final.contains("#77"), "fabricated record id must not reach the bubble: $final")
            val sources = events.filterIsInstance<RagStreamEvent.Sources>().single()
            assertEquals(
                listOf("VACCINATION#123"),
                sources.sources.map { "${it.recordType}#${it.recordId}" },
                "only the mapped citation may become a source card",
            )
        }

    @Test
    fun `given prompt-format placeholder echoed by model when completed then placeholder stripped from bubble`() =
        runTest {
            // Defect: the system prompt's old example ([Vaccination #123]
            // Thunder) was parroted verbatim by small models as if it were a
            // real record. The example is a FORMAT placeholder now, and even
            // when the model echoes it, the strip removes it from the bubble.
            every { searchRepositoryMock.search(any(), any(), any(), any()) } returns listOf(result())
            engine.nextChunkOverride = "Cite sources like [RECORD_TYPE #ID] in your answer."

            val events = sut()(QUERY).toList()

            val final = events.filterIsInstance<RagStreamEvent.Chunk>().last().text
            assertFalse(final.contains("[RECORD_TYPE #ID]"), "prompt format placeholder must never surface: $final")
            assertTrue(AssistantPrompts.SYSTEM_PROMPT.contains("[RECORD_TYPE #ID]"), "format-only example expected in prompt")
            assertFalse(
                AssistantPrompts.SYSTEM_PROMPT.contains("[Vaccination #123]"),
                "real-looking example invites parroting",
            )
        }

    @Test
    fun `given mid-sentence summary tag when completed then tag removed and sentence intact`() =
        runTest {
            // Defect: "Thunder had [Summary] treatment on 25 Aug 2026." used
            // the computed-facts tag as a word inside the sentence. The tag
            // is stripped and the surrounding whitespace repaired so the
            // sentence reads naturally.
            every { searchRepositoryMock.search(any(), any(), any(), any()) } returns listOf(result())
            engine.nextChunkOverride = "Thunder had [Summary] treatment on 25 Aug 2026."

            val events = sut()(QUERY).toList()

            val final = events.filterIsInstance<RagStreamEvent.Chunk>().last().text
            assertEquals("Thunder had treatment on 25 Aug 2026.", final)
        }

    @Test
    fun `given possessive farrier query when AND leg misses and retry recovers record then deterministic dated answer with sources`() =
        runTest {
            // Regression shape for the farrier UI test: the possessive cleans
            // to "Thunders", the strict AND leg misses, and the weak-retry OR
            // leg recovers the farrier visit. Superlative-date questions are
            // answered deterministically from the retrieved record date - the
            // model previously freeballed today-ish dates here - and the
            // answer cites the real header inline with a Sources event.
            val farrier =
                result().copy(recordType = "FARRIER_VISIT", recordId = 301L, snippet = "Full set steel shoes")
            every { searchRepositoryMock.search(any(), any(), any(), any()) } sequentiallyReturns
                listOf(emptyList(), listOf(farrier))

            val events = sut()("When was Thunder's last farrier visit?").toList()

            assertEquals(0, engine.calls, "superlative-date answers are computed, never modeled")
            val final = events.filterIsInstance<RagStreamEvent.Chunk>().last().text
            assertTrue(final.contains("1 May 2024"), "answer must carry the retrieved date: $final")
            assertTrue(final.contains("[FARRIER_VISIT #301]"), "deterministic answer cites its source inline")
            val sources = events.filterIsInstance<RagStreamEvent.Sources>().single()
            assertEquals("FARRIER_VISIT", sources.sources.single().recordType)
        }

    @Test
    fun `given zero retrieval and care summary when model answers uncited then bubble text stays clean`() =
        runTest {
            // Regression: fix-18's deterministic summary answers recency
            // questions even when retrieval is empty (sparse-indexed rows).
            // The [Summary] tag is enforced internally for the citation
            // channel, then stripped - the bubble must never show it, and no
            // record card may be invented without a retrieved record.
            every { searchRepositoryMock.search(any(), any(), any(), any()) } returns emptyList()
            val repos = FakeAnalysisRepos()
            repos.patients.patients = listOf(testPatient(1, "Thunder"))
            repos.farrierVisits.entries =
                listOf(testFarrierVisit(id = 301, patientId = 1, date = LocalDate(2026, 8, 24)))
            engine.nextChunkOverride = "Thunder's last farrier visit was 2026-08-24."

            val events =
                sut(analysisContextBuilder = repos.builder)("When was Thunder's last farrier visit?").toList()

            val final = events.filterIsInstance<RagStreamEvent.Chunk>().last().text
            assertEquals("Thunder's last farrier visit was 2026-08-24.", final)
            assertFalse(final.contains("["), "no bracket may reach the bubble: $final")
            assertTrue(
                events.filterIsInstance<RagStreamEvent.Sources>().isEmpty(),
                "no record was retrieved - no source cards may be invented",
            )
        }

    @Test
    fun `given summary-only turn with fabricated bracket when model skips Summary tag then brackets still never surface`() =
        runTest {
            // Regression: the [Summary]-append guard used to key on bare "[",
            // so a FABRICATED bracket the model invented ([Giraffe #1]) -
            // which maps to zero source cards - blocked the append and
            // shipped an uncited answer wearing a fake citation. The guard
            // keys on the literal [Summary] tag now, and the display strip
            // removes the fabricated bracket from the bubble either way.
            every { searchRepositoryMock.search(any(), any(), any(), any()) } returns emptyList()
            val repos = FakeAnalysisRepos()
            repos.patients.patients = listOf(testPatient(1, "Thunder"))
            repos.farrierVisits.entries =
                listOf(testFarrierVisit(id = 301, patientId = 1, date = LocalDate(2026, 8, 24)))
            engine.nextChunkOverride = "Thunder's last farrier visit was 2026-08-24. See [GIRAFFE #1]."

            val events =
                sut(analysisContextBuilder = repos.builder)("When was Thunder's last farrier visit?").toList()

            val final = events.filterIsInstance<RagStreamEvent.Chunk>().last().text
            assertTrue(final.contains("farrier visit was 2026-08-24"), "answer sentence must survive: $final")
            assertFalse(final.contains("["), "fabricated bracket must not reach the bubble: $final")
            assertFalse(final.contains("GIRAFFE"), "fabricated record type must not reach the bubble: $final")
            assertTrue(
                events.filterIsInstance<RagStreamEvent.Sources>().isEmpty(),
                "the fabricated citation maps to no record - no source card may be invented",
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
            every { searchRepositoryMock.search(any(), any(), any(), any()) } returns
                listOf(result(snippet = "512 kg recorded").copy(recordType = "WEIGHT"))

            val output = sut()("How much does she weigh?").answers()

            assertEquals(1, engine.calls, "weight questions are not dosage intents")
            assertTrue(!output.first().contains("dosages"))
        }

    @Test
    fun `given requested type outside the requested date then strict fallback wins`() =
        runTest {
            every { patientRepositoryMock.patientNames() } returns listOf("Thunder")
            val oldVaccination = result(recordId = 401L).copy(date = LocalDate(2026, 7, 31))
            val currentWeight =
                result(recordId = 402L, snippet = "512 kg recorded")
                    .copy(recordType = "WEIGHT", date = LocalDate(2026, 8, 10))
            val search =
                object : RagRecordSearch {
                    override fun search(ftsQuery: String): List<SearchResult> = listOf(oldVaccination, currentWeight)
                }

            val output =
                sut(
                    recordSearch = search,
                    patientRepository = patientRepositoryMock,
                    queryPolicyProvider = { RagQueryPolicy.CLOUD },
                )("What vaccination did Thunder receive this month?").chunks()

            assertEquals(listOf(PLACEHOLDER, FALLBACK_TEXT), output)
            assertEquals(0, engine.calls, "a different record in the same month must not ground vaccination")
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
    fun `given analysis builder when invoked then the use case reference date reaches gestation summary`() =
        runTest {
            every { searchRepositoryMock.search(any(), any(), any(), any()) } returns emptyList()
            val repos = FakeAnalysisRepos()
            repos.patients.patients = listOf(testPatient(1, "Bella"))
            repos.gestations.entries =
                listOf(
                    testGestation(
                        id = 41,
                        patientId = 1,
                        breedingDate = LocalDate(2025, 1, 1),
                        expectedDueDate = LocalDate(2000, 1, 1),
                    ),
                )

            sut(
                analysisContextBuilder = repos.builder,
                today = LocalDate(2025, 5, 11),
            )("Which mares are pregnant?").chunks()

            val prompt = engine.lastPrompt.orEmpty()
            assertTrue(prompt.contains("day 130"), "summary must use the turn's reference date: $prompt")
            assertTrue(
                prompt.contains("expected foaling 2025-12-07"),
                "summary must recompute the due date: $prompt",
            )
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
        const val QUERY = "Tell me about the vaccination note"
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
