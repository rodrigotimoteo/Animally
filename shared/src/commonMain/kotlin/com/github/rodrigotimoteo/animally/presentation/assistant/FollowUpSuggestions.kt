package com.github.rodrigotimoteo.animally.presentation.assistant

import com.github.rodrigotimoteo.animally.llm.AssistantStrings
import com.github.rodrigotimoteo.animally.llm.EnAssistantStrings

/**
 * Deterministic follow-up suggestions derived from the record types cited in
 * a completed assistant answer. Pure mapping - no retrieval, no model - so
 * the chips are instant and predictable. At most [MAX_SUGGESTIONS] chips;
 * when no cited type has a mapping (or nothing was cited) the default
 * exploration set is used instead.
 */
object FollowUpSuggestions {
    private const val MAX_SUGGESTIONS = 3

    /** Cited wire name -> suggestion string key. First match wins order. */
    private val BY_WIRE_NAME: Map<String, (AssistantStrings) -> String> =
        mapOf(
            "VACCINATION" to { it.followUpNextBooster },
            "GESTATION" to { it.followUpGestationDay },
            "WEIGHT" to { it.followUpWeightTrend },
            "FARRIER_VISIT" to { it.followUpNextFarrier },
        )

    /**
     * Follow-up suggestions for the [recordTypes] (FTS wire names) cited in
     * one answer, localized via [strings]. Deduplicated, capped at
     * [MAX_SUGGESTIONS]; falls back to the default exploration set when the
     * citations yield nothing.
     */
    fun forCitations(
        recordTypes: Collection<String>,
        strings: AssistantStrings = EnAssistantStrings,
    ): List<String> {
        val suggestions = mutableListOf<String>()
        for (type in recordTypes) {
            BY_WIRE_NAME[type]?.let { producer ->
                val text = producer(strings)
                if (text !in suggestions) suggestions.add(text)
            }
        }
        if (suggestions.isEmpty()) {
            suggestions += defaultSuggestions(strings)
        }
        return suggestions.take(MAX_SUGGESTIONS)
    }

    /** Exploration prompts used when the citations yield nothing. */
    private fun defaultSuggestions(strings: AssistantStrings): List<String> =
        listOf(
            strings.followUpDefaultPatients,
            strings.followUpDefaultTreatments,
            strings.followUpDefaultDates,
        )
}
