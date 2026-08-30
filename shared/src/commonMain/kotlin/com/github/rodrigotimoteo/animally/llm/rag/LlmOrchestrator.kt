package com.github.rodrigotimoteo.animally.llm.rag

import com.github.rodrigotimoteo.animally.llm.RagAnswerStreamCoordinator
import com.github.rodrigotimoteo.animally.llm.RagLlmEngine
import com.github.rodrigotimoteo.animally.llm.RagStreamEvent
import com.github.rodrigotimoteo.animally.llm.RagStreamRequest
import com.github.rodrigotimoteo.animally.llm.RagToolCallingEngine
import com.github.rodrigotimoteo.animally.llm.RagToolRegistry
import kotlinx.coroutines.flow.FlowCollector

/**
 * Engine routing (FmFirst vs Cloud), streaming and tool-call dispatch.
 *
 * Thin wrapper over [RagAnswerStreamCoordinator] so the facade depends on a
 * rag-scoped orchestrator instead of reaching directly into the llm package.
 * All provider-neutral business rules (citations, retry markers) remain in the
 * coordinator; this class owns the route decision boundary.
 */
internal class LlmOrchestrator(
    llmEngine: RagLlmEngine,
    toolCallingEngine: RagToolCallingEngine?,
    toolRegistry: RagToolRegistry?,
) {
    private val coordinator =
        RagAnswerStreamCoordinator(
            llmEngine = llmEngine,
            toolCallingEngine = toolCallingEngine,
            toolRegistry = toolRegistry,
        )

    /** Streams an answer, adds authoritative source cards, then hides citation syntax in the bubble. */
    suspend fun stream(
        collector: FlowCollector<RagStreamEvent>,
        request: RagStreamRequest,
    ) {
        coordinator.stream(collector, request)
    }
}
