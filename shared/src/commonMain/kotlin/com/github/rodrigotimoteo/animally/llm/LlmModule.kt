package com.github.rodrigotimoteo.animally.llm

import com.github.rodrigotimoteo.animally.data.vetreference.CompositeVeterinaryWebSourceProvider
import com.github.rodrigotimoteo.animally.data.vetreference.EuropePmcVeterinaryWebSourceProvider
import com.github.rodrigotimoteo.animally.data.vetreference.MsdVeterinaryWebSourceProvider
import com.github.rodrigotimoteo.animally.domain.search.ISearchRepository
import com.github.rodrigotimoteo.animally.domain.vetreference.VeterinaryWebSourceProvider
import com.github.rodrigotimoteo.animally.llm.cloud.CloudLlmConfig
import com.github.rodrigotimoteo.animally.llm.cloud.CloudLlmProviderPreset
import com.github.rodrigotimoteo.animally.llm.cloud.CloudModelCatalog
import com.github.rodrigotimoteo.animally.llm.cloud.CloudRagLlmEngine
import com.github.rodrigotimoteo.animally.llm.cloud.FmFirstRagLlmEngine
import com.github.rodrigotimoteo.animally.presentation.settings.CloudLlmSettingsStore
import com.github.rodrigotimoteo.animally.presentation.settings.isReadyForCloudRouting
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
                    json(
                        Json {
                            ignoreUnknownKeys = true
                            // A null local-only field must not become `"max_tokens": null`.
                            explicitNulls = false
                        },
                    )
                }
                install(HttpTimeout)
            }
        }
        // Models-list discovery for the Cloud AI settings (GET {baseUrl}/models);
        // shares the engine/timeout wiring with the chat-completions client above.
        single { CloudModelCatalog(get()) }
        // Public literature lookup for cloud-only general medical questions.
        // The provider performs the privacy/topic gate before any request and
        // returns references separately from local patient search results.
        single<VeterinaryWebSourceProvider> {
            CompositeVeterinaryWebSourceProvider(
                providers =
                    listOf(
                        MsdVeterinaryWebSourceProvider(get()),
                        EuropePmcVeterinaryWebSourceProvider(get()),
                    ),
            )
        }
        // Cloud engine: OpenAI-compatible chat completions over Ktor. Config resolved
        // per request from settings, so edits (key/model/URL) apply without restart;
        // the platform service loader picks the transport engine (Android/Darwin/CIO).
        single<CloudRagLlmEngine> {
            val settings = get<CloudLlmSettingsStore>()
            CloudRagLlmEngine(
                httpClient = get(),
                configProvider = {
                    val provider = CloudLlmProviderPreset.fromId(settings.presetId())
                    CloudLlmConfig(
                        baseUrl = settings.baseUrl(),
                        model = settings.model(),
                        apiKey = settings.apiKey().orEmpty(),
                        maxTokens = provider.takeIf { it.isLocalRuntime }?.let { CloudLlmConfig.DEFAULT_MAX_TOKENS },
                    )
                },
            )
        }
        // Routing: on-device Foundation Models first; the cloud engine answers only
        // when the user enabled it, the selected provider is configured, AND the
        // primary is unavailable or fails/times out. Local runtimes intentionally do
        // not require an API key; hosted providers do. The wrapper announces which
        // engine served each request so the assistant UI can badge cloud answers.
        single<FmFirstRagLlmEngine> {
            val settings = get<CloudLlmSettingsStore>()
            FmFirstRagLlmEngine(
                primary = LlmEngineRagAdapter(get<LlmEngine>()),
                fallback = get<CloudRagLlmEngine>(),
                isFallbackEligible = { settings.isReadyForCloudRouting() },
                isPrimaryAvailable = { get<LlmEngine>().availability() is LlmAvailability.Available },
            )
        }
        // Dictation on iPhone uses this routed engine when Foundation Models
        // structured generation is unavailable. The extraction use case owns
        // the JSON contract; the Swift edge only starts the request.
        single { GenerateDictationSessionUseCase(get<FmFirstRagLlmEngine>()) }
        // Retrieval goes through the repository's RAG snippet variant: chunks
        // carry a 48-token FTS5 window instead of full record text so long
        // consultations cannot eat the context budget. The OR retry bypasses
        // SearchUseCase (its tokenizer stars every token, corrupting boolean
        // operators) and hits the repository directly with the FTS-safe
        // expressions built by AssistantPrompts.
        single {
            val searchRepository = get<ISearchRepository>()
            val recordSearch =
                object : RagRecordSearch {
                    override fun search(ftsQuery: String) =
                        searchRepository.searchSnippets(
                            ftsQuery,
                            from = null,
                            to = null,
                            recordTypes = null,
                        )

                    override fun search(
                        ftsQuery: String,
                        from: kotlinx.datetime.LocalDate?,
                        to: kotlinx.datetime.LocalDate?,
                    ) = searchRepository.searchSnippets(
                        ftsQuery,
                        from = from,
                        to = to,
                        recordTypes = null,
                    )

                    override fun searchByDateRange(
                        from: kotlinx.datetime.LocalDate,
                        to: kotlinx.datetime.LocalDate,
                    ) = searchRepository.searchByDateRange(from, to)
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
                    reproductionRepository = get(),
                    calculateGestationUseCase = get(),
                )
            val analysisToolRegistry =
                AnalysisToolRegistry(
                    patientRepository = get(),
                    weightRepository = get(),
                    vaccinationRepository = get(),
                    dewormingRepository = get(),
                    farrierVisitRepository = get(),
                    gestationRepository = get(),
                    calculateGestationUseCase = get(),
                )
            val routedEngine = get<FmFirstRagLlmEngine>()
            GenerateRagResponseUseCase(
                get(),
                routedEngine,
                strings = get(),
                recordSearch = recordSearch,
                patientRepository = get(),
                ownerRepository = get(),
                analysisContextBuilder = analysisContextBuilder,
                queryPolicyProvider = { routedEngine.queryPolicy() },
                queryPolicyForQuestion = { question -> routedEngine.queryPolicy(question) },
                toolCallingEngine = routedEngine,
                toolRegistry = analysisToolRegistry,
                webSourceProvider = get(),
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
