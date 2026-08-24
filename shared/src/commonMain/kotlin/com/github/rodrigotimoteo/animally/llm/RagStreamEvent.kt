package com.github.rodrigotimoteo.animally.llm

import com.github.rodrigotimoteo.animally.domain.search.model.SearchResult

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
     * Retrieved records whose bracketed citations actually appear in the
     * final answer text, in citation order, deduplicated. Emitted once per
     * turn after the final chunk; empty citations emit nothing.
     */
    data class Sources(
        val sources: List<SearchResult>,
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
