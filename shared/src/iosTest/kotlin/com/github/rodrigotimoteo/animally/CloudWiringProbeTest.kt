package com.github.rodrigotimoteo.animally

import com.github.rodrigotimoteo.animally.di.infra.initKoin
import com.github.rodrigotimoteo.animally.llm.GenerateRagResponseUseCase
import com.github.rodrigotimoteo.animally.llm.cloud.FmFirstRagLlmEngine
import org.koin.core.context.stopKoin
import kotlin.test.Test
import kotlin.test.assertNotNull

class CloudWiringProbeTest {
    @Test
    fun engineChainResolves() {
        try {
            val app = initKoin(null)
            val useCase = app.koin.get<GenerateRagResponseUseCase>()
            assertNotNull(useCase)
            val routing = app.koin.get<FmFirstRagLlmEngine>()
            assertNotNull(routing)
        } finally {
            stopKoin()
        }
    }
}
