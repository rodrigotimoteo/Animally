package com.github.rodrigotimoteo.animally.llm

import org.koin.dsl.module

val llmModule =
    module {
        single { LlmConfig() }
        single { LlmEngine(get()) }
        // Adapt the platform expect class to the testable RagLlmEngine seam.
        single {
            GenerateRagResponseUseCase(
                get(),
                RagLlmEngine { prompt, instructions ->
                    get<LlmEngine>().generate(prompt, instructions)
                },
            )
        }
    }
