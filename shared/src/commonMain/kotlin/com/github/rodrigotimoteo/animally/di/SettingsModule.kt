package com.github.rodrigotimoteo.animally.di

import com.github.rodrigotimoteo.animally.domain.settings.usecase.WipeAllDataUseCase
import org.koin.dsl.module

/**
 * Settings-lane use cases registered with manual DSL (not annotations) so the
 * settings lane stays independent of generated-module regeneration — same
 * pattern as [dictationModule].
 */
val settingsModule =
    module {
        single { WipeAllDataUseCase(get(), get(), get(), get()) }
    }
