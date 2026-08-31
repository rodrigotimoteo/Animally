package com.github.rodrigotimoteo.animally.di.presentation

import com.github.rodrigotimoteo.animally.bridge.ObjCHidden
import org.koin.core.annotation.Module
import org.koin.dsl.module

/**
 * Aggregator for all per-feature presentation modules.
 * Keeps Koin startup single-entry [PresentationModule.provide].
 */
@Module
@ObjCHidden
internal class PresentationModule {
    fun provide() =
        module {
            includes(
                PatientPresentationModule().provide(),
                MedicalPresentationModule().provide(),
                DiagnosticsPresentationModule().provide(),
                ReproductionPresentationModule().provide(),
                CorePresentationModule().provide(),
                InsightsPresentationModule().provide(),
            )
        }
}
