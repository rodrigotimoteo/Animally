package com.github.rodrigotimoteo.animally.llm

import com.github.rodrigotimoteo.animally.domain.search.ISearchRepository
import com.github.rodrigotimoteo.animally.domain.search.model.SearchResult
import com.github.rodrigotimoteo.animally.domain.search.usecase.SearchUseCase
import dev.mokkery.MockMode
import dev.mokkery.mock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private class FakeToolEngine(
    private val alwaysRequestsTools: Boolean = false,
    private val failsOnFirstRequest: Boolean = false,
    private val includeCitation: Boolean = true,
) : RagToolCallingEngine {
    override val supportsToolCalling: Boolean = true
    var calls: Int = 0
    var messageSnapshots: List<List<RagChatMessage>> = emptyList()

    override fun generateStreamingWithTools(
        messages: List<RagChatMessage>,
        tools: List<RagToolDefinition>,
    ): Flow<RagToolStreamEvent> =
        flow {
            calls += 1
            messageSnapshots = messageSnapshots + listOf(messages)
            assertTrue(tools.isNotEmpty())
            if (failsOnFirstRequest && calls == 1) error("tools unsupported")
            if (alwaysRequestsTools || calls == 1) {
                emit(RagToolStreamEvent.Text("I’ll check that."))
                emit(
                    RagToolStreamEvent.ToolCalls(
                        listOf(
                            RagToolCall(
                                id = "call-$calls",
                                name = AnalysisToolNames.WEIGHT_SUMMARY,
                                arguments = "{}",
                            ),
                        ),
                    ),
                )
            } else {
                val answer = "The average is 505 kg." + if (includeCitation) " [WEIGHT #7]" else ""
                emit(RagToolStreamEvent.Text(answer))
            }
        }
}

private class FakeToolRegistry : RagToolRegistry {
    var returnsError: Boolean = false
    var receivedCalls: List<RagToolCall> = emptyList()
    override val definitions: List<RagToolDefinition> = AnalysisToolSchemas.definitions
    var calls: Int = 0

    override suspend fun execute(call: RagToolCall): RagToolResult {
        calls += 1
        receivedCalls += call
        return RagToolResult(
            toolCallId = call.id,
            name = call.name,
            content = """{"dataset":"weights","source":"[WEIGHT #7]","average_kg":505.0}""",
            sources =
                listOf(
                    SearchResult(
                        patientId = 1L,
                        patientName = "Bella",
                        breed = "Arabian",
                        microchipId = null,
                        recordType = "WEIGHT",
                        recordId = 7L,
                        date = LocalDate(2025, 2, 1),
                        snippet = "Weight 505 kg.",
                    ),
                ),
            isError = returnsError,
        )
    }
}

private class PlainFallbackEngine : RagLlmEngine {
    var calls: Int = 0

    override fun generate(
        prompt: String,
        instructions: String,
    ): Flow<String> =
        flow {
            calls += 1
            emit("I can still answer from the available context.")
        }
}

private class SilentToolEngine : RagToolCallingEngine {
    override val supportsToolCalling: Boolean = true

    override fun generateStreamingWithTools(
        messages: List<RagChatMessage>,
        tools: List<RagToolDefinition>,
    ): Flow<RagToolStreamEvent> = flow {}
}

class ToolAwareGenerateRagResponseUseCaseTest {
    private val searchRepository: ISearchRepository = mock(MockMode.autoUnit)

    private fun sut(
        toolEngine: RagToolCallingEngine,
        registry: RagToolRegistry,
        plainEngine: RagLlmEngine = PlainFallbackEngine(),
        recordSearch: RagRecordSearch = RagRecordSearch { emptyList() },
        analysisContextBuilder: AnalysisContextBuilder? = null,
        patientRepository: com.github.rodrigotimoteo.animally.domain.patient.IPatientRepository? = null,
    ): GenerateRagResponseUseCase =
        GenerateRagResponseUseCase(
            searchUseCase = SearchUseCase(searchRepository),
            llmEngine = plainEngine,
            recordSearch = recordSearch,
            toolCallingEngine = toolEngine,
            toolRegistry = registry,
            analysisContextBuilder = analysisContextBuilder,
            patientRepository = patientRepository,
            today = LocalDate(2025, 5, 11),
        )

    @Test
    fun `broader analysis executes a tool and replays its result into the final turn`() =
        runTest {
            val toolEngine = FakeToolEngine()
            val registry = FakeToolRegistry()

            val events = sut(toolEngine, registry)("Analyze the weight data").toList()
            val finalChunk = events.filterIsInstance<RagStreamEvent.Chunk>().last().text

            assertEquals(2, toolEngine.calls)
            assertEquals(1, registry.calls)
            assertEquals(4, toolEngine.messageSnapshots[1].size)
            assertEquals(RagChatRole.ASSISTANT, toolEngine.messageSnapshots[1][2].role)
            assertEquals(RagChatRole.TOOL, toolEngine.messageSnapshots[1][3].role)
            assertEquals("The average is 505 kg.", finalChunk)
            assertEquals(
                7L,
                events
                    .filterIsInstance<RagStreamEvent.Sources>()
                    .single()
                    .sources
                    .single()
                    .recordId,
            )
        }

    @Test
    fun `provider tool rejection does not invent an analysis`() =
        runTest {
            val plain = PlainFallbackEngine()
            val events =
                sut(
                    toolEngine = FakeToolEngine(failsOnFirstRequest = true),
                    registry = FakeToolRegistry(),
                    plainEngine = plain,
                )("Analyze the weight data").toList()

            assertEquals(0, plain.calls)
            assertEquals(EnAssistantStrings.analysisLimitReply, events.filterIsInstance<RagStreamEvent.Chunk>().last().text)
            assertTrue(events.none { it is RagStreamEvent.Interrupted })
        }

    @Test
    fun `failed authoritative tool result does not unlock an invented analysis`() =
        runTest {
            val plain = PlainFallbackEngine()
            val registry = FakeToolRegistry().apply { returnsError = true }
            val events =
                sut(
                    toolEngine = FakeToolEngine(),
                    registry = registry,
                    plainEngine = plain,
                )("Analyze the weight data").toList()

            assertEquals(0, plain.calls)
            assertEquals(EnAssistantStrings.analysisLimitReply, events.filterIsInstance<RagStreamEvent.Chunk>().last().text)
            assertTrue(events.none { it is RagStreamEvent.Sources })
        }

    @Test
    fun `uncited tool answer prefers tool sources over retrieved context`() =
        runTest {
            val retrievalSource =
                SearchResult(
                    patientId = 2L,
                    patientName = "Shadow",
                    breed = "Arabian",
                    microchipId = null,
                    recordType = "WEIGHT",
                    recordId = 99L,
                    date = LocalDate(2025, 1, 1),
                    snippet = "Weight 490 kg.",
                )
            val events =
                sut(
                    toolEngine = FakeToolEngine(includeCitation = false),
                    registry = FakeToolRegistry(),
                    recordSearch = RagRecordSearch { listOf(retrievalSource) },
                )("Analyze the weight data").toList()

            assertEquals("The average is 505 kg.", events.filterIsInstance<RagStreamEvent.Chunk>().last().text)
            assertEquals(
                7L,
                events
                    .filterIsInstance<RagStreamEvent.Sources>()
                    .single()
                    .sources
                    .single()
                    .recordId,
            )
        }

    @Test
    fun `tool loop stops when the provider repeats a tool call`() =
        runTest {
            val toolEngine = FakeToolEngine(alwaysRequestsTools = true)
            val registry = FakeToolRegistry()

            val events = sut(toolEngine, registry)("Analyze the weight data").toList()

            assertEquals(2, toolEngine.calls)
            assertEquals(1, registry.calls)
            assertEquals(
                EnAssistantStrings.analysisLimitReply,
                events.filterIsInstance<RagStreamEvent.Chunk>().last().text,
            )
        }

    @Test
    fun `grounded tool loop falls back to plain cloud completion after the safe limit`() =
        runTest {
            val toolEngine = FakeToolEngine(alwaysRequestsTools = true)
            val registry = FakeToolRegistry()
            val repos = FakeAnalysisRepos()
            repos.patients.patients = listOf(testPatient(1, "Bella"))
            repos.weights.entries = listOf(testWeight(7, 1, 505.0, LocalDate(2025, 2, 1)))
            val plain = PlainFallbackEngine()

            val events =
                sut(
                    toolEngine = toolEngine,
                    registry = registry,
                    plainEngine = plain,
                    analysisContextBuilder = repos.builder,
                )("Analyze the weight data").toList()

            assertEquals(2, toolEngine.calls)
            assertEquals(1, registry.calls)
            assertEquals(1, plain.calls)
            assertEquals(
                "I can still answer from the available context.",
                events.filterIsInstance<RagStreamEvent.Chunk>().last().text,
            )
        }

    @Test
    fun `empty tool turn falls back to plain cloud completion instead of a blank answer`() =
        runTest {
            val repos = FakeAnalysisRepos()
            repos.patients.patients = listOf(testPatient(1, "Bella"))
            repos.weights.entries = listOf(testWeight(7, 1, 505.0, LocalDate(2025, 2, 1)))
            val plain = PlainFallbackEngine()

            val events =
                sut(
                    toolEngine = SilentToolEngine(),
                    registry = FakeToolRegistry(),
                    plainEngine = plain,
                    analysisContextBuilder = repos.builder,
                )("Analyze the weight data").toList()

            assertEquals(1, plain.calls)
            assertEquals(
                "I can still answer from the available context.",
                events.filterIsInstance<RagStreamEvent.Chunk>().last().text,
            )
        }

    @Test
    fun `named analysis binds the resolved patient id to every executed tool call`() =
        runTest {
            val repos = FakeAnalysisRepos()
            repos.patients.patients = listOf(testPatient(1, "Bella"), testPatient(2, "Shadow"))
            val registry = FakeToolRegistry()

            sut(
                toolEngine = FakeToolEngine(),
                registry = registry,
                patientRepository = repos.patients,
            )("Analyze Bella's weight data").toList()

            assertEquals(1, registry.receivedCalls.size)
            assertEquals(
                RagToolExecutionScope(resolvedPatientId = 1L, requiresPatientName = true),
                registry.receivedCalls.single().executionScope,
            )
        }
}
