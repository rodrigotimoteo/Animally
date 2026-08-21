package com.github.rodrigotimoteo.animally.llm

import org.koin.dsl.module

val llmModule =
    module {
        single { LlmConfig() }
        single { LlmEngine(get()) }
        single { GenerateRagResponseUseCase(get(), get()) }
    }
