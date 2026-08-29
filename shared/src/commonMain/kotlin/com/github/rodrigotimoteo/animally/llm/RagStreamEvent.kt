package com.github.rodrigotimoteo.animally.llm

import com.github.rodrigotimoteo.animally.domain.search.model.SearchResult
import com.github.rodrigotimoteo.animally.domain.vetreference.model.VeterinaryWebSource

/**
 * One element of the assistant answer stream. Replaces the bare
 * `Flow<String>` so the UI can render typed side information (source cards,
 * interruption state) without string-matching the answer text.
 */
sealed interface RagStreamEvent {
    /**
     * Cumulative sanitized answer text so far. Streaming engines emit
     * full-so-far snapshots; downstream consumers replace their buffer with
     * each chunk.
     */
    data class Chunk(
        val text: String,
    ) : RagStreamEvent

    /**
     * Retrieved records that support the final answer, in source order,
     * deduplicated. The UI renders these as tappable source cards; internal
     * record headers used during grounding are never part of the display text.
     */
    data class Sources(
        val sources: List<SearchResult>,
    ) : RagStreamEvent

    /** Public veterinary references used for a general medical answer. */
    data class WebSources(
        val sources: List<VeterinaryWebSource>,
    ) : RagStreamEvent

    /**
     * The generation stream failed mid-emission (NOT user cancellation —
     * that propagates as [kotlinx.coroutines.CancellationException]).
     * [partialText] preserves whatever arrived so the UI can show it with a
     * retry affordance instead of discarding the work.
     */
    data class Interrupted(
        val partialText: String,
        val error: String?,
    ) : RagStreamEvent
}
