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
        // Retrieval goes through the repository's RAG snippet variant: chunks
        // carry a 24-token FTS5 window instead of full record text so long
        // consultations cannot eat the context budget. The OR retry bypasses
        // SearchUseCase (its tokenizer stars every token, corrupting boolean
        // operators) and hits the repository directly with the FTS-safe
        // expressions built by AssistantPrompts.
        single {
            val engine = get<LlmEngine>()
            val searchRepository = get<ISearchRepository>()
            val recordSearch =
                RagRecordSearch { ftsQuery ->
                    searchRepository.searchSnippets(ftsQuery, from = null, to = null, recordTypes = null)
                }
            // Analysis mode: Kotlin computes count/trend/overdue summaries
            // from the repositories; the model only narrates them.
            val analysisContextBuilder =
                AnalysisContextBuilder(
                    patientRepository = get(),
                    weightRepository = get(),
                    vaccinationRepository = get(),
                    dewormingRepository = get(),
                    farrierVisitRepository = get(),
                    gestationRepository = get(),
                )
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
                recordSearch = recordSearch,
                patientRepository = get(),
                analysisContextBuilder = analysisContextBuilder,
            )
        }
    }
