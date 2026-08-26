package com.github.rodrigotimoteo.animally.llm

import com.github.rodrigotimoteo.animally.domain.common.RecordType
import com.github.rodrigotimoteo.animally.domain.search.model.SearchResult
import kotlinx.coroutines.flow.FlowCollector

/**
 * Detects the record TYPE a question is about, so the RAG pipeline can refuse
 * deterministically when retrieval returned records but none of the asked-for
 * kind - the same hallucination shape as [DosageGuard]: a small model handed
 * only a patient-identity chunk plus "when was his last farrier visit?" will
 * freeball a plausible date instead of admitting the record does not exist.
 *
 * Deliberately conservative: only unambiguous topic keywords map to a type,
 * because a false positive turns an answerable question into a refusal.
 */
object RecordTypeIntent {
    /** Word-boundary keyword groups mapped to the wire names they demand. */
    private val intents: List<Pair<Regex, Set<String>>> =
        listOf(
            Regex(
                "\\b(farrier|farriers|shoe|shoes|shod|shoeing|reshod|" +
                    "trim|trims|trimmed|trimming|hoof|hoves)\\b",
            ) to setOf("FARRIER_VISIT"),
            Regex(
                "\\b(dental|dentistry|float|floated|floating|floats|molar|molars|incisor|incisors|quidding)\\b",
            ) to setOf("DENTISTRY"),
            Regex(
                "\\b(deworm|dewormed|deworming|wormer|worming|ivermectin|fenbendazole|moxidectin|pyrantel)\\b",
            ) to setOf("DEWORMING"),
            Regex(
                "\\b(vaccination|vaccinations|vaccinated|vaccine|vaccines|" +
                    "booster|boosters|tetanus|influenza|rabies)\\b",
            ) to setOf("VACCINATION"),
        )

    /** "when was his last..." / "most recent farrier visit" / "last ... date" shapes. */
    private val latestDateRegex =
        Regex(
            "\\b(when|date|how recent).{0,40}?\\b(last|latest|most recent|previous|prior)\\b|" +
                "\\b(last|latest|most recent)\\b.{0,40}?\\b(visit|vaccination|booster|check|appointment|record)\\b",
        )

    /**
     * The record-type wire names [query] asks about, or empty when the query
     * has no recognizable record-type intent (the gate then stays silent).
     */
    fun expectedRecordTypes(query: String): Set<String> {
        val lowered = query.lowercase()
        return intents
            .filter { (regex, _) -> regex.containsMatchIn(lowered) }
            .flatMap { (_, types) -> types }
            .toSet()
    }

    /**
     * True when [query] asks WHEN the most recent record of some kind happened
     * ("When was Thunder's last farrier visit?"). Small models reliably
     * freeball today-ish dates for this shape even with the right chunks in
     * context, so the pipeline answers it deterministically from the
     * retrieved record dates instead of streaming through the model.
     */
    fun isLatestRecordDateQuery(query: String): Boolean = latestDateRegex.containsMatchIn(query.lowercase())

    /**
     * True when [query] and [text] share a content (non-filler) token,
     * case-insensitively, matching also simple singular/plural variants via
     * shared prefix ("visit" ~ "visits"). Used by the RAG empty-grounding
     * gate: prior conversation may only unlock the model call when it
     * actually speaks about the same subject - otherwise a follow-up with
     * zero retrieved records lets the model freeball an answer from thin
     * context.
     */
    fun sharesContentToken(
        query: String,
        text: String,
    ): Boolean {
        val tokens = contentTokens(query).map(String::lowercase).toSet()
        if (tokens.isEmpty()) return false
        return contentTokens(text).map(String::lowercase).any { candidate ->
            candidate.length >= MIN_PREFIX_CHARS &&
                tokens.any { token ->
                    token.length >= MIN_PREFIX_CHARS &&
                        (candidate.startsWith(token) || token.startsWith(candidate))
                }
        }
    }

    /** Tokens below this length never count for the shared-subject check. */
    private const val MIN_PREFIX_CHARS = 3

    /** Content tokens of [text]: cleaned, non-blank, non-filler (mirrors AssistantPrompts). */
    private fun contentTokens(text: String): List<String> =
        text
            .split(Regex("\\s+"))
            .map(::clean)
            .filter { it.isNotBlank() && it.lowercase() !in FILLER_WORDS }

    /** Mirrors AssistantPrompts.clean exactly so both tokenizers agree. */
    private fun clean(token: String): String =
        token
            .trim('?', ',', '.', '!', ':', ';')
            .replace("'", "")
            .replace("’", "")

    // Mirrors AssistantPrompts' filler list (kept private there) so the
    // shared-subject check tokenizes identically to query shaping.
    private val FILLER_WORDS =
        setOf(
            "what",
            "when",
            "which",
            "who",
            "did",
            "do",
            "does",
            "how",
            "is",
            "are",
            "was",
            "were",
            "the",
            "a",
            "an",
            "of",
            "for",
            "to",
            "in",
            "on",
            "any",
            "have",
            "has",
            "had",
            "she",
            "he",
            "her",
            "his",
            "it",
            "there",
            "me",
            "my",
            "i",
            "tell",
            "about",
        )
}

/**
 * Deterministic answer for superlative-date questions ("When was Thunder's
 * last farrier visit?"): picks the newest retrieved record of the asked-for
 * type for a single patient, or null when the question is not a
 * latest-record-date query, no typed/dated record was retrieved, or the
 * candidate pool spans several patients without a resolvable name scope
 * (those turns keep the normal model path).
 */
internal fun latestRecordAnswer(
    query: String,
    results: List<SearchResult>,
    scope: String?,
): SearchResult? {
    if (AssistantPrompts.isPortugueseQuery(query) || !RecordTypeIntent.isLatestRecordDateQuery(query)) {
        return null
    }
    val expectedTypes = RecordTypeIntent.expectedRecordTypes(query)
    val typedDated =
        if (expectedTypes.isEmpty()) {
            emptyList()
        } else {
            results.filter { it.recordType in expectedTypes && it.date != null }
        }
    val pool =
        typedDated
            .takeIf { it.isNotEmpty() }
            ?.let { typed ->
                scope
                    ?.let { target -> typed.filter { it.patientName.lowercase() == target } }
                    ?.ifEmpty { null }
                    ?: typed
            }
    return pool
        ?.takeIf { candidates -> candidates.map { it.patientId }.distinct().size == 1 }
        ?.maxByOrNull { it.date!! }
}

/** Human noun for a record type used in deterministic latest-record answers. */
internal fun recordTypeNoun(recordType: String): String =
    when (recordType) {
        RecordType.FarrierVisit.wireName -> "farrier visit"
        RecordType.Dentistry.wireName -> "dentistry visit"
        RecordType.Vaccination.wireName -> "vaccination"
        RecordType.Deworming.wireName -> "deworming"
        else ->
            RecordType
                .fromWireName(recordType)
                ?.displayName
                ?.lowercase()
                ?.plus(" record")
                ?: "record"
    }

/** Renders a date as "14 Mar 2026" (locale-independent, mirrors the RAG chunk format). */
internal fun formatHumanDateShort(date: kotlinx.datetime.LocalDate): String {
    val months =
        listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    return "${date.day} ${months[date.monthNumber - 1]} ${date.year}"
}

/**
 * Emits the deterministic latest-record answer when [query] is a
 * superlative-date question with resolvable grounding; false when the turn
 * should continue through the normal model path.
 */
internal suspend fun FlowCollector<RagStreamEvent>.emitLatestRecordAnswer(
    query: String,
    results: List<SearchResult>,
    scope: String?,
): Boolean {
    val latest = latestRecordAnswer(query, results, scope) ?: return false
    val date = latest.date ?: return false
    val header = "[${latest.recordType} #${latest.recordId}]"
    val sentence =
        "${latest.patientName}'s most recent ${recordTypeNoun(latest.recordType)} on record was on " +
            formatHumanDateShort(date)
    emit(RagStreamEvent.Chunk("$sentence. $header"))
    emit(RagStreamEvent.Sources(listOf(latest)))
    return true
}
