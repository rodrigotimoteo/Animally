package com.github.rodrigotimoteo.animally.domain.dictation.usecase

import com.github.rodrigotimoteo.animally.domain.dictation.IDictationCaptureRepository
import com.github.rodrigotimoteo.animally.domain.dictation.model.DictationCapture
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single

/** Loads all locally retained dictation captures, newest first. */
@Single
class GetDictationCapturesUseCase(
    @Provided private val repository: IDictationCaptureRepository,
) {
    operator fun invoke(): List<DictationCapture> = repository.getAll()
}
