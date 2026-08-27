@file:Suppress("TooManyFunctions")

package com.github.rodrigotimoteo.animally.llm

import com.github.rodrigotimoteo.animally.domain.common.RecordType
import com.github.rodrigotimoteo.animally.domain.patient.IPatientRepository
import com.github.rodrigotimoteo.animally.domain.search.model.SearchResult
import com.github.rodrigotimoteo.animally.domain.search.usecase.RetrievalPolicy
import com.github.rodrigotimoteo.animally.domain.search.usecase.SearchUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.math.ceil
import kotlin.time.Clock

/**
 * Narrow generation seam so the RAG pipeline can be tested without the
 * platform [LlmEngine] expect class (which cannot be instantiated or faked
 * from common code). Production wiring adapts [LlmEngine] to this in
 * LlmModule.
 */
fun interface RagLlmEngine {
    fun generate(
        prompt: String,
        instructions: String,
    ): Flow<String>

    /**
     * Streaming variant emitting CUMULATIVE text (full response so far per value).
     * Defaults to a single-emission stream over [generate] so existing fakes keep
     * working unchanged.
     */
    fun generateStreaming(
        prompt: String,
        instructions: String,
    ): Flow<String> = generate(prompt, instructions)
}

/** Whether the strict on-device policy should stop before an ungrounded model call. */
private fun shouldUseNoResultsFallback(
    policy: RagQueryPolicy,
    grounded: Boolean,
    historyRelevant: Boolean,
): Boolean = !policy.allowGeneralQuestions && !grounded && !historyRelevant

/** Record or computed-summary grounding used by the Foundation Models gate. */
private fun hasGrounding(
    query: String,
    selectedIndices: List<Int>,
    results: List<SearchResult>,
    deterministicSummary: String?,
    dateRange: RagDateRange?,
): Boolean {
    val expectedTypes = RecordTypeIntent.expectedRecordTypes(query)
    return when {
        deterministicSummary != null && summarySupportsQueryTypes(deterministicSummary, expectedTypes) -> true
        selectedIndices.isEmpty() -> false
        else ->
            selectedIndices.any { index ->
                val result = results[index]
                val dateMatches = dateRange == null || result.date?.let(dateRange::contains) == true
                val typeMatches = expectedTypes.isEmpty() || result.recordType in expectedTypes
                dateMatches && typeMatches
            }
    }
}

private fun summarySupportsQueryTypes(
    summary: String,
    expectedTypes: Set<String>,
): Boolean = expectedTypes.isEmpty() || expectedTypes.any { summarySupportsRecordType(summary, it) }

/**
 * A computed summary only grounds the record kind it actually contains. For
 * example, a care-count summary can mention deworming and farrier rows while
 * still having no vaccination evidence; it must not unlock a vaccination
 * answer merely because some summary exists.
 */
private fun summarySupportsRecordType(
    summary: String,
    recordType: String,
): Boolean {
    val lowered = summary.lowercase()
    return when (recordType) {
        "VACCINATION" -> "vaccination" in lowered
        "DEWORMING" -> "deworming" in lowered
        "FARRIER_VISIT" -> "farrier visit" in lowered
        "GESTATION" -> "gestations:" in lowered || "gestation " in lowered
        "WEIGHT" -> "weight " in lowered
        else -> false
    }
}

private fun hasRelevantHistory(
    query: String,
    recentConversation: String,
): Boolean = recentConversation.isNotEmpty() && RecordTypeIntent.sharesContentToken(query, recentConversation)

private fun canUseHistoryAsGrounding(
    query: String,
    recordQuestion: Boolean,
    dateRange: RagDateRange?,
    historyRelevant: Boolean,
): Boolean {
    if (!historyRelevant || !recordQuestion) return historyRelevant
    val asksForTypedRecord = RecordTypeIntent.expectedRecordTypes(query).isNotEmpty()
    val asksForDatedActivity = dateRange != null && RecentActivityIntent.matches(query, dateRange)
    return !asksForTypedRecord && !asksForDatedActivity
}

@Suppress("LongParameterList")
private fun shouldUseHonestFallback(
    query: String,
    recordQuestion: Boolean,
    dateRange: RagDateRange?,
    historyRelevant: Boolean,
    policy: RagQueryPolicy,
    grounded: Boolean,
): Boolean =
    shouldUseNoResultsFallback(
        policy,
        grounded,
        canUseHistoryAsGrounding(query, recordQuestion, dateRange, historyRelevant),
    )

private fun shouldUseAnalysisTools(
    query: String,
    toolCallingEngine: RagToolCallingEngine?,
    toolRegistry: RagToolRegistry?,
): Boolean =
    AnalysisIntents.requiresTools(query) &&
        toolCallingEngine?.supportsToolCalling == true &&
        toolRegistry?.definitions?.isNotEmpty() == true

/**
 * One prior conversational turn fed back into the prompt for multi-turn
 * context. Both sides are truncated by the use case before prompting.
 */
data class RagHistoryEntry(
    val question: String,
    val answer: String,
)

/**
 * Retrieval seam for the RAG pipeline. Receives an ALREADY FTS5-shaped
 * expression (from [AssistantPrompts.toFtsAndQuery] or
 * [AssistantPrompts.toFtsOrQuery]) and must pass it to the repository
 * untouched. Production wiring routes it to
 * [com.github.rodrigotimoteo.animally.domain.search.ISearchRepository.searchSnippets]
 * so retrieved chunks carry snippet windows instead of full record text;
 * [SearchUseCase] is deliberately bypassed because its tokenizer stars every
 * whitespace token, which corrupts boolean operators ("OR" -> "OR*", an FTS5
 * syntax error).
 */
fun interface RagRecordSearch {
    fun search(ftsQuery: String): List<SearchResult>

    /** Date-filtered FTS leg; the default keeps existing test seams compatible. */
    fun search(
        ftsQuery: String,
        from: LocalDate?,
        to: LocalDate?,
    ): List<SearchResult> = search(ftsQuery)

    /** Full dated-record leg used when a question contains no useful FTS terms. */
    fun searchByDateRange(
        from: LocalDate,
        to: LocalDate,
    ): List<SearchResult> = emptyList()
}

@Suppress("TooManyFunctions")
class GenerateRagResponseUseCase(
    private val searchUseCase: SearchUseCase,
    private val llmEngine: RagLlmEngine,
    private val config: RagConfig = RagConfig.DEFAULT,
    private val strings: AssistantStrings = EnAssistantStrings,
    private val recordSearch: RagRecordSearch? = null,
    private val patientRepository: IPatientRepository? = null,
    private val analysisContextBuilder: AnalysisContextBuilder? = null,
    private val today: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault()),
    private val queryPolicyProvider: suspend () -> RagQueryPolicy = { RagQueryPolicy.ON_DEVICE },
    private val toolCallingEngine: RagToolCallingEngine? = null,
    private val toolRegistry: RagToolRegistry? = null,
) {
    private data class StreamAnswerRequest(
        val context: String,
        val turnStrings: AssistantStrings,
        val selected: List<String>,
        val contextResults: List<SearchResult>,
        val usedDeterministicSummary: Boolean,
        val allowGeneralQuestions: Boolean,
        val useTools: Boolean,
    )

    private data class PatientScope(
        val name: String?,
        val requiresFilter: Boolean,
        val nameMentioned: Boolean,
    )

    /** Rough token estimate: ~4 characters per token (see RAG budget in CONTEXT docs). */
    private companion object {
        const val CHARS_PER_TOKEN = 4.0

        // Markdown link: [any text without ]]( any url without ) )
        val linkRegex = Regex("\\[([^\\]]*)]\\(([^)]*)\\)")

        const val MIN_QUERY_CHARS = 2

        // Multi-turn context: keep at most this many prior Q/A pairs and
        // truncate each side so one verbose turn cannot eat the budget.
        const val MAX_HISTORY_ENTRIES = 3
        const val MAX_HISTORY_SIDE_CHARS = 200

        // Patient-name scoping: tokens shorter than this never count as name
        // prefixes (a single letter would prefix-match unrelated names).
        const val MIN_NAME_PREFIX_CHARS = 2

        private val PATIENT_SCOPE_STOP_WORDS =
            setOf(
                "what",
                "when",
                "which",
                "who",
                "how",
                "why",
                "where",
                "did",
                "do",
                "does",
                "is",
                "are",
                "was",
                "were",
                "can",
                "could",
                "would",
                "should",
                "the",
                "a",
                "an",
                "of",
                "for",
                "to",
                "in",
                "on",
                "at",
                "and",
                "or",
                "any",
                "have",
                "has",
                "had",
                "my",
                "our",
                "your",
                "this",
                "that",
                "these",
                "those",
                "tell",
                "me",
                "about",
                "please",
                "patient",
                "patients",
                "horse",
                "horses",
                "mare",
                "mares",
                "cavalo",
                "cavalos",
                "égua",
                "éguas",
                "paciente",
                "pacientes",
                "o",
                "os",
                "as",
                "um",
                "uma",
                "uns",
                "umas",
                "que",
                "foi",
                "são",
                "sao",
                "não",
                "nao",
                "há",
                "ha",
                "do",
                "da",
                "dos",
                "das",
                "em",
                "com",
                "para",
                "por",
                "como",
                "porque",
                "porquê",
                "tenho",
                "temos",
                "está",
                "esta",
                "é",
                "e",
                "aconteceu",
                "ocorreu",
                "pregnant",
                "pregnancy",
                "gestation",
                "vaccination",
                "vaccinations",
                "vaccine",
                "booster",
                "farrier",
                "visit",
                "visits",
                "deworming",
                "weight",
                "ultrasound",
                "latest",
                "last",
                "previous",
                "recent",
                "record",
                "records",
                "treatment",
                "treatments",
                "received",
                "given",
                "happened",
                "occurred",
                "activity",
                "month",
                "week",
                "year",
                "este",
                "esta",
                "neste",
                "nesta",
                "mês",
                "mes",
                "semana",
                "ano",
                "hoje",
                "ontem",
                "quando",
                "qual",
                "quais",
                "quantos",
                "quantas",
                "último",
                "última",
                "ultimo",
                "ultima",
                "recente",
                "recentes",
                "registo",
                "registos",
            )

        // Model sometimes regurgitates prompt scaffolding (--- separators,
        // "Question: ..." echoes). Stripped defensively from every chunk.
        val scaffoldLineRegex = Regex("(?m)^\\s*(?:-{3,}|Question:.*|Context:.*|You are .*)\\s*\\n?")

        // Bracketed citation header in the final answer text: [TYPE #id].
        val citationRegex = Regex("\\[([A-Z_]+) #(\\d+)]")

        // Literal non-record citation tags: the computed-facts tag, the
        // system prompt's FORMAT placeholder ([RECORD_TYPE #ID] - "ID" is
        // not digits, so citationRegex cannot catch it), and headings from
        // deterministic analysis context. Small models sometimes echo these
        // internal labels verbatim; none may reach the user-facing bubble.
        val literalTagRegex =
            Regex(
                """\[(?:Summary|RECORD_TYPE #ID|PATIENT CENSUS|CARE COUNTS|GESTATIONS|OVERDUE CARE[^]]*)]""",
            )

        // Whitespace damage left behind by a stripped citation: doubled
        // spaces, a space before punctuation ("in ." -> "in."), line-leading
        // spaces, and blank-line runs where a standalone citation line sat.
        val multiSpaceRegex = Regex("[ \\t]{2,}")
        val spaceBeforePunctuationRegex = Regex("[ \\t]+([.,;:!?])")
        val spacedRepeatedPunctuationRegex = Regex("([.!?])([ \\t]+\\1)+")
        val lineLeadingSpaceRegex = Regex("(?m)^[ \\t]+")
        val blankLineRunRegex = Regex("\\n{3,}")

        // Citation-enforcement fallback caps appended headers: a ten-record
        // answer must not gain ten noise lines when the model cites nothing.
        const val MAX_ENFORCED_SOURCES = 3
        const val MAX_RECENT_ACTIVITY_ROWS = 12
        const val MAX_ACTIVITY_DETAIL_CHARS = 180

        /** Human-readable month abbreviations for chunk/TODAY dates (locale-independent). */
        val MONTH_ABBREVIATIONS =
            listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

        /** Renders a date as "14 Mar 2026" (locale-independent, model-friendly). */
        fun formatHumanDate(date: LocalDate): String {
            val month = MONTH_ABBREVIATIONS[date.month.ordinal]
            return "${date.day} $month ${date.year}"
        }
    }

    /**
     * Asks [query] against the record corpus, optionally grounded in
     * [history] (prior Q/A pairs, most recent last). Emits [RagStreamEvent]s:
     * cumulative sanitized [chunks][RagStreamEvent.Chunk], one
     * [sources][RagStreamEvent.Sources] event after the final chunk (records
     * actually cited), and an [interruption][RagStreamEvent.Interrupted]
     * marker when the stream fails mid-emission. User cancellation still
     * propagates as [CancellationException].
     */
    operator fun invoke(
        query: String,
        history: List<RagHistoryEntry> = emptyList(),
    ): Flow<RagStreamEvent> =
        flow {
            // Language mirroring: the device locale picks the default strings,
            // but a Portuguese question gets a Portuguese turn even on an EN
            // device - answering "Quantos pacientes tenho?" in English reads
            // as broken to a PT-speaking vet.
            val turnStrings =
                if (AssistantPrompts.isPortugueseQuery(query)) PtAssistantStrings else strings
            // Greetings/small talk never touch retrieval - searching "hi" in
            // veterinary records and answering with the no-results fallback
            // reads as broken to the user.
            AssistantPrompts.greetingReply(query, turnStrings)?.let {
                emit(RagStreamEvent.Chunk(it))
                return@flow
            }
            val queryPolicy = queryPolicyProvider()
            val enriched = AssistantPrompts.enrichQuery(query)
            if (enriched.length < MIN_QUERY_CHARS) {
                // One-letter queries fuzzy-match nonsense ("A" hits any name
                // containing A); asking for more beats answering garbage.
                emit(RagStreamEvent.Chunk(turnStrings.tooShortReply))
                return@flow
            }
            // Immediate feedback: retrieval runs before the first model
            // emission, so without this line the user stares at nothing.
            // Consumers replace their buffer with each chunk, so this
            // placeholder is overwritten by the real answer (or fallback).
            emit(RagStreamEvent.Chunk(turnStrings.searchingPlaceholder))
            emitAnswer(
                query = query,
                enriched = enriched,
                history = history,
                turnStrings = turnStrings,
                queryPolicy = queryPolicy,
            )
        }

    @Suppress("LongMethod")
    private suspend fun FlowCollector<RagStreamEvent>.emitAnswer(
        query: String,
        enriched: String,
        history: List<RagHistoryEntry>,
        turnStrings: AssistantStrings,
        queryPolicy: RagQueryPolicy,
    ) {
        val dateRange = RagDateRangeIntent.resolve(query, today)
        val patientScope = resolvePatientScope(query)
        val scopedPatient = patientScope.name
        val recordQuestion =
            RecordQuestionIntent.isRecordQuestion(
                query,
                scopedPatient,
                dateRange,
                patientNameMentioned = patientScope.nameMentioned,
            )
        val results =
            restrictResults(
                retrieve(query, enriched, dateRange),
                scopedPatient,
                dateRange,
                patientScope.requiresFilter,
            )
        // Dosage guardrail: a how-much-drug question answered without any
        // medication record in context must be refused deterministically -
        // a small model with no grounding will hallucinate a dose. Checked
        // BEFORE deterministic/model paths so prior conversation alone can
        // never unlock dosage advice.
        if (DosageGuard.isDosageIntent(query) && results.none(::isMedicationRecord)) {
            emit(RagStreamEvent.Chunk(turnStrings.dosageRefusal))
            return
        }
        val deterministicHandled =
            (recordQuestion && emitCurrentGestationAnswer(query, scopedPatient)) ||
                emitDeterministicAnswer(query, results, scopedPatient, dateRange, turnStrings)
        if (deterministicHandled) {
            return
        }

        val deterministicSummary = analysisContextBuilder?.build(query, today)
        val recentConversation = formatHistory(history)
        val chunks = results.map(::formatChunk)
        val reservedTokens =
            estimateTokens(recentConversation) + estimateTokens(deterministicSummary.orEmpty())
        val selectedIndices =
            selectWithinBudget(
                chunks,
                maxContextTokens = queryPolicy.maxContextTokens ?: config.maxContextTokens,
                reservedTokens = reservedTokens,
            )
        val historyRelevant = hasRelevantHistory(query, recentConversation)
        val grounded = hasGrounding(query, selectedIndices, results, deterministicSummary, dateRange)
        val useTools = shouldUseAnalysisTools(query, toolCallingEngine, toolRegistry)
        val effectiveAllowGeneralQuestions = !recordQuestion && (queryPolicy.allowGeneralQuestions || useTools)
        val effectiveGrounding = grounded || (recordQuestion && useTools)
        val fallbackPolicy = queryPolicy.copy(allowGeneralQuestions = effectiveAllowGeneralQuestions)
        if (
            shouldUseHonestFallback(
                query = query,
                recordQuestion = recordQuestion,
                dateRange = dateRange,
                historyRelevant = historyRelevant,
                policy = fallbackPolicy,
                grounded = effectiveGrounding,
            )
        ) {
            emit(RagStreamEvent.Chunk(turnStrings.noResultsFallback))
            return
        }
        val selected = selectedIndices.map(chunks::get)
        streamAnswer(
            StreamAnswerRequest(
                context = buildContext(selected, query, recentConversation, deterministicSummary),
                turnStrings = turnStrings,
                selected = selected,
                contextResults = selectedIndices.map(results::get),
                usedDeterministicSummary = deterministicSummary != null,
                allowGeneralQuestions = effectiveAllowGeneralQuestions,
                useTools = useTools,
            ),
        )
    }

    /** Handles answers that are safer as direct projections of stored data. */
    private suspend fun FlowCollector<RagStreamEvent>.emitDeterministicAnswer(
        query: String,
        results: List<SearchResult>,
        scopedPatient: String?,
        dateRange: RagDateRange?,
        turnStrings: AssistantStrings,
    ): Boolean {
        // "When was the last <type> visit?" is answered from the retrieved
        // record dates so a model cannot drift to a plausible but false date.
        if (emitLatestRecordAnswer(query, results, scopedPatient)) return true
        return RecentActivityIntent.matches(query, dateRange) &&
            emitRecentActivityAnswer(results, dateRange, turnStrings)
    }

    /** Emits live pregnancy facts before any model can recalculate or invent them. */
    private suspend fun FlowCollector<RagStreamEvent>.emitCurrentGestationAnswer(
        query: String,
        scopedPatient: String?,
    ): Boolean {
        val facts = analysisContextBuilder?.gestationFacts(query, today) ?: return false
        emitGestationAnswer(query, facts, scopedPatient)
        return true
    }

    /**
     * Streams the model answer for [context], then applies citation
     * enforcement and emits the cited-sources event. A mid-stream failure
     * (anything but user cancellation) becomes an
     * [RagStreamEvent.Interrupted] marker carrying the partial text instead
     * of tearing down the whole turn.
     */
    @Suppress("CyclomaticComplexMethod")
    private suspend fun FlowCollector<RagStreamEvent>.streamAnswer(request: StreamAnswerRequest) {
        // Streaming emits cumulative snapshots; sanitize() is idempotent, so
        // re-sanitizing the growing text each step is safe and downstream
        // consumers replace their buffer with each emission.
        var lastEmitted = ""
        var toolSources = emptyList<SearchResult>()
        try {
            val answer = streamModelAnswer(request) { text -> lastEmitted = text }
            if (answer.fallbackToPlainText) {
                lastEmitted = streamPlainText(request) { text -> lastEmitted = text }
            } else {
                lastEmitted = answer.text
                toolSources = answer.sources
            }
        } catch (ce: CancellationException) {
            throw ce
        } catch (t: Throwable) {
            // Mid-stream failure (engine error, not user cancellation):
            // surface a typed marker carrying the partial text so the UI
            // can offer a retry instead of showing a dead bubble.
            emit(RagStreamEvent.Interrupted(partialText = lastEmitted, error = t.message))
            return
        }
        // Snapshot BEFORE enforcement: appended citation headers below exist
        // for the Sources channel only - the bubble must never show them
        // (nor their bracket-stripped residue).
        val streamedText = lastEmitted
        // Citation enforcement: the system prompt mandates citing bracketed
        // headers, but the model skips them often enough that the guarantee
        // is enforced here - when records were used and the reply carries
        // none, append the ACTUAL retrieved headers (never invented ones).
        // The trigger is MAPPED citations, not bare "[": a fabricated or
        // stale header the model invented ([Giraffe #1], a deleted id)
        // satisfies the eye but maps to no source card, so the guarantee
        // needs the real headers appended anyway.
        val allContextResults = (request.contextResults + toolSources).distinctBy { it.recordType to it.recordId }
        var mappedCitations = citedResults(lastEmitted, allContextResults)
        if (mappedCitations.isEmpty()) {
            // Prefer tool-backed headers because they identify the authoritative
            // database rows used by an analysis. Fall back to retrieval headers
            // for ordinary RAG answers; cap both paths to avoid citation noise.
            val candidateSources =
                if (toolSources.isNotEmpty()) {
                    toolSources.map(::sourceHeader)
                } else {
                    request.selected.mapNotNull(::sourceHeader)
                }
            val sources =
                candidateSources.distinct().take(MAX_ENFORCED_SOURCES)
            if (sources.isNotEmpty()) {
                lastEmitted =
                    listOf(lastEmitted.takeIf(String::isNotBlank), sources.joinToString("\n"))
                        .filterNotNull()
                        .joinToString("\n\n")
                mappedCitations = citedResults(lastEmitted, allContextResults)
                emit(RagStreamEvent.Chunk(lastEmitted))
            }
        }
        // Summary-only answers: when retrieval came back empty but the
        // deterministic summary carried the facts, there are no record
        // headers to append - and an uncited confident answer is exactly
        // what the citation guarantee forbids. The system prompt tells the
        // model to cite the summary as [Summary]; when it skips that too,
        // the tag is enforced here. The guard keys on the LITERAL [Summary]
        // tag, not bare "[": a fabricated bracket the model invented
        // ([Giraffe #1]) satisfies the eye but maps to no source card, so it
        // must not block the append either.
        if (request.usedDeterministicSummary && mappedCitations.isEmpty() && "[Summary]" !in lastEmitted) {
            lastEmitted =
                listOf(lastEmitted.takeIf(String::isNotBlank), "[Summary]")
                    .filterNotNull()
                    .joinToString("\n\n")
            emit(RagStreamEvent.Chunk(lastEmitted))
        }
        citedResults(lastEmitted, allContextResults).takeIf { it.isNotEmpty() }?.let {
            emit(RagStreamEvent.Sources(it))
        }
        // Display split: citations are parsed from the bracketed text FIRST
        // (above), then the bubble is re-emitted from the PRE-enforcement
        // snapshot with every citation token stripped - mapped, fabricated,
        // and prompt-placeholder brackets alike. The references live on as
        // Sources chips; the prose carries none of them.
        val displayText = stripCitationTokens(streamedText)
        if (displayText != lastEmitted) {
            lastEmitted = displayText
            emit(RagStreamEvent.Chunk(lastEmitted))
        }
    }

    /** Streams a normal text-only model request and returns its final snapshot. */
    private suspend fun FlowCollector<RagStreamEvent>.streamPlainText(
        request: StreamAnswerRequest,
        onText: (String) -> Unit = {},
    ): String {
        var lastEmitted = ""
        llmEngine
            .generateStreaming(
                request.context,
                AssistantPrompts.systemPrompt(
                    request.turnStrings,
                    allowGeneralQuestions = request.allowGeneralQuestions,
                ),
            ).collect { text ->
                lastEmitted = sanitize(text)
                onText(lastEmitted)
                emit(RagStreamEvent.Chunk(lastEmitted))
            }
        return lastEmitted
    }

    /** Streams either a normal answer or a bounded native-tool answer. */
    private suspend fun FlowCollector<RagStreamEvent>.streamModelAnswer(
        request: StreamAnswerRequest,
        onText: (String) -> Unit = {},
    ): RagToolAnswer {
        val toolAnswer =
            if (request.useTools) {
                val engine = toolCallingEngine
                val registry = toolRegistry
                if (engine == null || registry == null) {
                    null
                } else {
                    val systemPrompt =
                        AssistantPrompts.systemPrompt(
                            request.turnStrings,
                            allowGeneralQuestions = request.allowGeneralQuestions,
                        ) +
                            "\nUse the read-only analysis tools when they improve accuracy. " +
                            "Tool results are authoritative for this app's data. Never invent a source header; " +
                            "when a tool result includes a source field, cite that exact [TYPE #ID] value."
                    val messages =
                        mutableListOf(
                            RagChatMessage(RagChatRole.SYSTEM, content = systemPrompt),
                            RagChatMessage(RagChatRole.USER, content = request.context),
                        )
                    RagToolCallingCoordinator(
                        engine = engine,
                        registry = registry,
                        turnStrings = request.turnStrings,
                        sanitize = ::sanitize,
                        onText = onText,
                        emitChunk = { chunk -> emit(RagStreamEvent.Chunk(chunk)) },
                    ).run(messages)
                }
            } else {
                null
            }
        return toolAnswer ?: RagToolAnswer(streamPlainText(request, onText), emptyList())
    }

    /**
     * Two-leg retrieval mirroring the production contract: a strict AND query
     * over the filler-stripped question first, then one broad OR retry (with
     * synonym expansion) when the AND leg is EMPTY or WEAK (fewer than
     * [RetrievalPolicy.WEAK_RESULT_THRESHOLD] records) — AND semantics require
     * every content word to match, so natural questions like "which patients
     * belong to X" would otherwise return nothing, and a single lucky hit used
     * to suppress the recall-fixing retry. Retry hits are deduplicated against
     * the AND leg by record identity and appended after it. The caller applies
     * patient/date boundaries to the MERGED list, so a scoped-patient record
     * recovered by the retry cannot be mixed with rows belonging to another
     * patient or period.
     *
     * When no [recordSearch] seam is wired, falls back to [SearchUseCase]
     * with full-text snippets.
     */
    private fun retrieve(
        query: String,
        enriched: String,
        dateRange: RagDateRange?,
    ): List<SearchResult> {
        val seam =
            recordSearch
                ?: return searchUseCase(
                    enriched,
                    from = dateRange?.from,
                    to = dateRange?.to,
                    recordTypes = null,
                )
        val andQuery = AssistantPrompts.toFtsAndQuery(enriched)
        if (dateRange != null && andQuery.isBlank()) {
            return seam.searchByDateRange(dateRange.from, dateRange.to)
        }
        val andResults = seam.search(andQuery, dateRange?.from, dateRange?.to)
        return RetrievalPolicy.mergeWeakRetry(andResults) {
            seam.search(AssistantPrompts.toFtsOrQuery(query), dateRange?.from, dateRange?.to)
        }
    }

    /**
     * Applies patient and date boundaries after retrieval as a second line of
     * defence. The database query applies the same date bounds in production,
     * but keeping this invariant here protects alternate/test search seams.
     */
    private fun restrictResults(
        results: List<SearchResult>,
        scopedPatient: String?,
        dateRange: RagDateRange?,
        requiresPatientFilter: Boolean,
    ): List<SearchResult> =
        if (requiresPatientFilter && scopedPatient == null) {
            // Test/alternate seams may not have an IPatientRepository. A
            // pronoun can still be safely resolved when retrieval itself has
            // returned records for exactly one patient; never allow a mixed
            // result set to cross patient boundaries without an explicit
            // scope.
            results
                .takeIf { rows -> rows.map(SearchResult::patientId).distinct().size <= 1 }
                .orEmpty()
        } else {
            results.filter { result ->
                val patientMatches = scopedPatient == null || result.patientName.lowercase() == scopedPatient
                val dateMatches = dateRange == null || result.date?.let(dateRange::contains) == true
                patientMatches && dateMatches
            }
        }

    /** Resolves a unique patient, or marks the result set unsafe to share. */
    private fun resolvePatientScope(query: String): PatientScope {
        val activeNames = patientRepository?.patientNames().orEmpty()
        val tokens = patientScopeTokens(query)
        val matchedNames =
            activeNames.filter { name ->
                val lowered = name.lowercase()
                tokens.any { token -> lowered.startsWith(token) }
            }
        val hasIndividualReference = RecordQuestionIntent.hasIndividualPatientReference(query)
        val hasLikelyName =
            patientRepository != null && RecordQuestionIntent.hasLikelyNamedPatientReference(query)
        val name =
            when {
                matchedNames.size == 1 -> matchedNames.single().lowercase()
                matchedNames.isEmpty() && hasIndividualReference && activeNames.size == 1 ->
                    activeNames.single().lowercase()
                else -> null
            }
        return PatientScope(
            name = name,
            requiresFilter = matchedNames.isNotEmpty() || hasIndividualReference || hasLikelyName,
            nameMentioned = matchedNames.isNotEmpty() || hasLikelyName,
        )
    }

    private fun patientScopeTokens(query: String): Set<String> =
        query
            .split(Regex("\\s+"))
            .map(::cleanPatientToken)
            .filter { it.length >= MIN_NAME_PREFIX_CHARS && it !in PATIENT_SCOPE_STOP_WORDS }
            .toSet()

    private fun cleanPatientToken(token: String): String =
        token
            .trim('?', ',', '.', '!', ':', ';', '\'')
            .removeSuffix("'s")
            .removeSuffix("'S")
            .removeSuffix("’s")
            .removeSuffix("’S")
            .lowercase()

    /**
     * A date-only activity answer is already a database projection. Keeping it
     * deterministic means a provider cannot turn unrelated context into a
     * clinical story for a simple “what happened?” question.
     */
    private suspend fun FlowCollector<RagStreamEvent>.emitRecentActivityAnswer(
        results: List<SearchResult>,
        dateRange: RagDateRange?,
        turnStrings: AssistantStrings,
    ): Boolean {
        if (dateRange == null || results.isEmpty()) return false
        val visible = results.take(MAX_RECENT_ACTIVITY_ROWS)
        val omitted = results.size - visible.size
        val portuguese = turnStrings === PtAssistantStrings
        val period = "${formatHumanDate(dateRange.from)}–${formatHumanDate(dateRange.to)}"
        val heading =
            if (portuguese) {
                "Encontrei ${results.size} ${if (results.size == 1) "registo" else "registos"} " +
                    "no período $period:"
            } else {
                "I found ${results.size} record${if (results.size == 1) "" else "s"} " +
                    "between $period:"
            }
        val lines =
            visible.joinToString("\n") { result ->
                val date = result.date?.let(::formatHumanDate) ?: "unknown date"
                val type = recordTypeNoun(result.recordType, portuguese)
                val detail =
                    result.snippet
                        .replace(Regex("\\s+"), " ")
                        .trim()
                        .take(MAX_ACTIVITY_DETAIL_CHARS)
                "- $date — ${result.patientName}: $type" +
                    detail.takeIf(String::isNotBlank)?.let { " — $it" }.orEmpty()
            }
        val suffix =
            if (omitted > 0) {
                if (portuguese) {
                    "\n(E há mais $omitted registos neste período.)"
                } else {
                    "\n(There are $omitted more records in this period.)"
                }
            } else {
                ""
            }
        emit(RagStreamEvent.Chunk("$heading\n$lines$suffix"))
        emit(RagStreamEvent.Sources(visible))
        return true
    }

    /**
     * Formats one search hit as a citable source block. The bracketed header
     * carries record id and patient id so the model can cite precisely; the
     * system prompt tells the model these headers are source references.
     * Dates render humanized ("14 Mar 2026") - raw ISO strings read as noise
     * to the model and leak into answers verbatim. Snippets are capped at
     * [RagConfig.chunkCharCap] so one long record cannot dominate the budget.
     */
    private fun formatChunk(result: SearchResult): String {
        val date = result.date?.let(::formatHumanDate) ?: "unknown date"
        val breed = result.breed ?: "unknown breed"
        return buildString {
            val header = "[${result.recordType} #${result.recordId}] ${result.patientName} ($breed, $date)"
            appendLine("$header | patient #${result.patientId}")
            appendLine(result.snippet.take(config.chunkCharCap))
        }
    }

    /**
     * Keeps the INDICES of the chunks that fit the token budget after
     * reserving room for the system prompt, the query, the response, and
     * [reservedTokens] for any recent-conversation block. Indices (not
     * strings) so callers can map back to the originating [SearchResult]s
     * for source-card emission. An empty result means every chunk was
     * filtered out (or there were none).
     *
     * An individually oversized chunk is SKIPPED, not a stopping point: with
     * `break`, one huge record early in the ranking starved every smaller
     * relevant record behind it. Chunks are pre-capped by
     * [RagConfig.chunkCharCap] in [formatChunk], so skipping only fires when
     * the remaining budget is genuinely exhausted for that chunk.
     */
    private fun selectWithinBudget(
        chunks: List<String>,
        maxContextTokens: Int,
        reservedTokens: Int = 0,
    ): List<Int> {
        val reserve =
            config.systemReserveTokens + config.queryReserveTokens +
                config.responseReserveTokens + reservedTokens
        val budget = maxContextTokens - reserve
        val selected = mutableListOf<Int>()
        var used = 0
        for ((index, chunk) in chunks.withIndex()) {
            val est = ceil(chunk.length / CHARS_PER_TOKEN).toInt()
            if (used + est > budget) continue
            selected.add(index)
            used += est
        }
        return selected
    }

    /**
     * Renders prior Q/A pairs as a compact transcript block: at most
     * [MAX_HISTORY_ENTRIES] most-recent pairs, each side truncated to
     * [MAX_HISTORY_SIDE_CHARS]. Empty when there is no history.
     */
    private fun formatHistory(history: List<RagHistoryEntry>): String {
        val recent = history.takeLast(MAX_HISTORY_ENTRIES)
        if (recent.isEmpty()) return ""
        return buildString {
            appendLine("Recent conversation:")
            for (entry in recent) {
                append("User: ").appendLine(entry.question.take(MAX_HISTORY_SIDE_CHARS))
                append("Assistant: ").appendLine(entry.answer.take(MAX_HISTORY_SIDE_CHARS))
            }
        }.trimEnd()
    }

    private fun estimateTokens(text: String): Int = ceil(text.length / CHARS_PER_TOKEN).toInt()

    /**
     * Extracts the citable header ("[TYPE #id] Name") from a formatted chunk,
     * or null when the chunk has no header line. Used by the citation
     * enforcement fallback - only headers of chunks actually retrieved are
     * ever appended.
     */
    private fun sourceHeader(chunk: String): String? =
        chunk
            .lineSequence()
            .firstOrNull()
            ?.substringBefore(" |")
            ?.takeIf { it.startsWith("[") }

    /** Formats a tool-backed record as the same citation token used by RAG chunks. */
    private fun sourceHeader(result: SearchResult): String = "[${result.recordType} #${result.recordId}]"

    /**
     * Maps the `[TYPE #id]` citations actually present in [answerText] back
     * to their retrieved records, in citation order, deduplicated. Only
     * records that were selected into the context count - a citation naming
     * an unselected (or invented) record yields no source card.
     */
    private fun citedResults(
        answerText: String,
        contextResults: List<SearchResult>,
    ): List<SearchResult> {
        if (contextResults.isEmpty()) return emptyList()
        val byKey = contextResults.associateBy { "${it.recordType}#${it.recordId}" }
        return citationRegex
            .findAll(answerText)
            .mapNotNull { match -> byKey["${match.groupValues[1]}#${match.groupValues[2]}"] }
            .distinct()
            .toList()
    }

    /**
     * True when [result] is a medication-bearing record (prescription,
     * controlled substance, or repro medication) - the only grounding that
     * unlocks dosage questions past the guardrail.
     */
    private fun isMedicationRecord(result: SearchResult): Boolean =
        result.recordType == RecordType.Medication.wireName ||
            result.recordType == RecordType.ControlledSubstance.wireName ||
            result.recordType == RecordType.ReproMedication.wireName

    /**
     * Assembles the user-turn prompt: today's date first (so relative
     * questions like "is the Coggins still valid?" are answerable - kept in
     * the user turn, not the system prompt, so the reserve budget stays
     * stable), optional deterministic summary (computed facts the model must
     * never contradict), then optional recent conversation for multi-turn
     * context, retrieved context, and the raw question. Role/scope/citation
     * rules live in the system prompt and are passed as instructions, not
     * inline.
     */
    private fun buildContext(
        chunks: List<String>,
        query: String,
        recentConversation: String = "",
        deterministicSummary: String? = null,
    ): String {
        val prompt = StringBuilder()
        prompt.appendLine("TODAY IS ${formatHumanDate(today)}.")
        if (deterministicSummary != null) {
            prompt.appendLine(deterministicSummary)
            prompt.appendLine("---")
        }
        if (recentConversation.isNotEmpty()) {
            prompt.appendLine(recentConversation)
            prompt.appendLine("---")
        }
        prompt.appendLine("Context:")
        val sb = StringBuilder()
        for (chunk in chunks) {
            sb.appendLine(chunk)
        }
        prompt.appendLine(sb.toString().trimEnd())
        prompt.append("---")
        prompt.appendLine()
        prompt.append("Question: ").append(query)
        return prompt.toString()
    }

    /**
     * Removes citation tokens from answer text AFTER the Sources event has
     * been derived from them: the bubble renders prose only, while the
     * bracketed references live on as source-card chips. Mapped and
     * fabricated brackets are stripped alike, and the whitespace the removal
     * leaves behind is repaired (doubled spaces, space before punctuation,
     * orphaned blank lines).
     */
    private val stripCitationTokens: (String) -> String =
        { text ->
            text
                .replace(citationRegex, "")
                .replace(literalTagRegex, "")
                .replace(multiSpaceRegex, " ")
                .replace(spacedRepeatedPunctuationRegex, "$1")
                .replace(spaceBeforePunctuationRegex, "$1")
                .replace(lineLeadingSpaceRegex, "")
                .replace(blankLineRunRegex, "\n\n")
                .trim()
        }

    /**
     * Strips markdown the model was told not to produce but sometimes does:
     * bold markers (** and __), backticks, and [text](url) links reduced to
     * their text. Applied to every emitted chunk before it reaches the UI.
     */
    private fun sanitize(text: String): String =
        text
            .replace(scaffoldLineRegex, "")
            .replace(linkRegex, "$1")
            .replace("**", "")
            .replace("__", "")
            .replace("`", "")
            .replace(Regex("\\n{3,}"), "\n\n")
            .trim()
}
