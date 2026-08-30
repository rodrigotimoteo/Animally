package com.github.rodrigotimoteo.animally.llm

import com.github.rodrigotimoteo.animally.domain.owner.IOwnerRepository
import com.github.rodrigotimoteo.animally.domain.owner.model.Owner
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
import kotlinx.coroutines.flow.collect
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
    var cloudFirstCalls: Int = 0
    var lastPrompt: String? = null
    var lastInstructions: String? = null

    /** When set, emitted instead of the default markdown-laden chunk (used by sanitizer cases). */
    var nextChunkOverride: String? = null

    /** Cumulative snapshots used to exercise stream-only rendering edges. */
    var nextChunkOverrides: List<String>? = null

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
            val chunks =
                nextChunkOverrides
                    ?: listOf(
                        nextChunkOverride
                            ?: "She is **pregnant** with a `due date` of __May 2025__. See [Vaccination #1](https://example.com/fake).",
                    )
            chunks.forEach { emit(it) }
            streamingError?.let { throw it }
        }

    override fun generateCloudFirst(
        prompt: String,
        instructions: String,
    ): Flow<String> =
        flow {
            cloudFirstCalls++
            generateStreaming(prompt, instructions).collect { emit(it) }
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
    private val ownerRepositoryMock: IOwnerRepository = mock(MockMode.autoUnit)
    private val engine = FakeRagLlmEngine()

    private fun sut(
        config: RagConfig = RagConfig.DEFAULT,
        strings: AssistantStrings = EnAssistantStrings,
        recordSearch: RagRecordSearch? =
            RagRecordSearch { ftsQuery -> searchRepositoryMock.search(ftsQuery, null, null, null) },
        analysisContextBuilder: AnalysisContextBuilder? = null,
        today: LocalDate = LocalDate(2026, 8, 24),
        patientRepository: IPatientRepository? = null,
        ownerRepository: IOwnerRepository? = null,
        queryPolicyProvider: suspend () -> RagQueryPolicy = { RagQueryPolicy.ON_DEVICE },
        queryPolicyForQuestion: (suspend (String) -> RagQueryPolicy)? = null,
    ) = GenerateRagResponseUseCase(
        SearchUseCase(searchRepositoryMock),
        engine,
        config,
        strings,
        recordSearch,
        patientRepository = patientRepository,
        ownerRepository = ownerRepository,
        analysisContextBuilder = analysisContextBuilder,
        today = today,
        queryPolicyProvider = queryPolicyProvider,
        queryPolicyForQuestion = queryPolicyForQuestion,
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

    private fun owner() =
        Owner(
            id = 99L,
            name = "Inês Martins",
            email = null,
            phone = null,
            address = "Herdade da Serra, Évora",
            createdAt = kotlin.time.Instant.fromEpochMilliseconds(0L),
            updatedAt = kotlin.time.Instant.fromEpochMilliseconds(0L),
        )

    private fun ownerResult() =
        SearchResult(
            patientId = 99L,
            patientName = "Inês Martins",
            breed = null,
            microchipId = null,
            recordType = ISearchRepository.TYPE_OWNER,
            recordId = 99L,
            date = null,
            snippet = "Inês Martins Herdade da Serra, Évora",
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
    fun `given cloud policy and generic husbandry question with possessive horse wording then model answers`() =
        runTest {
            every { searchRepositoryMock.search(any(), any(), any(), any()) } returns emptyList()
            engine.nextChunkOverride = "Start with good-quality forage, fresh water, and a diet tailored to the horse's needs."

            val output =
                sut(queryPolicyProvider = { RagQueryPolicy.CLOUD })("What should I feed my horse?").answers()

            assertEquals(1, engine.cloudFirstCalls)
            assertEquals(1, engine.calls)
            assertTrue(output.first().contains("good-quality forage"))
        }

    @Test
    fun `question-specific cloud policy uses cloud-first route for a general question`() =
        runTest {
            every { searchRepositoryMock.search(any(), any(), any(), any()) } returns emptyList()

            sut(
                queryPolicyProvider = { RagQueryPolicy.ON_DEVICE },
                queryPolicyForQuestion = { RagQueryPolicy.CLOUD },
            )("What is the capital of Portugal?").answers()

            assertEquals(1, engine.cloudFirstCalls)
            assertEquals(1, engine.calls)
        }

    @Test
    fun `cloud policy uses cloud-first route for a grounded record question`() =
        runTest {
            val farrier =
                result(recordId = 91L, snippet = "Trimmed all four feet")
                    .copy(recordType = "FARRIER_VISIT")
            every { searchRepositoryMock.search(any(), any(), any(), any()) } returns listOf(farrier)

            sut(queryPolicyProvider = { RagQueryPolicy.CLOUD })("What did Thunder's farrier do?").answers()

            assertEquals(1, engine.cloudFirstCalls)
            assertEquals(1, engine.calls)
        }

    @Test
    fun `cloud policy uses cloud-first route for a grounded analysis question`() =
        runTest {
            every { searchRepositoryMock.search(any(), any(), any(), any()) } returns emptyList()
            val repos = FakeAnalysisRepos()
            repos.patients.patients = listOf(testPatient(1, "Thunder"))

            sut(
                analysisContextBuilder = repos.builder,
                queryPolicyProvider = { RagQueryPolicy.CLOUD },
            )("How many patients do I have?").answers()

            assertEquals(1, engine.cloudFirstCalls)
            assertEquals(1, engine.calls)
        }

    @Test
    fun `given active patients when asking title-cased educational question then cloud model answers`() =
        runTest {
            every { patientRepositoryMock.patientNames() } returns listOf("Thunder", "Bella")
            every { searchRepositoryMock.search(any(), any(), any(), any()) } returns emptyList()

            val output =
                sut(
                    patientRepository = patientRepositoryMock,
                    queryPolicyProvider = { RagQueryPolicy.CLOUD },
                )("Can you explain Equine Metabolic Syndrome?").answers()

            assertEquals(1, engine.calls)
            assertTrue(output.first().contains("pregnant"))
        }

    @Test
    fun `given active patients when asking unrelated title-cased question then records stay out`() =
        runTest {
            every { patientRepositoryMock.patientNames() } returns listOf("Thunder", "Bella")
            every { searchRepositoryMock.search(any(), any(), any(), any()) } returns listOf(result())

            val output =
                sut(
                    patientRepository = patientRepositoryMock,
                    queryPolicyProvider = { RagQueryPolicy.CLOUD },
                )("Who wrote Pride and Prejudice?").answers()

            assertEquals(1, engine.calls)
            assertTrue(output.first().contains("pregnant"))
            assertFalse(engine.lastPrompt.orEmpty().contains("[VACCINATION #123]"))
        }

    @Test
    fun `given lowercase unknown patient then another patient cannot ground the lookup`() =
        runTest {
            every { patientRepositoryMock.patientNames() } returns listOf("Thunder", "Bella")
            val search =
                object : RagRecordSearch {
                    override fun search(ftsQuery: String): List<SearchResult> = listOf(result())
                }

            val output =
                sut(
                    recordSearch = search,
                    patientRepository = patientRepositoryMock,
                    queryPolicyProvider = { RagQueryPolicy.CLOUD },
                )("is storm pregnant?").chunks()

            assertEquals(listOf(PLACEHOLDER, FALLBACK_TEXT), output)
            assertEquals(0, engine.calls)
        }

    @Test
    fun `given typed lookup when broad retrieval includes unrelated rows then only requested type reaches model`() =
        runTest {
            every { patientRepositoryMock.patientNames() } returns listOf("Thunder", "Bella")
            val ultrasound =
                result(
                    snippet = "ultrasound finding",
                    recordId = 11L,
                    patientName = "Thunder",
                ).copy(recordType = "ULTRASOUND")
            val vaccination =
                result(
                    snippet = "date vaccination",
                    recordId = 12L,
                    patientName = "Thunder",
                )
            val otherPatientUltrasound =
                result(
                    snippet = "ultrasound finding",
                    recordId = 13L,
                    patientName = "Bella",
                ).copy(recordType = "ULTRASOUND")
            val search =
                object : RagRecordSearch {
                    override fun search(ftsQuery: String): List<SearchResult> = listOf(ultrasound, vaccination, otherPatientUltrasound)
                }

            sut(
                recordSearch = search,
                patientRepository = patientRepositoryMock,
                queryPolicyProvider = { RagQueryPolicy.CLOUD },
            )("What did Thunder's ultrasound show?").chunks()

            val prompt = engine.lastPrompt.orEmpty()
            assertTrue(prompt.contains("[ULTRASOUND #11] Thunder"), prompt)
            assertFalse(prompt.contains("[VACCINATION #12]"), prompt)
            assertFalse(prompt.contains("[ULTRASOUND #13] Bella"), prompt)
        }

    @Test
    fun `given typed lookup with no requested record then unrelated rows cannot unlock model`() =
        runTest {
            every { patientRepositoryMock.patientNames() } returns listOf("Thunder")
            val unrelated = result(snippet = "vaccination date", recordId = 12L)
            val search = RagRecordSearch { listOf(unrelated) }

            val output =
                sut(
                    recordSearch = search,
                    patientRepository = patientRepositoryMock,
                    queryPolicyProvider = { RagQueryPolicy.CLOUD },
                )("What did Thunder's dentistry show?").chunks()

            assertEquals(0, engine.calls)
            assertEquals(listOf(PLACEHOLDER, FALLBACK_TEXT), output)
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
    fun `given named owner contact question then exact owner data bypasses engine`() =
        runTest {
            val owner = ownerResult()
            val patient = result(recordId = 12L, patientName = "Thunder", snippet = "Herdade da Serra")
            val search =
                object : RagRecordSearch {
                    override fun search(ftsQuery: String): List<SearchResult> = listOf(owner, patient)
                }
            every { patientRepositoryMock.patientNames() } returns listOf("Thunder")
            every { ownerRepositoryMock.getOwnerList() } returns listOf(owner())

            val events =
                sut(
                    recordSearch = search,
                    patientRepository = patientRepositoryMock,
                    ownerRepository = ownerRepositoryMock,
                    queryPolicyProvider = { RagQueryPolicy.CLOUD },
                )("What is Inês Martins's address?").toList()

            val sources = events.filterIsInstance<RagStreamEvent.Sources>().single()
            assertEquals(listOf(99L), sources.sources.map { it.recordId })
            assertEquals(0, engine.calls, "exact owner contact fields must not be paraphrased by the model")
            assertEquals(
                "Inês Martins's address: Herdade da Serra, Évora.",
                events.filterIsInstance<RagStreamEvent.Chunk>().last().text,
            )
        }

    @Test
    fun `given portuguese owner contact question then exact owner data is localized`() =
        runTest {
            val storedOwner = owner().copy(phone = "+351 910 000 101")
            every { patientRepositoryMock.patientNames() } returns emptyList()
            every { ownerRepositoryMock.getOwnerList() } returns listOf(storedOwner)

            val output =
                sut(
                    recordSearch = RagRecordSearch { emptyList() },
                    patientRepository = patientRepositoryMock,
                    ownerRepository = ownerRepositoryMock,
                    queryPolicyProvider = { RagQueryPolicy.CLOUD },
                )("Qual é o telefone da Inês Martins?").chunks()

            assertEquals(0, engine.calls)
            assertEquals("Telefone de Inês Martins: +351 910 000 101.", output.last())
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
    fun `given unknown named patient typed lookup then cloud model is not asked to guess`() =
        runTest {
            every { patientRepositoryMock.patientNames() } returns listOf("Thunder", "Bella")
            val search = RagRecordSearch { listOf(result(patientName = "Thunder")) }

            val output =
                sut(
                    recordSearch = search,
                    patientRepository = patientRepositoryMock,
                    queryPolicyProvider = { RagQueryPolicy.CLOUD },
                )("What is the vaccination date for a horse named Pegasus?").chunks()

            assertEquals(listOf(PLACEHOLDER, FALLBACK_TEXT), output)
            assertEquals(0, engine.calls, "an unknown named patient must not reach the cloud model")
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
            assertFalse(answer.contains("["), "internal record headers must stay out of the visible answer")
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
    fun `given patient date of birth when asked then exact patient row projection bypasses engine`() =
        runTest {
            val patient = testPatient(7L, "Thunder").copy(dateOfBirth = LocalDate(2017, 4, 18))
            every { searchRepositoryMock.search(any(), any(), any(), any()) } returns emptyList()

            val events =
                sut(
                    patientRepository = FakePatientRepository(listOf(patient)),
                )("What is Thunder's date of birth?")
                    .toList()

            assertEquals(0, engine.calls)
            val answer = events.filterIsInstance<RagStreamEvent.Chunk>().last().text
            assertEquals("Thunder's date of birth is 18 Apr 2017.", answer)
            val source =
                events
                    .filterIsInstance<RagStreamEvent.Sources>()
                    .single()
                    .sources
                    .single()
            assertEquals(ISearchRepository.TYPE_PATIENT, source.recordType)
            assertEquals(7L, source.recordId)
        }

    @Test
    fun `given missing patient date of birth when asked then no date is invented`() =
        runTest {
            val patient = testPatient(7L, "Thunder")
            every { searchRepositoryMock.search(any(), any(), any(), any()) } returns emptyList()

            val output =
                sut(
                    patientRepository = FakePatientRepository(listOf(patient)),
                )("What is Thunder's date of birth?").chunks()

            assertEquals(0, engine.calls)
            assertEquals("The date of birth for Thunder is not recorded.", output.last())
        }

    @Test
    fun `given Portuguese patient date of birth when asked then exact year is preserved`() =
        runTest {
            val patient = testPatient(7L, "Thunder").copy(dateOfBirth = LocalDate(2017, 4, 18))
            every { searchRepositoryMock.search(any(), any(), any(), any()) } returns emptyList()

            val output =
                sut(
                    patientRepository = FakePatientRepository(listOf(patient)),
                )("Qual é a data de nascimento do Thunder?").chunks()

            assertEquals(0, engine.calls)
            assertTrue(output.last().contains("2017"))
            assertTrue(output.last().contains("data de nascimento"))
        }

    @Test
    fun `given stored patient identity fields when asked then exact fields bypass the engine`() =
        runTest {
            val patient =
                testPatient(7L, "Thunder").copy(
                    species = "Equine",
                    breed = "Lusitano",
                    dateOfBirth = LocalDate(2017, 4, 18),
                    gender = "Mare",
                    microchipId = "985141000123456",
                )
            every { searchRepositoryMock.search(any(), any(), any(), any()) } returns emptyList()

            val events =
                sut(
                    patientRepository = FakePatientRepository(listOf(patient)),
                    today = LocalDate(2026, 8, 24),
                )("What breed, age, sex, and microchip does Thunder have?").toList()

            assertEquals(0, engine.calls)
            val answer = events.filterIsInstance<RagStreamEvent.Chunk>().last().text
            assertTrue(answer.contains("Thunder's breed is Lusitano."), answer)
            assertTrue(answer.contains("Thunder's age is 9."), answer)
            assertTrue(answer.contains("Thunder's sex is Mare."), answer)
            assertTrue(answer.contains("Thunder's microchip is 985141000123456."), answer)
            val source =
                events
                    .filterIsInstance<RagStreamEvent.Sources>()
                    .single()
                    .sources
                    .single()
            assertEquals(7L, source.recordId)
        }

    @Test
    fun `given missing patient identity fields when asked then no values are invented`() =
        runTest {
            val patient = testPatient(7L, "Thunder")
            every { searchRepositoryMock.search(any(), any(), any(), any()) } returns emptyList()

            val output =
                sut(
                    patientRepository = FakePatientRepository(listOf(patient)),
                )("What breed and age is Thunder?").chunks()

            assertEquals(0, engine.calls)
            assertTrue(output.last().contains("Thunder's breed is not recorded."), output.last())
            assertTrue(output.last().contains("Thunder's age is not recorded."), output.last())
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
    fun `given portuguese question when invoked then user turn carries matching language instruction`() =
        runTest {
            every { searchRepositoryMock.search(any(), any(), any(), any()) } returns listOf(result())

            sut()("Qual vacinação foi registada para Thunder?").toList()

            val prompt = engine.lastPrompt.orEmpty()
            assertTrue(prompt.contains("LANGUAGE FOR THIS TURN: Answer only in European Portuguese."))
            assertTrue(prompt.indexOf("LANGUAGE FOR THIS TURN") < prompt.indexOf("Question:"))
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
            assertTrue(text.contains("She is pregnant with a due date of May 2025. See."))
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
            engine.nextChunkOverride = "See [the source](https://vet.example.com/x) for details"

            val output = sut()(QUERY).answers()

            assertEquals("See the source for details", output.first())
        }

    @Test
    fun `humanized multiword record citation becomes a source card and never reaches the bubble`() =
        runTest {
            val farrier =
                result().copy(
                    recordType = "FARRIER_VISIT",
                    recordId = 91L,
                    snippet = "Routine trim",
                )
            every { searchRepositoryMock.search(any(), any(), any(), any()) } returns listOf(farrier)
            engine.nextChunkOverride =
                "The visit is documented in [FARRIER VISIT #91](https://example.com/internal)."

            val events = sut()("Tell me about Thunder's farrier visit").toList()

            val final = events.filterIsInstance<RagStreamEvent.Chunk>().last().text
            assertEquals("The visit is documented in.", final)
            assertTrue(
                events
                    .filterIsInstance<RagStreamEvent.Sources>()
                    .single()
                    .sources
                    .single()
                    .recordId == 91L,
                "humanized citation should still open the matching source card",
            )
        }

    @Test
    fun `record citation with model prose never leaks internal citation prose`() =
        runTest {
            val farrier =
                result().copy(
                    recordType = "FARRIER_VISIT",
                    recordId = 91L,
                    snippet = "Routine trim",
                )
            every { searchRepositoryMock.search(any(), any(), any(), any()) } returns listOf(farrier)
            engine.nextChunkOverride =
                "The visit [FARRIER VISIT #91 — internal reference] is documented."

            val events = sut()("Tell me about Thunder's farrier visit").toList()

            val final = events.filterIsInstance<RagStreamEvent.Chunk>().last().text
            assertFalse(final.contains("[FARRIER VISIT #91"))
            assertFalse(final.contains("#91"))
            assertFalse(final.contains("internal reference"))
            val sources = events.filterIsInstance<RagStreamEvent.Sources>().single()
            assertEquals(
                91L,
                sources.sources.single().recordId,
            )
        }

    @Test
    fun `given a partial internal citation snapshot then it never reaches the live bubble`() =
        runTest {
            val farrier =
                result().copy(
                    recordType = "FARRIER_VISIT",
                    recordId = 91L,
                    snippet = "Routine trim",
                )
            every { searchRepositoryMock.search(any(), any(), any(), any()) } returns listOf(farrier)
            engine.nextChunkOverrides =
                listOf(
                    "The visit is documented in [FARRIER VISIT #",
                    "The visit is documented in [FARRIER VISIT #91].",
                )

            val events = sut()("Tell me about Thunder's farrier visit").toList()

            assertTrue(
                events
                    .filterIsInstance<RagStreamEvent.Chunk>()
                    .none { it.text.contains("[FARRIER VISIT #") || it.text.contains("#91") },
                "partial citation syntax belongs in the source card, not the live bubble",
            )
            val source =
                events
                    .filterIsInstance<RagStreamEvent.Sources>()
                    .single()
                    .sources
                    .single()
            assertEquals(91L, source.recordId)
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

            assertEquals("Pregnant — see and notes here", output.first())
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
    fun `given a name prefix that is not an active patient then retrieval cannot silently select a longer name`() =
        runTest {
            every { patientRepositoryMock.patientNames() } returns listOf("Annabelle")
            every { searchRepositoryMock.search(any(), any(), any(), any()) } returns listOf(result(patientName = "Annabelle"))

            val output =
                sut(patientRepository = patientRepositoryMock)("What vaccination did Ann receive?").chunks()

            assertEquals(listOf(PLACEHOLDER, FALLBACK_TEXT), output)
            assertEquals(0, engine.calls, "an unresolved patient prefix must not unlock another patient's records")
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
            assertTrue(prompt.contains("Recent conversation (context only; not evidence):"), "history block missing")
            assertTrue(prompt.contains("User: Tell me about Thunder"))
            assertTrue(prompt.contains("Assistant: Thunder is a 7 year old Thoroughbred."))
            assertTrue(prompt.indexOf("Recent conversation (context only; not evidence):") < prompt.indexOf("Context:"))
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
    fun `given empty retrieval but relevant assistant history then patient fact still falls back`() =
        runTest {
            every { searchRepositoryMock.search(any(), any(), any(), any()) } returns emptyList()

            val history = listOf(RagHistoryEntry("Tell me about Thunder", "Thunder is a 7 year old mare."))
            val output = sut()("What is her condition?", history).chunks()

            assertEquals(0, engine.calls, "assistant prose must not unlock an unsupported patient fact")
            assertEquals(listOf(PLACEHOLDER, FALLBACK_TEXT), output)
        }

    @Test
    fun `given a singular follow-up then the latest active patient in history scopes retrieval`() =
        runTest {
            every { patientRepositoryMock.patientNames() } returns listOf("Thunder", "Estrela")
            every { searchRepositoryMock.search(any(), any(), any(), any()) } returns listOf(result())

            val history = listOf(RagHistoryEntry("Tell me about Thunder", "Thunder is a 7 year old mare."))
            val output = sut(patientRepository = patientRepositoryMock)("How old is she?", history).answers()

            assertEquals(1, engine.calls, "a scoped follow-up should reach the model with retrieved records")
            assertTrue(engine.lastPrompt.orEmpty().contains("Thunder"))
            assertTrue(output.last().contains("pregnant"))
        }

    @Test
    fun `given a singular follow-up when AND misses then retry keeps the resolved patient scope`() =
        runTest {
            every { patientRepositoryMock.patientNames() } returns listOf("Thunder", "Estrela")
            val searchedQueries = mutableListOf<String>()
            val scopedSearch =
                RagRecordSearch { ftsQuery ->
                    searchedQueries += ftsQuery
                    if (ftsQuery.contains(" OR ") && ftsQuery.contains("thunder*")) {
                        listOf(result(patientName = "Thunder"))
                    } else {
                        emptyList()
                    }
                }
            val history = listOf(RagHistoryEntry("Tell me about Thunder", "Thunder is a 7 year old mare."))

            val output =
                sut(
                    patientRepository = patientRepositoryMock,
                    recordSearch = scopedSearch,
                )("What breed is she?", history).answers()

            assertEquals(1, engine.calls, "resolved follow-up should reach the model after retry retrieval")
            assertTrue(output.last().contains("pregnant"))
            assertTrue(
                searchedQueries.any { query -> query.contains(" OR ") && query.contains("thunder*") },
                "OR retry must include the resolved patient token: $searchedQueries",
            )
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
    fun `given records in context when prompted then source cards are delegated to the app`() =
        runTest {
            every { searchRepositoryMock.search(any(), any(), any(), any()) } returns listOf(result())

            sut()(QUERY).toList()

            val instructions = engine.lastInstructions.orEmpty()
            assertTrue(instructions.lowercase().contains("tappable source card"))
            assertTrue(instructions.lowercase().contains("never expose internal record headers"))
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
    fun `given model reply without citation when sources are enforced then internal headers never flicker as chunks`() =
        runTest {
            every { searchRepositoryMock.search(any(), any(), any(), any()) } returns listOf(result())
            engine.nextChunkOverride = "Thunder is a horse."

            val events = sut()(QUERY).toList()

            assertTrue(
                events
                    .filterIsInstance<RagStreamEvent.Chunk>()
                    .none { it.text.contains("[VACCINATION #123]") },
                "citation headers belong in source cards, not transient answer snapshots",
            )
            assertEquals(1, events.filterIsInstance<RagStreamEvent.Sources>().size)
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

    @Test
    fun `given grouped citations when completed then all real sources map and citation block is hidden`() =
        runTest {
            val first = result(recordId = 123L)
            val second = result(recordId = 124L)
            every { searchRepositoryMock.search(any(), any(), any(), any()) } returns listOf(first, second)
            engine.nextChunkOverride =
                "The recorded trend is stable across both visits [VACCINATION #123, VACCINATION #124]."

            val events = sut()(QUERY).toList()

            val final = events.filterIsInstance<RagStreamEvent.Chunk>().last().text
            assertEquals("The recorded trend is stable across both visits.", final)
            val sources = events.filterIsInstance<RagStreamEvent.Sources>().single()
            assertEquals(
                listOf("VACCINATION#123", "VACCINATION#124"),
                sources.sources.map { "${it.recordType}#${it.recordId}" },
            )
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
            assertFalse(
                AssistantPrompts.SYSTEM_PROMPT.contains("[RECORD_TYPE #ID]"),
                "the prompt must not teach models to print internal headers",
            )
            assertFalse(
                AssistantPrompts.SYSTEM_PROMPT.contains("[Vaccination #123]"),
                "real-looking example invites parroting",
            )
        }

    @Test
    fun `given deterministic analysis heading echoed by model then internal heading stays out of bubble`() =
        runTest {
            every { searchRepositoryMock.search(any(), any(), any(), any()) } returns listOf(result())
            engine.nextChunkOverride =
                "There are four patients. [PATIENT CENSUS] [CARE COUNTS] [GESTATIONS] " +
                "[OVERDUE CARE (due before 2026-08-24)]."

            val events = sut()("How many patients do I have?").toList()

            val final = events.filterIsInstance<RagStreamEvent.Chunk>().last().text
            assertEquals("There are four patients.", final)
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
            // answer exposes the real record through a Sources event, not an
            // internal header in the visible bubble.
            val farrier =
                result().copy(recordType = "FARRIER_VISIT", recordId = 301L, snippet = "Full set steel shoes")
            every { searchRepositoryMock.search(any(), any(), any(), any()) } sequentiallyReturns
                listOf(emptyList(), listOf(farrier))

            val events = sut()("When was Thunder's last farrier visit?").toList()

            assertEquals(0, engine.calls, "superlative-date answers are computed, never modeled")
            val final = events.filterIsInstance<RagStreamEvent.Chunk>().last().text
            assertTrue(final.contains("1 May 2024"), "answer must carry the retrieved date: $final")
            assertFalse(final.contains("["), "internal record headers must stay out of the visible answer")
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
            assertTrue(prompt.contains("PATIENT CENSUS: 1 active patient: Thunder."))
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
            )("How many breeding records are recorded?").chunks()

            val prompt = engine.lastPrompt.orEmpty()
            assertTrue(prompt.contains("day 130"), "summary must use the turn's reference date: $prompt")
            assertTrue(
                prompt.contains("expected foaling 2025-12-07"),
                "summary must recompute the due date: $prompt",
            )
        }

    @Test
    fun `given current gestation question then answer uses live facts and source cards without model`() =
        runTest {
            every { searchRepositoryMock.search(any(), any(), any(), any()) } returns emptyList()
            val repos = FakeAnalysisRepos()
            repos.patients.patients = listOf(testPatient(1, "Lua"), testPatient(2, "Estrela"))
            repos.gestations.entries =
                listOf(
                    testGestation(
                        id = 41,
                        patientId = 1,
                        breedingDate = LocalDate(2025, 1, 1),
                        expectedDueDate = LocalDate(2000, 1, 1),
                    ).copy(gestationDays = 1),
                    testGestation(
                        id = 42,
                        patientId = 2,
                        breedingDate = LocalDate(2025, 2, 1),
                        expectedDueDate = LocalDate(2000, 1, 1),
                    ).copy(gestationDays = 2),
                )

            val query = "Which mares are currently pregnant, how many days along are they, and when are they due?"
            assertTrue(AnalysisIntents.wantsCurrentGestation(query))
            assertTrue(RecordQuestionIntent.isRecordQuestion(query, null, null))
            assertEquals(2, repos.builder.gestationFacts(query, LocalDate(2025, 5, 11))?.size)

            val events =
                sut(
                    analysisContextBuilder = repos.builder,
                    patientRepository = repos.patients,
                    today = LocalDate(2025, 5, 11),
                )(query)
                    .toList()

            assertEquals(0, engine.calls, "current gestation facts must never be rewritten by the model")
            val answer = events.filterIsInstance<RagStreamEvent.Chunk>().last().text
            assertTrue(answer.contains("Lua — day 130"), answer)
            assertTrue(answer.contains("Estrela — day 99"), answer)
            assertTrue(answer.contains("7 Dec 2025"), answer)
            assertTrue(answer.contains("7 Jan 2026"), answer)
            assertFalse(answer.contains("day 1,"), answer)
            assertFalse(answer.contains("day 2,"), answer)
            val sources = events.filterIsInstance<RagStreamEvent.Sources>().single()
            assertEquals(listOf(41L, 42L), sources.sources.map { it.recordId })
        }

    @Test
    fun `given breeding timing question then answer uses breeding card date without model`() =
        runTest {
            every { searchRepositoryMock.search(any(), any(), any(), any()) } returns emptyList()
            val repos = FakeAnalysisRepos()
            repos.patients.patients = listOf(testPatient(1, "Descarada"))
            repos.gestations.entries =
                listOf(
                    testGestation(
                        id = 44,
                        patientId = 1,
                        breedingDate = LocalDate(2025, 4, 1),
                        expectedDueDate = LocalDate(2026, 3, 7),
                    ),
                )

            val query = "How long ago was Descarada bred?"
            val events =
                sut(
                    analysisContextBuilder = repos.builder,
                    patientRepository = repos.patients,
                    today = LocalDate(2025, 5, 11),
                )(query).toList()

            assertEquals(0, engine.calls)
            val answer = events.filterIsInstance<RagStreamEvent.Chunk>().last().text
            assertTrue(answer.contains("bred on 1 Apr 2025"), answer)
            assertTrue(answer.contains("40 days ago"), answer)
            assertTrue(answer.contains("active"), answer)
            assertEquals(
                listOf(44L),
                events
                    .filterIsInstance<RagStreamEvent.Sources>()
                    .single()
                    .sources
                    .map { it.recordId },
            )
        }

    @Test
    fun `given breeding timing question then reproduction card wins over gestation date`() =
        runTest {
            every { searchRepositoryMock.search(any(), any(), any(), any()) } returns emptyList()
            val repos = FakeAnalysisRepos()
            repos.patients.patients = listOf(testPatient(1, "Descarada"))
            repos.reproductions.entries =
                listOf(
                    testReproductionEvent(
                        id = 61,
                        patientId = 1,
                        eventType = "Pregnancy Check",
                        date = LocalDate(2025, 4, 20),
                    ),
                    testReproductionEvent(
                        id = 60,
                        patientId = 1,
                        date = LocalDate(2025, 4, 1),
                    ),
                )
            repos.gestations.entries =
                listOf(
                    testGestation(
                        id = 44,
                        patientId = 1,
                        breedingDate = LocalDate(2025, 4, 2),
                        expectedDueDate = LocalDate(2026, 3, 8),
                    ),
                )

            val query = "How long ago was Descarada bred?"
            val events =
                sut(
                    analysisContextBuilder = repos.builder,
                    patientRepository = repos.patients,
                    today = LocalDate(2025, 5, 11),
                )(query).toList()

            assertEquals(0, engine.calls)
            val answer = events.filterIsInstance<RagStreamEvent.Chunk>().last().text
            assertTrue(answer.contains("bred on 1 Apr 2025"), answer)
            assertTrue(answer.contains("40 days ago"), answer)
            assertEquals(
                listOf(60L),
                events
                    .filterIsInstance<RagStreamEvent.Sources>()
                    .single()
                    .sources
                    .map { it.recordId },
            )
            assertEquals(
                listOf("REPRODUCTION_EVENT"),
                events
                    .filterIsInstance<RagStreamEvent.Sources>()
                    .single()
                    .sources
                    .map { it.recordType },
            )
        }

    @Test
    fun `given stallion question then answer uses reproduction card field without model`() =
        runTest {
            every { searchRepositoryMock.search(any(), any(), any(), any()) } returns emptyList()
            val repos = FakeAnalysisRepos()
            repos.patients.patients = listOf(testPatient(1, "Lua do Pinhal"))
            repos.reproductions.entries =
                listOf(
                    testReproductionEvent(
                        id = 62,
                        patientId = 1,
                        date = LocalDate(2025, 4, 1),
                        stallionName = "Eclipse",
                        breedingType = "Fresh cooled",
                    ),
                )

            val events =
                sut(
                    analysisContextBuilder = repos.builder,
                    patientRepository = repos.patients,
                    today = LocalDate(2025, 5, 11),
                )("Which stallion was used to breed Lua do Pinhal?").toList()

            assertEquals(0, engine.calls)
            val answer = events.filterIsInstance<RagStreamEvent.Chunk>().last().text
            assertTrue(answer.contains("Eclipse"), answer)
            assertTrue(answer.contains("1 Apr 2025"), answer)
            assertEquals(
                listOf(62L),
                events
                    .filterIsInstance<RagStreamEvent.Sources>()
                    .single()
                    .sources
                    .map { it.recordId },
            )
        }

    @Test
    fun `given failed gestation question then answer does not invent an active pregnancy`() =
        runTest {
            every { searchRepositoryMock.search(any(), any(), any(), any()) } returns emptyList()
            val repos = FakeAnalysisRepos()
            repos.patients.patients = listOf(testPatient(4, "Brisa"))
            repos.gestations.entries =
                listOf(
                    testGestation(
                        id = 43,
                        patientId = 4,
                        breedingDate = LocalDate(2025, 1, 1),
                        expectedDueDate = LocalDate(2000, 1, 1),
                        status = "Failed",
                    ),
                )

            val events =
                sut(
                    analysisContextBuilder = repos.builder,
                    patientRepository = repos.patients,
                    today = LocalDate(2025, 5, 11),
                )("Is Brisa pregnant?").toList()

            assertEquals(0, engine.calls)
            val answer = events.filterIsInstance<RagStreamEvent.Chunk>().last().text
            assertEquals("I couldn't find an active pregnancy recorded for Brisa.", answer)
            assertEquals(
                43L,
                events
                    .filterIsInstance<RagStreamEvent.Sources>()
                    .single()
                    .sources
                    .single()
                    .recordId,
            )
        }

    @Test
    fun `given breeding outcome question then reproduction timeline is emitted exactly`() =
        runTest {
            every { searchRepositoryMock.search(any(), any(), any(), any()) } returns emptyList()
            val repos = FakeAnalysisRepos()
            repos.patients.patients = listOf(testPatient(4, "Brisa"))
            repos.reproductions.entries =
                listOf(
                    testReproductionEvent(
                        id = 70,
                        patientId = 4,
                        eventType = "Pregnancy Check",
                        date = LocalDate(2025, 5, 12),
                        details = "Negative; no conceptus visualised",
                    ),
                    testReproductionEvent(
                        id = 71,
                        patientId = 4,
                        eventType = "Heat",
                        date = LocalDate(2025, 7, 20),
                        details = "Cycle resumed; no breeding performed this cycle",
                    ),
                )

            val events =
                sut(
                    analysisContextBuilder = repos.builder,
                    patientRepository = repos.patients,
                    today = LocalDate(2025, 8, 1),
                )("What is Brisa's breeding outcome?").toList()

            assertEquals(0, engine.calls)
            val answer = events.filterIsInstance<RagStreamEvent.Chunk>().last().text
            assertTrue(answer.contains("Pregnancy Check"), answer)
            assertTrue(answer.contains("Negative; no conceptus visualised"), answer)
            assertTrue(answer.contains("Cycle resumed; no breeding performed this cycle"), answer)
            assertEquals(
                listOf(70L, 71L),
                events
                    .filterIsInstance<RagStreamEvent.Sources>()
                    .single()
                    .sources
                    .map { it.recordId },
            )
        }

    @Test
    fun `given portuguese current gestation question then answer mirrors the language`() =
        runTest {
            every { searchRepositoryMock.search(any(), any(), any(), any()) } returns emptyList()
            val repos = FakeAnalysisRepos()
            repos.patients.patients = listOf(testPatient(1, "Lua"))
            repos.gestations.entries =
                listOf(
                    testGestation(
                        id = 41,
                        patientId = 1,
                        breedingDate = LocalDate(2025, 1, 1),
                        expectedDueDate = LocalDate(2000, 1, 1),
                    ),
                )

            val events =
                sut(
                    analysisContextBuilder = repos.builder,
                    patientRepository = repos.patients,
                    today = LocalDate(2025, 5, 11),
                )("Que éguas estão prenhes e em que dia de gestação?").toList()

            assertEquals(0, engine.calls)
            val answer = events.filterIsInstance<RagStreamEvent.Chunk>().last().text
            assertTrue(answer.contains("Lua tem uma gestação ativa registada"), answer)
            assertTrue(answer.contains("dia 130"), answer)
            assertTrue(answer.contains("parto previsto"), answer)
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
