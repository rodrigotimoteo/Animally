package com.github.rodrigotimoteo.animally.llm.rag

import com.github.rodrigotimoteo.animally.domain.common.RecordType
import com.github.rodrigotimoteo.animally.domain.search.model.SearchResult
import com.github.rodrigotimoteo.animally.llm.AnalysisIntents
import com.github.rodrigotimoteo.animally.llm.DosageGuard
import com.github.rodrigotimoteo.animally.llm.RagDateRange
import com.github.rodrigotimoteo.animally.llm.RagQueryPolicy
import com.github.rodrigotimoteo.animally.llm.RagToolCallingEngine
import com.github.rodrigotimoteo.animally.llm.RagToolRegistry
import com.github.rodrigotimoteo.animally.llm.RecordTypeIntent

/**
 * Grounding enforcement, citation guarantees and fallback policy for RAG.
 *
 * Keeps all "should we answer from records or refuse?" decisions in one place
 * so the thin facade stays declarative. Pure functions — no I/O.
 */
@Suppress("TooManyFunctions")
internal object ResponsePolicy {
    fun shouldUseNoResultsFallback(
        policy: RagQueryPolicy,
        grounded: Boolean,
        historyRelevant: Boolean,
    ): Boolean = !policy.allowGeneralQuestions && !grounded && !historyRelevant

    fun hasGrounding(
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

    fun summarySupportsQueryTypes(
        summary: String,
        expectedTypes: Set<String>,
    ): Boolean = expectedTypes.isEmpty() || expectedTypes.any { summarySupportsRecordType(summary, it) }

    /**
     * A computed summary only grounds the record kind it actually contains. For
     * example, a care-count summary can mention deworming and farrier rows while
     * still having no vaccination evidence; it must not unlock a vaccination
     * answer merely because some summary exists.
     */
    fun summarySupportsRecordType(
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

    fun hasRelevantHistory(
        query: String,
        recentConversation: String,
    ): Boolean = recentConversation.isNotEmpty() && RecordTypeIntent.sharesContentToken(query, recentConversation)

    fun canUseHistoryAsGrounding(
        recordQuestion: Boolean,
        analysisQuery: Boolean,
        historyRelevant: Boolean,
    ): Boolean = historyRelevant && !recordQuestion && !analysisQuery

    @Suppress("LongParameterList")
    fun shouldUseHonestFallback(
        recordQuestion: Boolean,
        analysisQuery: Boolean,
        historyRelevant: Boolean,
        policy: RagQueryPolicy,
        grounded: Boolean,
        canAttemptToolGrounding: Boolean,
    ): Boolean =
        shouldUseNoResultsFallback(
            policy,
            grounded || canAttemptToolGrounding,
            canUseHistoryAsGrounding(recordQuestion, analysisQuery, historyRelevant),
        )

    fun shouldUseAnalysisTools(
        query: String,
        toolCallingEngine: RagToolCallingEngine?,
        toolRegistry: RagToolRegistry?,
    ): Boolean =
        AnalysisIntents.requiresTools(query) &&
            toolCallingEngine?.supportsToolCalling == true &&
            toolRegistry?.definitions?.isNotEmpty() == true

    fun isMedicationRecord(result: SearchResult): Boolean =
        result.recordType == RecordType.Medication.wireName ||
            result.recordType == RecordType.ControlledSubstance.wireName ||
            result.recordType == RecordType.ReproMedication.wireName

    fun isDosageRefusalNeeded(
        query: String,
        results: List<SearchResult>,
    ): Boolean = DosageGuard.isDosageIntent(query) && results.none(::isMedicationRecord)

    fun requiresGrounding(
        recordQuestion: Boolean,
        analysisQuery: Boolean,
    ): Boolean = recordQuestion || analysisQuery
}
