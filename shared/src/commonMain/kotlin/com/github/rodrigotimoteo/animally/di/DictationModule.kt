package com.github.rodrigotimoteo.animally.di

import com.github.rodrigotimoteo.animally.di.dispatchers.DispatchersModule.Companion.IO_DISPATCHER
import com.github.rodrigotimoteo.animally.domain.dictation.InsertSuggestionsUseCase
import com.github.rodrigotimoteo.animally.domain.dictation.ValidateSuggestionsUseCase
import com.github.rodrigotimoteo.animally.domain.patient.IPatientRepository
import com.github.rodrigotimoteo.animally.domain.patient.usecase.ResolvePatientUseCase
import com.github.rodrigotimoteo.animally.llm.GenerateDictationSessionUseCase
import com.github.rodrigotimoteo.animally.presentation.dictation.DictationViewModel
import com.github.rodrigotimoteo.animally.presentation.settings.CloudLlmSettingsStore
import com.github.rodrigotimoteo.animally.presentation.settings.isReadyForCloudRouting
import kotlinx.coroutines.CoroutineDispatcher
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
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
        single {
            val patientRepository = get<IPatientRepository>()
            InsertSuggestionsUseCase(
                saveUltrasoundUseCase = get(),
                saveWeightUseCase = get(),
                saveDewormingUseCase = get(),
                patientExists = { patientId -> patientRepository.getPatientById(patientId) != null },
            )
        }
        viewModel {
            DictationViewModel(
                validateSuggestionsUseCase = get(),
                resolvePatientUseCase = get(),
                getDictationCapturesUseCase = get(),
                saveDictationCaptureUseCase = get(),
                deleteDictationCaptureUseCase = get(),
                ioDispatcher = get<CoroutineDispatcher>(named(IO_DISPATCHER)),
                generateDictationSession = get<GenerateDictationSessionUseCase>(),
                isCloudReady = { get<CloudLlmSettingsStore>().isReadyForCloudRouting() },
            )
        }
    }
