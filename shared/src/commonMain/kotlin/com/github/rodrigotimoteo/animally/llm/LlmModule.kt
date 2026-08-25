package com.github.rodrigotimoteo.animally.llm

import com.github.rodrigotimoteo.animally.domain.search.ISearchRepository
import com.github.rodrigotimoteo.animally.llm.cloud.CloudLlmConfig
import com.github.rodrigotimoteo.animally.llm.cloud.CloudModelCatalog
import com.github.rodrigotimoteo.animally.llm.cloud.CloudRagLlmEngine
import com.github.rodrigotimoteo.animally.llm.cloud.FmFirstRagLlmEngine
import com.github.rodrigotimoteo.animally.presentation.settings.CloudLlmSettingsStore
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json
import org.koin.dsl.module

val llmModule =
    module {
        single { LlmConfig() }
        single { LlmEngine(get()) }
        // Assistant strings resolve once from the device locale at wiring time.
        single<AssistantStrings> { assistantStrings() }
        // CloudLlmSettingsStore is provided by CloudLlmModule (Koin annotations,
        // picked up by the AppModule component scan) so the annotation processor
        // can satisfy SettingsViewModel's constructor dependency.
        single {
            // MUST install ContentNegotiation: the cloud engine posts a
            // @Serializable ChatCompletionRequest via setBody(dto), and without a
            // serializer plugin Ktor fails the request before it is ever sent
            // ("Fail to prepare request body for sending") - which surfaced to
            // users as "Response cut short" on EVERY cloud answer. This client is
            // deliberately separate from HttpClientModule's: it adds HttpTimeout
            // so SSE streams get an inactivity cap instead of hanging forever.
            HttpClient {
                install(ContentNegotiation) {
                    json(Json { ignoreUnknownKeys = true })
                }
                install(HttpTimeout)
            }
        }
        // Models-list discovery for the Cloud AI settings (GET {baseUrl}/models);
        // shares the engine/timeout wiring with the chat-completions client above.
        single { CloudModelCatalog(get()) }
        // Cloud engine: OpenAI-compatible chat completions over Ktor. Config resolved
        // per request from settings, so edits (key/model/URL) apply without restart;
        // the platform service loader picks the transport engine (Android/Darwin/CIO).
        single<CloudRagLlmEngine> {
            val settings = get<CloudLlmSettingsStore>()
            CloudRagLlmEngine(
                httpClient = get(),
                configProvider = {
                    CloudLlmConfig(
                        baseUrl = settings.baseUrl(),
                        model = settings.model(),
                        apiKey = settings.apiKey().orEmpty(),
                    )
                },
            )
        }
        // Routing: on-device Foundation Models first; the cloud engine answers only
        // when the user enabled it, stored a key, AND the primary is unavailable or
        // fails/times out. The wrapper announces which engine served each request so
        // the assistant UI can badge cloud answers.
        single<FmFirstRagLlmEngine> {
            val settings = get<CloudLlmSettingsStore>()
            FmFirstRagLlmEngine(
                primary = LlmEngineRagAdapter(get<LlmEngine>()),
                fallback = get<CloudRagLlmEngine>(),
                isFallbackEligible = { settings.isEnabled() && !settings.apiKey().isNullOrBlank() },
                isPrimaryAvailable = { get<LlmEngine>().availability() is LlmAvailability.Available },
            )
        }
        // Retrieval goes through the repository's RAG snippet variant: chunks
        // carry a 24-token FTS5 window instead of full record text so long
        // consultations cannot eat the context budget. The OR retry bypasses
        // SearchUseCase (its tokenizer stars every token, corrupting boolean
        // operators) and hits the repository directly with the FTS-safe
        // expressions built by AssistantPrompts.
        single {
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
                get<FmFirstRagLlmEngine>(),
                strings = get(),
                recordSearch = recordSearch,
                patientRepository = get(),
                analysisContextBuilder = analysisContextBuilder,
            )
        }
    }

/** Adapts the platform [LlmEngine] expect class to the domain-side [RagLlmEngine] seam. */
private class LlmEngineRagAdapter(
    private val engine: LlmEngine,
) : RagLlmEngine {
    override fun generate(
        prompt: String,
        instructions: String,
    ): Flow<String> = engine.generate(prompt, instructions)

    override fun generateStreaming(
        prompt: String,
        instructions: String,
    ): Flow<String> = engine.generateStreaming(prompt, instructions)
}
