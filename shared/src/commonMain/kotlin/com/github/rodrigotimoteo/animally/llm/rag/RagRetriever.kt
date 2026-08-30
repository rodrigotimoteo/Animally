package com.github.rodrigotimoteo.animally.llm.rag

import com.github.rodrigotimoteo.animally.domain.owner.IOwnerRepository
import com.github.rodrigotimoteo.animally.domain.patient.IPatientRepository
import com.github.rodrigotimoteo.animally.domain.search.model.SearchResult
import com.github.rodrigotimoteo.animally.domain.search.usecase.RetrievalPolicy
import com.github.rodrigotimoteo.animally.domain.search.usecase.SearchUseCase
import com.github.rodrigotimoteo.animally.llm.AnalysisIntents
import com.github.rodrigotimoteo.animally.llm.AssistantPrompts
import com.github.rodrigotimoteo.animally.llm.RagDateRange
import com.github.rodrigotimoteo.animally.llm.RagDateRangeIntent
import com.github.rodrigotimoteo.animally.llm.RagHistoryEntry
import com.github.rodrigotimoteo.animally.llm.RagRecordSearch
import com.github.rodrigotimoteo.animally.llm.RecordQuestionIntent
import com.github.rodrigotimoteo.animally.llm.RecordTypeIntent
import com.github.rodrigotimoteo.animally.llm.support.SharedStopWords
import kotlinx.datetime.LocalDate

/**
 * FTS query building + search execution + result ranking.
 *
 * Owns the two-leg retrieval contract (strict AND first, broad OR retry when
 * EMPTY or WEAK), patient-scope resolution and post-retrieval ranking
 * (patient/date/record-type boundaries). No prompt or policy work here.
 */
internal data class PatientScope(
    val name: String?,
    val requiresFilter: Boolean,
    val nameMentioned: Boolean,
)

internal data class AnswerIntent(
    val dateRange: RagDateRange?,
    val patientScope: PatientScope,
    val recordQuestion: Boolean,
    val analysisQuery: Boolean,
)

@Suppress("TooManyFunctions")
internal class RagRetriever(
    private val searchUseCase: SearchUseCase,
    private val recordSearch: RagRecordSearch?,
    private val patientRepository: IPatientRepository?,
    private val ownerRepository: IOwnerRepository?,
    private val today: LocalDate,
) {
    private companion object {
        const val MIN_NAME_PREFIX_CHARS = 2
    }

    fun classifyQuery(
        query: String,
        history: List<RagHistoryEntry>,
    ): AnswerIntent {
        val dateRange = RagDateRangeIntent.resolve(query, today)
        val patientScope = resolvePatientScope(query, dateRange, history)
        val recordQuestion =
            RecordQuestionIntent.isRecordQuestion(
                query = query,
                scopedPatientName = patientScope.name,
                dateRange = dateRange,
                patientNameMentioned = patientScope.nameMentioned,
            )
        return AnswerIntent(
            dateRange = dateRange,
            patientScope = patientScope,
            recordQuestion = recordQuestion,
            analysisQuery = AnalysisIntents.isAnalysisQuery(query),
        )
    }

    fun retrieveRelevantResults(
        query: String,
        enriched: String,
        intent: AnswerIntent,
    ): List<SearchResult> {
        if (!intent.recordQuestion && !intent.analysisQuery) {
            // Do not leak incidental patient rows into a general cloud answer
            // just because a word such as "colic" or "vaccine" matches the index.
            return emptyList()
        }
        // A short follow-up such as "How old is she?" contains no searchable
        // patient token. The classifier may have resolved the pronoun against
        // the latest conversation turn; carry that resolved name into both
        // retrieval legs so the database, rather than the model, remains the
        // source of the answer.
        val scopedQuery = appendResolvedPatientScope(query, intent.patientScope.name)
        val scopedEnriched = appendResolvedPatientScope(enriched, intent.patientScope.name)
        val restricted =
            restrictResults(
                retrieve(scopedQuery, scopedEnriched, intent.dateRange),
                intent.patientScope.name,
                intent.dateRange,
                intent.patientScope.requiresFilter,
                intent.patientScope.nameMentioned,
            )
        return restrictToExpectedRecordTypes(restricted, query)
    }

    /**
     * A typed question must not feed unrelated record kinds to the model just
     * because the broad OR retry matched a generic word such as "date" or
     * "treatment". Keeping this boundary after patient/date filtering also
     * preserves the honest empty-result path for missing record categories.
     */
    fun restrictToExpectedRecordTypes(
        results: List<SearchResult>,
        query: String,
    ): List<SearchResult> {
        val expectedTypes = RecordTypeIntent.expectedRecordTypes(query)
        return if (expectedTypes.isEmpty()) {
            results
        } else {
            results.filter { it.recordType in expectedTypes }
        }
    }

    fun appendResolvedPatientScope(
        query: String,
        patientName: String?,
    ): String =
        if (patientName != null && RecordQuestionIntent.hasIndividualPatientReference(query)) {
            "$query $patientName"
        } else {
            query
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
    fun retrieve(
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
    fun restrictResults(
        results: List<SearchResult>,
        scopedPatient: String?,
        dateRange: RagDateRange?,
        requiresPatientFilter: Boolean,
        patientNameMentioned: Boolean,
    ): List<SearchResult> =
        if (requiresPatientFilter && scopedPatient == null) {
            if (patientNameMentioned) {
                emptyList()
            } else {
                // Test/alternate seams may not have an IPatientRepository. A
                // pronoun can still be safely resolved when retrieval itself has
                // returned records for exactly one patient; never allow a mixed
                // result set to cross patient boundaries without an explicit
                // scope.
                results
                    .takeIf { rows -> rows.map(SearchResult::patientId).distinct().size <= 1 }
                    .orEmpty()
            }
        } else {
            results.filter { result ->
                val patientMatches = scopedPatient == null || result.patientName.lowercase() == scopedPatient
                val dateMatches = dateRange == null || result.date?.let(dateRange::contains) == true
                patientMatches && dateMatches
            }
        }

    /** Resolves a unique patient/owner scope, or marks the result set unsafe to share. */
    fun resolvePatientScope(
        query: String,
        dateRange: RagDateRange?,
        history: List<RagHistoryEntry>,
    ): PatientScope {
        val activePatientNames = patientRepository?.patientNames().orEmpty()
        val activeOwnerNames =
            ownerRepository
                ?.getOwnerList()
                ?.filter { it.isActive }
                ?.map { it.name }
                .orEmpty()
        val activeNames = activePatientNames + activeOwnerNames
        val matchedNames = matchingScopeNames(activeNames, query)
        val matchedPatientNames = matchingScopeNames(activePatientNames, query)
        val hasIndividualReference = RecordQuestionIntent.hasIndividualPatientReference(query)
        val hasLikelyName = hasLikelyPatientName(query, dateRange)
        val historyPatientName =
            resolveHistoryPatientName(
                query,
                activePatientNames,
                matchedPatientNames,
                hasLikelyName,
                history,
            )
        val name = selectPatientName(matchedNames, historyPatientName, hasIndividualReference, activePatientNames)
        return PatientScope(
            name = name,
            requiresFilter =
                requiresPatientFilter(
                    matchedNames,
                    hasIndividualReference,
                    hasLikelyName,
                    historyPatientName,
                ),
            nameMentioned = matchedNames.isNotEmpty() || hasLikelyName,
        )
    }

    private fun matchingScopeNames(
        activeNames: List<String>,
        query: String,
    ): List<String> {
        val tokens = patientScopeTokens(query)
        return activeNames.filter { name ->
            val nameTokens = patientScopeTokens(name)
            tokens.any { token -> token in nameTokens }
        }
    }

    private fun hasLikelyPatientName(
        query: String,
        dateRange: RagDateRange?,
    ): Boolean {
        if (patientRepository == null && ownerRepository == null) return false
        val isEducational = RecordQuestionIntent.isEducationalQuestion(query)
        val isRecordQuestion = RecordQuestionIntent.isRecordQuestion(query, null, dateRange)
        return (!isEducational || isRecordQuestion) && RecordQuestionIntent.hasLikelyNamedPatientReference(query)
    }

    private fun resolveHistoryPatientName(
        query: String,
        activeNames: List<String>,
        matchedNames: List<String>,
        hasLikelyName: Boolean,
        history: List<RagHistoryEntry>,
    ): String? =
        if (matchedNames.isEmpty() && !hasLikelyName && RecordQuestionIntent.hasIndividualPatientReference(query)) {
            resolvePatientFromHistory(activeNames, history)
        } else {
            null
        }

    private fun selectPatientName(
        matchedNames: List<String>,
        historyPatientName: String?,
        hasIndividualReference: Boolean,
        activeNames: List<String>,
    ): String? =
        when {
            matchedNames.size == 1 -> matchedNames.single().lowercase()
            historyPatientName != null -> historyPatientName.lowercase()
            matchedNames.isEmpty() && hasIndividualReference && activeNames.size == 1 ->
                activeNames.single().lowercase()
            else -> null
        }

    private fun requiresPatientFilter(
        matchedNames: List<String>,
        hasIndividualReference: Boolean,
        hasLikelyName: Boolean,
        historyPatientName: String?,
    ): Boolean = matchedNames.isNotEmpty() || hasIndividualReference || hasLikelyName || historyPatientName != null

    /**
     * Resolves a singular pronoun only from a patient name that appears in the
     * most recent unambiguous conversation turn. A turn mentioning multiple
     * active patients deliberately stays unresolved rather than silently
     * selecting one.
     */
    private fun resolvePatientFromHistory(
        activeNames: List<String>,
        history: List<RagHistoryEntry>,
    ): String? {
        for (entry in history.asReversed()) {
            // Resolve from the user's prior wording only. An assistant answer
            // is useful context for generation, but it is not authoritative
            // enough to establish which patient a factual follow-up targets.
            val matches = activeNames.filter { entry.question.containsPatientName(it) }
            if (matches.isNotEmpty()) return matches.singleOrNull()
        }
        return null
    }

    private fun String.containsPatientName(patientName: String): Boolean =
        Regex(
            "(?<![\\p{L}\\p{N}_])${Regex.escape(patientName)}(?![\\p{L}\\p{N}_])",
            RegexOption.IGNORE_CASE,
        ).containsMatchIn(this)

    private fun patientScopeTokens(query: String): Set<String> =
        query
            .split(Regex("\\s+"))
            .map(::cleanPatientToken)
            .filter { it.length >= MIN_NAME_PREFIX_CHARS && it !in SharedStopWords.PATIENT_SCOPE_STOP_WORDS }
            .toSet()

    private fun cleanPatientToken(token: String): String =
        token
            .trim('?', ',', '.', '!', ':', ';', '\'')
            .removeSuffix("'s")
            .removeSuffix("'S")
            .removeSuffix("’s")
            .removeSuffix("’S")
            .lowercase()
}
