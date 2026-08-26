package com.github.rodrigotimoteo.animally.domain.dictation.usecase

import com.github.rodrigotimoteo.animally.domain.dictation.IDictationCaptureRepository
import com.github.rodrigotimoteo.animally.domain.dictation.model.DictationCapture
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single

/** Persists one completed dictation independently of assistant-chat history. */
@Single
class SaveDictationCaptureUseCase(
    @Provided private val repository: IDictationCaptureRepository,
) {
    operator fun invoke(capture: DictationCapture): Long = repository.insert(capture)
}
