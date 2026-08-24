package com.github.rodrigotimoteo.animally.llm

import com.github.rodrigotimoteo.animally.domain.search.ISearchRepository
import kotlinx.coroutines.flow.Flow
import org.koin.dsl.module

val llmModule =
    module {
        single { LlmConfig() }
        single { LlmEngine(get()) }
        // Assistant strings resolve once from the device locale at wiring time.
        single<AssistantStrings> { assistantStrings() }
        // Adapt the platform expect class to the testable RagLlmEngine seam.
        // The OR retry bypasses SearchUseCase (its tokenizer stars every token,
        // corrupting boolean operators) and hits the repository directly with
        // the FTS-safe expression built by AssistantPrompts.toFtsOrQuery.
        single {
            val engine = get<LlmEngine>()
            val searchRepository = get<ISearchRepository>()
            val orSearch =
                RagOrSearch { ftsQuery ->
                    searchRepository.search(ftsQuery, from = null, to = null, recordTypes = null)
                }
            GenerateRagResponseUseCase(
                get(),
                object : RagLlmEngine {
                    override fun generate(
                        prompt: String,
                        instructions: String,
                    ): Flow<String> = engine.generate(prompt, instructions)

                    override fun generateStreaming(
                        prompt: String,
                        instructions: String,
                    ): Flow<String> = engine.generateStreaming(prompt, instructions)
                },
                strings = get(),
                orSearch = orSearch,
            )
        }
    }
