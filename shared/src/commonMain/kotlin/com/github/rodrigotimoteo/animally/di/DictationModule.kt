package com.github.rodrigotimoteo.animally.di

import com.github.rodrigotimoteo.animally.domain.dictation.InsertSuggestionsUseCase
import com.github.rodrigotimoteo.animally.domain.dictation.ValidateSuggestionsUseCase
import com.github.rodrigotimoteo.animally.domain.patient.usecase.ResolvePatientUseCase
import com.github.rodrigotimoteo.animally.presentation.dictation.DictationViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * Voice-dictation pipeline: validation, patient resolution, insertion and the
 * review view model. Manual DSL (not annotations) so the dictation lane stays
 * independent of generated-module regeneration.
 */
val dictationModule =
    module {
        single { ValidateSuggestionsUseCase() }
        single { ResolvePatientUseCase(get()) }
        single { InsertSuggestionsUseCase(get(), get(), get()) }
        viewModel { DictationViewModel(get(), get()) }
    }
