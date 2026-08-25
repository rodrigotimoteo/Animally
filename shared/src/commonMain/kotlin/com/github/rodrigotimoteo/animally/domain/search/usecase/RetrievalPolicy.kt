package com.github.rodrigotimoteo.animally.domain.search.usecase

import com.github.rodrigotimoteo.animally.domain.search.model.SearchResult

/**
 * Shared retrieval policy for two-legged RAG retrieval: a strict AND leg
 * first, then one broad OR retry when the AND leg is EMPTY or WEAK.
 *
 * Extracted so the golden-set harness and [GenerateRagResponseUseCase]
 * cannot drift: production changes to the weak-retry/dedup rules now flip
 * golden expectations visibly instead of silently invalidating them while
 * tests stay green against a hand-copied mirror.
 */
object RetrievalPolicy {
    /**
     * An AND leg returning FEWER than this many records is treated as a miss
     * for retrieval purposes. A single weak hit used to suppress the
     * synonym/OR retry entirely ("standing sedation" locked onto Xylazine
     * while Detomidine's "Colic sedation" never joined; "trim" never reached
     * the hoof-care synonyms).
     */
    const val WEAK_RESULT_THRESHOLD = 3

    /**
     * Merges the AND leg with the OR retry leg: when [andResults] already
     * carries [threshold] or more records the retry is skipped entirely
     * ([orRetry] is not invoked), otherwise retry hits are appended after the
     * AND leg, deduplicated by record identity (recordType + recordId).
     *
     * @param orRetry lazy provider for the broad OR results - laziness keeps
     * the strong-leg fast path at one FTS query, exactly like the pre-extraction
     * behavior.
     */
    fun mergeWeakRetry(
        andResults: List<SearchResult>,
        threshold: Int = WEAK_RESULT_THRESHOLD,
        orRetry: () -> List<SearchResult>,
    ): List<SearchResult> {
        if (andResults.size >= threshold) return andResults
        val seenKeys = andResults.map { it.recordType to it.recordId }.toSet()
        return andResults + orRetry().filter { retry -> (retry.recordType to retry.recordId) !in seenKeys }
    }
}
