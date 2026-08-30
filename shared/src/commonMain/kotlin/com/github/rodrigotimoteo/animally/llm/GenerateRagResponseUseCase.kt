@file:Suppress("TooManyFunctions")

package com.github.rodrigotimoteo.animally.llm

import com.github.rodrigotimoteo.animally.domain.owner.IOwnerRepository
import com.github.rodrigotimoteo.animally.domain.patient.IPatientRepository
import com.github.rodrigotimoteo.animally.domain.search.model.SearchResult
import com.github.rodrigotimoteo.animally.domain.search.usecase.SearchUseCase
import com.github.rodrigotimoteo.animally.domain.vetreference.VeterinaryWebQuery
import com.github.rodrigotimoteo.animally.domain.vetreference.VeterinaryWebSearchResult
import com.github.rodrigotimoteo.animally.domain.vetreference.VeterinaryWebSourceProvider
import com.github.rodrigotimoteo.animally.domain.vetreference.model.VeterinaryWebSource
import com.github.rodrigotimoteo.animally.llm.rag.AnswerIntent
import com.github.rodrigotimoteo.animally.llm.rag.LlmOrchestrator
import com.github.rodrigotimoteo.animally.llm.rag.PromptBuilder
import com.github.rodrigotimoteo.animally.llm.rag.RagRetriever
import com.github.rodrigotimoteo.animally.llm.rag.ResponsePolicy
import com.github.rodrigotimoteo.animally.llm.support.DateFormatting
import com.github.rodrigotimoteo.animally.llm.support.TokenEstimator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
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

    /**
     * Generates through a configured cloud-capable route when one is
     * available. Engines without a separate cloud route retain the normal
     * behavior, so existing fakes and local-only builds remain compatible.
     */
    fun generateCloudFirst(
        prompt: String,
        instructions: String,
    ): Flow<String> = generateStreaming(prompt, instructions)
}

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

// This facade owns the assistant turn boundary; keeping its routing helpers
// together preserves one atomic grounding decision per request.
@Suppress("TooManyFunctions", "LargeClass")
class GenerateRagResponseUseCase(
    private val searchUseCase: SearchUseCase,
    private val llmEngine: RagLlmEngine,
    private val config: RagConfig = RagConfig.DEFAULT,
    private val strings: AssistantStrings = EnAssistantStrings,
    private val recordSearch: RagRecordSearch? = null,
    private val patientRepository: IPatientRepository? = null,
    private val ownerRepository: IOwnerRepository? = null,
    private val analysisContextBuilder: AnalysisContextBuilder? = null,
    private val today: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault()),
    private val queryPolicyProvider: suspend () -> RagQueryPolicy = { RagQueryPolicy.ON_DEVICE },
    private val queryPolicyForQuestion: (suspend (String) -> RagQueryPolicy)? = null,
    private val toolCallingEngine: RagToolCallingEngine? = null,
    private val toolRegistry: RagToolRegistry? = null,
    private val webSourceProvider: VeterinaryWebSourceProvider? = null,
) {
    private data class AnswerContext(
        val deterministicSummary: String?,
        val recentConversation: String,
        val selected: List<String>,
        val contextResults: List<SearchResult>,
        val grounded: Boolean,
        val historyGrounding: Boolean,
        val useTools: Boolean,
        val webSources: List<VeterinaryWebSource>,
    )

    private data class ModelAnswerPlan(
        val request: RagStreamRequest?,
        val useFallback: Boolean,
    )

    private data class ModelAnswerInput(
        val query: String,
        val results: List<SearchResult>,
        val intent: AnswerIntent,
        val history: List<RagHistoryEntry>,
        val turnStrings: AssistantStrings,
        val queryPolicy: RagQueryPolicy,
        val webSources: List<VeterinaryWebSource>,
        val webReferencesUnavailable: Boolean,
    )

    private data class ModelAnswerRequest(
        val query: String,
        val results: List<SearchResult>,
        val intent: AnswerIntent,
        val history: List<RagHistoryEntry>,
        val turnStrings: AssistantStrings,
        val queryPolicy: RagQueryPolicy,
        val webReferencesUnavailable: Boolean,
    )

    private val retriever =
        RagRetriever(
            searchUseCase = searchUseCase,
            recordSearch = recordSearch,
            patientRepository = patientRepository,
            ownerRepository = ownerRepository,
            today = today,
        )

    private val promptBuilder =
        PromptBuilder(
            config = config,
            today = today,
        )

    private val orchestrator =
        LlmOrchestrator(
            llmEngine = llmEngine,
            toolCallingEngine = toolCallingEngine,
            toolRegistry = toolRegistry,
        )

    /** Rough token estimate: ~4 characters per token (see RAG budget in CONTEXT docs). */
    private companion object {
        const val MIN_QUERY_CHARS = 2

        // Patient-name scoping: tokens shorter than this never count as name
        // prefixes (a single letter would prefix-match unrelated names).
        const val MAX_RECENT_ACTIVITY_ROWS = 12
        const val MAX_ACTIVITY_DETAIL_CHARS = 180
        const val WEB_REFERENCE_TIMEOUT_MILLIS = 15_000L
    }

    /**
     * Asks [query] against the record corpus, optionally grounded in
     * [history] (prior Q/A pairs, most recent last). History provides
     * conversational continuity and follow-up scope, but never authorizes a
     * new patient fact by itself. Emits [RagStreamEvent]s:
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
            val queryPolicy = queryPolicyForQuestion?.invoke(query) ?: queryPolicyProvider()
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

    private suspend fun FlowCollector<RagStreamEvent>.emitAnswer(
        query: String,
        enriched: String,
        history: List<RagHistoryEntry>,
        turnStrings: AssistantStrings,
        queryPolicy: RagQueryPolicy,
    ) {
        val intent = retriever.classifyQuery(query, history)
        val results = retriever.retrieveRelevantResults(query, enriched, intent)
        // Dosage guardrail: a how-much-drug question answered without any
        // medication record in context must be refused deterministically -
        // a small model with no grounding will hallucinate a dose. Checked
        // BEFORE deterministic/model paths so prior conversation alone can
        // never unlock dosage advice.
        if (ResponsePolicy.isDosageRefusalNeeded(query, results)) {
            emit(RagStreamEvent.Chunk(turnStrings.dosageRefusal))
            return
        }
        if (
            emitDeterministicAnswer(
                query = query,
                results = results,
                intent = intent,
                turnStrings = turnStrings,
            )
        ) {
            return
        }
        emitModelAnswer(
            ModelAnswerRequest(
                query = query,
                results = results,
                intent = intent,
                history = history,
                turnStrings = turnStrings,
                queryPolicy = queryPolicy,
                webReferencesUnavailable = false,
            ),
        )
    }

    private suspend fun FlowCollector<RagStreamEvent>.emitModelAnswer(input: ModelAnswerRequest) {
        val webSources = findWebSources(input.query, input.intent, input.queryPolicy)
        val webReferencesUnavailable = webSources is VeterinaryWebSearchResult.Unavailable
        if (webReferencesUnavailable) {
            emit(RagStreamEvent.Chunk(input.turnStrings.webReferenceUnavailable))
            return
        }
        val webFallback =
            when (webSources) {
                VeterinaryWebSearchResult.Unavailable -> null
                is VeterinaryWebSearchResult.Success ->
                    input.turnStrings.webReferenceNoResults.takeIf { webSources.sources.isEmpty() }
                null -> null
            }
        if (webFallback != null) {
            emit(RagStreamEvent.Chunk(webFallback))
            return
        }
        val trustedWebSources =
            when (webSources) {
                is VeterinaryWebSearchResult.Success -> webSources.sources
                else -> emptyList()
            }
        val plan =
            prepareModelAnswer(
                ModelAnswerInput(
                    query = input.query,
                    results = input.results,
                    intent = input.intent,
                    history = input.history,
                    turnStrings = input.turnStrings,
                    queryPolicy = input.queryPolicy,
                    webSources = trustedWebSources,
                    webReferencesUnavailable = webReferencesUnavailable,
                ),
            )
        if (plan.useFallback) {
            emit(RagStreamEvent.Chunk(input.turnStrings.noResultsFallback))
        } else {
            plan.request?.let { orchestrator.stream(this, it) }
        }
    }

    private fun prepareModelAnswer(input: ModelAnswerInput): ModelAnswerPlan {
        val context = buildAnswerContext(input)
        val intent = input.intent
        val requiresGrounding = ResponsePolicy.requiresGrounding(intent.recordQuestion, intent.analysisQuery)
        val grounded = context.grounded || context.historyGrounding
        val canAttemptToolGrounding = intent.analysisQuery && context.useTools
        // A cloud policy permits general knowledge, not unsupported patient
        // facts. Tool availability is only a possible route to grounding; the
        // coordinator must prove a successful result after the model calls it.
        val fallbackPolicy =
            input.queryPolicy.copy(
                allowGeneralQuestions = input.queryPolicy.allowGeneralQuestions && !requiresGrounding,
            )
        val useFallback =
            ResponsePolicy.shouldUseHonestFallback(
                recordQuestion = intent.recordQuestion,
                analysisQuery = intent.analysisQuery,
                historyRelevant = ResponsePolicy.hasRelevantHistory(input.query, context.recentConversation),
                policy = fallbackPolicy,
                grounded = grounded,
                canAttemptToolGrounding = canAttemptToolGrounding,
            )
        if (useFallback) return ModelAnswerPlan(request = null, useFallback = true)

        return ModelAnswerPlan(
            request =
                RagStreamRequest(
                    context =
                        promptBuilder.buildContext(
                            context.selected,
                            input.query,
                            context.recentConversation,
                            context.deterministicSummary,
                            context.webSources,
                        ),
                    turnStrings = input.turnStrings,
                    selected = context.selected,
                    contextResults = context.contextResults,
                    usedDeterministicSummary = context.deterministicSummary != null,
                    allowGeneralQuestions = input.queryPolicy.allowGeneralQuestions || context.useTools,
                    useTools = context.useTools,
                    // Cloud selection is independent from the evidence policy:
                    // paid cloud should serve grounded record turns too, while
                    // the gates above still prevent unsupported facts.
                    forceCloud = input.queryPolicy.allowGeneralQuestions,
                    requiresGrounding = requiresGrounding,
                    grounded = grounded,
                    webSources = context.webSources,
                    webReferencesUnavailable = input.webReferencesUnavailable,
                ),
            useFallback = false,
        )
    }

    private fun buildAnswerContext(input: ModelAnswerInput): AnswerContext {
        val deterministicSummary = analysisContextBuilder?.build(input.query, today)
        val recentConversation = promptBuilder.formatHistory(input.history)
        val historyGrounding =
            ResponsePolicy.canUseHistoryAsGrounding(
                recordQuestion = input.intent.recordQuestion,
                analysisQuery = input.intent.analysisQuery,
                historyRelevant = ResponsePolicy.hasRelevantHistory(input.query, recentConversation),
            )
        val chunks = input.results.map(promptBuilder::formatChunk)
        val selectedIndices =
            promptBuilder.selectWithinBudget(
                chunks,
                maxContextTokens = input.queryPolicy.maxContextTokens ?: config.maxContextTokens,
                reservedTokens =
                    TokenEstimator.estimateTokens(recentConversation) +
                        TokenEstimator.estimateTokens(deterministicSummary.orEmpty()) +
                        TokenEstimator.estimateTokens(
                            input.webSources
                                .mapIndexed { index, source -> promptBuilder.formatWebSource(source, index + 1) }
                                .joinToString("\n"),
                        ),
            )
        return AnswerContext(
            deterministicSummary = deterministicSummary,
            recentConversation = recentConversation,
            selected = selectedIndices.map(chunks::get),
            contextResults = selectedIndices.map(input.results::get),
            grounded =
                ResponsePolicy.hasGrounding(
                    input.query,
                    selectedIndices,
                    input.results,
                    deterministicSummary,
                    input.intent.dateRange,
                ),
            historyGrounding = historyGrounding,
            useTools = ResponsePolicy.shouldUseAnalysisTools(input.query, toolCallingEngine, toolRegistry),
            webSources = input.webSources,
        )
    }

    /**
     * Public medical references are cloud-only and general-question-only. A
     * patient or record question must stay on the local data path, even when a
     * cloud model is enabled, so a literature excerpt cannot be mistaken for
     * evidence about a named horse.
     */
    private suspend fun findWebSources(
        query: String,
        intent: AnswerIntent,
        queryPolicy: RagQueryPolicy,
    ): VeterinaryWebSearchResult? {
        val provider = webSourceProvider ?: return null
        return when {
            !queryPolicy.allowGeneralQuestions -> null
            intent.recordQuestion -> null
            intent.analysisQuery -> null
            !VeterinaryWebQuery.isMedicalQuestion(query) -> null
            else ->
                try {
                    val safeTopic = VeterinaryWebQuery.extractTopic(query)
                    if (safeTopic == null) {
                        VeterinaryWebSearchResult.Success(emptyList())
                    } else {
                        withTimeoutOrNull(WEB_REFERENCE_TIMEOUT_MILLIS) {
                            when (val result = provider.search(safeTopic)) {
                                is VeterinaryWebSearchResult.Success ->
                                    VeterinaryWebSearchResult.Success(
                                        VeterinaryWebQuery.filterRelevantSources(query, result.sources),
                                    )
                                VeterinaryWebSearchResult.Unavailable -> result
                            }
                        } ?: VeterinaryWebSearchResult.Unavailable
                    }
                } catch (ce: kotlinx.coroutines.CancellationException) {
                    throw ce
                } catch (_: Throwable) {
                    VeterinaryWebSearchResult.Unavailable
                }
        }
    }

    /** Handles answers that are safer as direct projections of stored data. */
    private suspend fun FlowCollector<RagStreamEvent>.emitDeterministicAnswer(
        query: String,
        results: List<SearchResult>,
        intent: AnswerIntent,
        turnStrings: AssistantStrings,
    ): Boolean =
        when {
            emitPatientIdentityAnswer(
                query = query,
                scopedPatient = intent.patientScope.name,
                patientNameMentioned = intent.patientScope.nameMentioned,
                patientRepository = patientRepository,
                today = today,
            ) -> true
            emitOwnerContactAnswer(query, intent.patientScope.name, ownerRepository) -> true
            intent.recordQuestion &&
                emitReproductionAttributeAnswer(
                    query = query,
                    scopedPatient = intent.patientScope.name,
                    patientNameMentioned = intent.patientScope.nameMentioned,
                ) -> true
            intent.recordQuestion &&
                emitCurrentGestationAnswer(
                    query = query,
                    scopedPatient = intent.patientScope.name,
                    patientNameMentioned = intent.patientScope.nameMentioned,
                ) -> true
            // "When was the last <type> visit?" is answered from the retrieved
            // record dates so a model cannot drift to a plausible but false date.
            emitLatestRecordAnswer(query, results, intent.patientScope.name) -> true
            RecentActivityIntent.matches(query, intent.dateRange) &&
                emitRecentActivityAnswer(results, intent.dateRange, turnStrings) -> true
            else -> false
        }

    private suspend fun FlowCollector<RagStreamEvent>.emitReproductionAttributeAnswer(
        query: String,
        scopedPatient: String?,
        patientNameMentioned: Boolean,
    ): Boolean {
        val builder = analysisContextBuilder
        val attribute = ReproductionAttributeIntent.requestedAttribute(query)
        val canResolvePatient = !patientNameMentioned || scopedPatient != null
        val facts =
            if (builder != null && attribute != null && canResolvePatient) {
                builder.reproductionAttributeFacts(query)
            } else {
                null
            }
        return if (attribute != null && facts != null) {
            emitReproductionAttributeAnswer(query, attribute, facts)
        } else {
            false
        }
    }

    /** Emits live pregnancy facts before any model can recalculate or invent them. */
    private suspend fun FlowCollector<RagStreamEvent>.emitCurrentGestationAnswer(
        query: String,
        scopedPatient: String?,
        patientNameMentioned: Boolean,
    ): Boolean {
        // An unresolved or ambiguous explicit name must never be converted
        // into a deterministic "no pregnancy" answer for another horse (or
        // for the whole herd). Leave it to the normal grounding gate, which
        // will refuse the unsupported lookup honestly.
        val builder = analysisContextBuilder
        if (builder == null) return false
        if (patientNameMentioned && scopedPatient == null) return false
        return emitCurrentGestationAnswerFromBuilder(query, scopedPatient, builder)
    }

    private suspend fun FlowCollector<RagStreamEvent>.emitCurrentGestationAnswerFromBuilder(
        query: String,
        scopedPatient: String?,
        builder: AnalysisContextBuilder,
    ): Boolean {
        val outcomeFacts = builder.reproductionOutcomeFacts(query)
        val breedingFacts = if (outcomeFacts == null) builder.breedingFacts(query, today) else null
        return when {
            outcomeFacts != null -> {
                emitBreedingOutcomeAnswer(query, outcomeFacts)
                true
            }
            !breedingFacts.isNullOrEmpty() -> {
                emitBreedingAnswer(query, breedingFacts)
                true
            }
            else -> {
                val facts = builder.gestationFacts(query, today)
                if (facts != null) emitGestationAnswer(query, facts, scopedPatient)
                facts != null
            }
        }
    }

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
        val period = "${DateFormatting.formatHumanDate(dateRange.from)}–${DateFormatting.formatHumanDate(dateRange.to)}"
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
                val date = result.date?.let(DateFormatting::formatHumanDate) ?: "unknown date"
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
}
