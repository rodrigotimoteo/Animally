package com.github.rodrigotimoteo.animally.llm

import com.github.rodrigotimoteo.animally.domain.common.RecordType
import com.github.rodrigotimoteo.animally.domain.patient.IPatientRepository
import com.github.rodrigotimoteo.animally.domain.search.model.SearchResult
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
}

class GenerateRagResponseUseCase(
    private val searchUseCase: SearchUseCase,
    private val llmEngine: RagLlmEngine,
    private val config: RagConfig = RagConfig.DEFAULT,
    private val strings: AssistantStrings = EnAssistantStrings,
    private val recordSearch: RagRecordSearch? = null,
    private val patientRepository: IPatientRepository? = null,
    private val analysisContextBuilder: AnalysisContextBuilder? = null,
    private val today: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault()),
) {
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

        // Weak-leg retry threshold: an AND leg returning FEWER than this many
        // records is treated as a miss for retrieval purposes. A single weak
        // hit used to suppress the synonym/OR retry entirely ("standing
        // sedation" locked onto Xylazine while Detomidine's "Colic sedation"
        // never joined; "trim" never reached the hoof-care synonyms).
        const val WEAK_RESULT_THRESHOLD = 3

        // Model sometimes regurgitates prompt scaffolding (--- separators,
        // "Question: ..." echoes). Stripped defensively from every chunk.
        val scaffoldLineRegex = Regex("(?m)^\\s*(?:-{3,}|Question:.*|Context:.*|You are .*)\\s*\\n?")

        // Bracketed citation header in the final answer text: [TYPE #id].
        val citationRegex = Regex("\\[([A-Z_]+) #(\\d+)]")

        // Citation-enforcement fallback caps appended headers: a ten-record
        // answer must not gain ten noise lines when the model cites nothing.
        const val MAX_ENFORCED_SOURCES = 3

        /** Human-readable month abbreviations for chunk/TODAY dates (locale-independent). */
        val MONTH_ABBREVIATIONS =
            listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

        /** Renders a date as "14 Mar 2026" (locale-independent, model-friendly). */
        fun formatHumanDate(date: LocalDate): String {
            val month = MONTH_ABBREVIATIONS[date.monthNumber - 1]
            return "${date.dayOfMonth} $month ${date.year}"
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
            val results = prioritizePatientScope(retrieve(query, enriched), query)
            // Dosage guardrail: a how-much-drug question answered without any
            // medication record in context must be refused deterministically -
            // a small model with no grounding will hallucinate a dose. Checked
            // BEFORE the history fallback so prior conversation alone can never
            // unlock dosage advice.
            if (DosageGuard.isDosageIntent(query) && results.none(::isMedicationRecord)) {
                emit(RagStreamEvent.Chunk(turnStrings.dosageRefusal))
                return@flow
            }
            // Analysis mode: deterministic summaries computed in Kotlin feed
            // as authoritative context so the model narrates instead of doing
            // arithmetic. Built once per turn; counts toward the token budget.
            val deterministicSummary = analysisContextBuilder?.build(query)
            val recentConversation = formatHistory(history)
            val chunks = results.map { result -> formatChunk(result) }
            val reservedTokens =
                estimateTokens(recentConversation) + estimateTokens(deterministicSummary.orEmpty())
            val selectedIndices =
                selectWithinBudget(
                    chunks,
                    reservedTokens = reservedTokens,
                )
            if (selectedIndices.isEmpty() && recentConversation.isEmpty() && deterministicSummary == null) {
                emit(RagStreamEvent.Chunk(turnStrings.noResultsFallback))
                return@flow
            }
            val selected = selectedIndices.map(chunks::get)
            val context = buildContext(selected, query, recentConversation, deterministicSummary)
            streamAnswer(
                context,
                turnStrings,
                selected,
                selectedIndices.map(results::get),
                usedDeterministicSummary = deterministicSummary != null,
            )
        }

    /**
     * Streams the model answer for [context], then applies citation
     * enforcement and emits the cited-sources event. A mid-stream failure
     * (anything but user cancellation) becomes an
     * [RagStreamEvent.Interrupted] marker carrying the partial text instead
     * of tearing down the whole turn.
     */
    private suspend fun FlowCollector<RagStreamEvent>.streamAnswer(
        context: String,
        turnStrings: AssistantStrings,
        selected: List<String>,
        contextResults: List<SearchResult>,
        usedDeterministicSummary: Boolean,
    ) {
        // Streaming emits cumulative snapshots; sanitize() is idempotent, so
        // re-sanitizing the growing text each step is safe and downstream
        // consumers replace their buffer with each emission.
        var lastEmitted = ""
        try {
            llmEngine
                .generateStreaming(context, AssistantPrompts.systemPrompt(turnStrings))
                .collect { text ->
                    lastEmitted = sanitize(text)
                    emit(RagStreamEvent.Chunk(lastEmitted))
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
        // Citation enforcement: the system prompt mandates citing bracketed
        // headers, but the model skips them often enough that the guarantee
        // is enforced here - when records were used and the reply carries
        // none, append the ACTUAL retrieved headers (never invented ones).
        // The trigger is MAPPED citations, not bare "[": a fabricated or
        // stale header the model invented ([Giraffe #1], a deleted id)
        // satisfies the eye but maps to no source card, so the guarantee
        // needs the real headers appended anyway.
        val mappedCitations = citedResults(lastEmitted, contextResults)
        if (selected.isNotEmpty() && mappedCitations.isEmpty()) {
            // Top-3 by retrieval rank only: appending every selected header
            // turns a ten-record answer into ten lines of citation noise.
            val sources = selected.mapNotNull(::sourceHeader).take(MAX_ENFORCED_SOURCES)
            if (sources.isNotEmpty()) {
                lastEmitted =
                    listOf(lastEmitted.takeIf(String::isNotBlank), sources.joinToString("\n"))
                        .filterNotNull()
                        .joinToString("\n\n")
                emit(RagStreamEvent.Chunk(lastEmitted))
            }
        }
        // Summary-only answers: when retrieval came back empty but the
        // deterministic summary carried the facts, there are no record
        // headers to append - and an uncited confident answer is exactly
        // what the citation guarantee forbids. The system prompt tells the
        // model to cite the summary as [Summary]; when it skips that too,
        // the tag is enforced here.
        if (usedDeterministicSummary && mappedCitations.isEmpty() && "[" !in lastEmitted) {
            lastEmitted =
                listOf(lastEmitted.takeIf(String::isNotBlank), "[Summary]")
                    .filterNotNull()
                    .joinToString("\n\n")
            emit(RagStreamEvent.Chunk(lastEmitted))
        }
        citedResults(lastEmitted, contextResults).takeIf { it.isNotEmpty() }?.let {
            emit(RagStreamEvent.Sources(it))
        }
    }

    /**
     * Two-leg retrieval mirroring the production contract: a strict AND query
     * over the filler-stripped question first, then one broad OR retry (with
     * synonym expansion) when the AND leg is EMPTY or WEAK (fewer than
     * [WEAK_RESULT_THRESHOLD] records) — AND semantics require every content
     * word to match, so natural questions like "which patients belong to X"
     * would otherwise return nothing, and a single lucky hit used to suppress
     * the recall-fixing retry. Retry hits are deduplicated against the AND
     * leg by record identity and appended after it. The caller applies
     * patient-scope prioritization to the MERGED list, so scoped-patient
     * records recovered by the retry rank ahead of same-topic records from
     * other patients exactly like leg-1 results.
     *
     * When no [recordSearch] seam is wired, falls back to [SearchUseCase]
     * with full-text snippets.
     */
    private fun retrieve(
        query: String,
        enriched: String,
    ): List<SearchResult> {
        val seam = recordSearch ?: return searchUseCase(enriched, from = null, to = null, recordTypes = null)
        val andResults = seam.search(AssistantPrompts.toFtsAndQuery(enriched))
        if (andResults.size >= WEAK_RESULT_THRESHOLD) return andResults
        val retryResults = seam.search(AssistantPrompts.toFtsOrQuery(query))
        return andResults + retryResults.filter { retry -> andResults.none { it.sameRecordAs(retry) } }
    }

    /**
     * Patient-name scoping: when a query token prefix-matches exactly one
     * active patient name (case-insensitive, no fuzzy matching), that
     * patient's records move to the front of the retrieval order so
     * [selectWithinBudget] keeps them inside the context budget ahead of
     * same-topic records from other patients. Ambiguous prefixes (two or
     * more patients) and misses leave the order untouched.
     */
    private fun prioritizePatientScope(
        results: List<SearchResult>,
        query: String,
    ): List<SearchResult> {
        val target = scopedPatientName(results, query) ?: return results
        val (scoped, rest) = results.partition { it.patientName.lowercase() == target }
        return scoped + rest
    }

    /** The single patient name (lowercased) whose records should be boosted, or null. */
    private fun scopedPatientName(
        results: List<SearchResult>,
        query: String,
    ): String? {
        val repository = patientRepository
        if (repository == null || results.size < 2) return null
        val tokens =
            query
                .split(Regex("\\s+"))
                .map { it.trim('?', ',', '.', '!', ':', ';', '\'') }
                .filter { it.length >= MIN_NAME_PREFIX_CHARS }
                .map(String::lowercase)
                .toSet()
        val matchedNames =
            if (tokens.isEmpty()) {
                emptyList()
            } else {
                repository.patientNames().filter { name ->
                    val lowered = name.lowercase()
                    tokens.any { token -> lowered.startsWith(token) }
                }
            }
        return matchedNames.singleOrNull()?.lowercase()
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
        reservedTokens: Int = 0,
    ): List<Int> {
        val reserve =
            config.systemReserveTokens + config.queryReserveTokens +
                config.responseReserveTokens + reservedTokens
        val budget = config.maxContextTokens - reserve
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

/** True when both results point at the same stored record. File-level so the
 * use case class stays under its detekt function-count threshold. */
private fun SearchResult.sameRecordAs(that: SearchResult) = recordType == that.recordType && recordId == that.recordId
