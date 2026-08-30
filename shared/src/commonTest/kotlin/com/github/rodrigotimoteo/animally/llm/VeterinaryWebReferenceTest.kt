package com.github.rodrigotimoteo.animally.llm

import com.github.rodrigotimoteo.animally.domain.search.FakeSearchRepository
import com.github.rodrigotimoteo.animally.domain.search.usecase.SearchUseCase
import com.github.rodrigotimoteo.animally.domain.vetreference.VeterinaryWebSearchResult
import com.github.rodrigotimoteo.animally.domain.vetreference.VeterinaryWebSourceProvider
import com.github.rodrigotimoteo.animally.domain.vetreference.model.VeterinaryWebSource
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private class FakeVeterinaryWebSourceProvider(
    private val result: VeterinaryWebSearchResult,
) : VeterinaryWebSourceProvider {
    var calls = 0
    var lastQuery: String? = null

    override suspend fun search(query: String): VeterinaryWebSearchResult {
        calls++
        lastQuery = query
        return result
    }
}

private class HangingVeterinaryWebSourceProvider : VeterinaryWebSourceProvider {
    var calls = 0

    override suspend fun search(query: String): VeterinaryWebSearchResult {
        calls++
        awaitCancellation()
    }
}

private class WebReferenceLlmEngine(
    private val response: String = "Laminitis is a painful hoof condition. [WEB #1]",
) : RagLlmEngine {
    var calls = 0
    var lastPrompt = ""
    var lastInstructions = ""

    override fun generate(
        prompt: String,
        instructions: String,
    ): Flow<String> =
        flow {
            calls++
            lastPrompt = prompt
            lastInstructions = instructions
            emit(response)
        }
}

class VeterinaryWebReferenceTest {
    private val source =
        VeterinaryWebSource(
            sourceId = "pubmed:123456",
            title = "Laminitis in horses",
            publisher = "PubMed / Europe PMC",
            url = "https://pubmed.ncbi.nlm.nih.gov/123456/",
            excerpt = "Laminitis is a painful disease affecting the hoof.",
            publishedYear = "2024",
        )

    @Test
    fun `cloud medical question gets separate web sources and grounded prompt`() =
        runTest {
            val engine = WebReferenceLlmEngine()
            val provider = FakeVeterinaryWebSourceProvider(VeterinaryWebSearchResult.Success(listOf(source)))
            val events =
                GenerateRagResponseUseCase(
                    searchUseCase =
                        SearchUseCase(
                            FakeSearchRepository(),
                        ),
                    llmEngine = engine,
                    recordSearch = RagRecordSearch { emptyList() },
                    queryPolicyProvider = { RagQueryPolicy.CLOUD },
                    webSourceProvider = provider,
                    today = kotlinx.datetime.LocalDate(2026, 8, 29),
                )("What is laminitis in horses?").toList()

            assertEquals(1, provider.calls)
            assertEquals("laminitis horses", provider.lastQuery)
            assertEquals(1, engine.calls)
            assertTrue(engine.lastPrompt.contains("[WEB #1] Laminitis in horses"))
            assertTrue(engine.lastPrompt.contains(source.excerpt))
            assertFalse(engine.lastPrompt.contains(source.url), "the model does not need the clickable URL")
            assertTrue(engine.lastInstructions.contains("WEB REFERENCES"))
            assertTrue(events.any { it is RagStreamEvent.WebSources && it.sources == listOf(source) })
            assertEquals("Laminitis is a painful hoof condition.", events.filterIsInstance<RagStreamEvent.Chunk>().last().text)
        }

    @Test
    fun `web answer without a valid citation is replaced with a safe response`() =
        runTest {
            val engine = WebReferenceLlmEngine("Laminitis is a painful hoof condition from memory.")
            val provider = FakeVeterinaryWebSourceProvider(VeterinaryWebSearchResult.Success(listOf(source)))
            val events =
                GenerateRagResponseUseCase(
                    searchUseCase = SearchUseCase(FakeSearchRepository()),
                    llmEngine = engine,
                    recordSearch = RagRecordSearch { emptyList() },
                    queryPolicyProvider = { RagQueryPolicy.CLOUD },
                    webSourceProvider = provider,
                )("What is laminitis in horses?").toList()

            assertEquals(1, engine.calls)
            val answer = events.filterIsInstance<RagStreamEvent.Chunk>().last().text
            assertTrue(answer.startsWith(EnAssistantStrings.webReferenceAnswerUnavailable))
            assertTrue(answer.contains(source.excerpt))
            assertTrue(events.any { it is RagStreamEvent.WebSources })
        }

    @Test
    fun `on-device policy never calls the public reference provider`() =
        runTest {
            val engine = WebReferenceLlmEngine()
            val provider = FakeVeterinaryWebSourceProvider(VeterinaryWebSearchResult.Success(listOf(source)))
            GenerateRagResponseUseCase(
                searchUseCase = SearchUseCase(FakeSearchRepository()),
                llmEngine = engine,
                recordSearch = RagRecordSearch { emptyList() },
                queryPolicyProvider = { RagQueryPolicy.ON_DEVICE },
                webSourceProvider = provider,
            )("What is laminitis in horses?").toList()

            assertEquals(0, provider.calls)
            assertEquals(0, engine.calls)
        }

    @Test
    fun `unavailable public reference service fails closed without a model call`() =
        runTest {
            val engine = WebReferenceLlmEngine("Laminitis is a painful hoof condition from general knowledge.")
            val provider = FakeVeterinaryWebSourceProvider(VeterinaryWebSearchResult.Unavailable)
            val events =
                GenerateRagResponseUseCase(
                    searchUseCase = SearchUseCase(FakeSearchRepository()),
                    llmEngine = engine,
                    recordSearch = RagRecordSearch { emptyList() },
                    queryPolicyProvider = { RagQueryPolicy.CLOUD },
                    webSourceProvider = provider,
                )("What is laminitis in horses?").toList()
            val chunks =
                events.filterIsInstance<RagStreamEvent.Chunk>()

            assertEquals(0, engine.calls)
            assertTrue(events.none { it is RagStreamEvent.WebSources })
            assertEquals(EnAssistantStrings.webReferenceUnavailable, chunks.last().text)
        }

    @Test
    fun `hanging public reference service is bounded and fails closed without a model call`() =
        runTest {
            val engine = WebReferenceLlmEngine("Laminitis is a painful hoof condition from general knowledge.")
            val provider = HangingVeterinaryWebSourceProvider()
            val chunks =
                GenerateRagResponseUseCase(
                    searchUseCase = SearchUseCase(FakeSearchRepository()),
                    llmEngine = engine,
                    recordSearch = RagRecordSearch { emptyList() },
                    queryPolicyProvider = { RagQueryPolicy.CLOUD },
                    webSourceProvider = provider,
                )("What is laminitis in horses?").toList().filterIsInstance<RagStreamEvent.Chunk>()

            assertEquals(1, provider.calls)
            assertEquals(0, engine.calls)
            assertEquals(EnAssistantStrings.webReferenceUnavailable, chunks.last().text)
        }
}
