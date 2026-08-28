package com.github.rodrigotimoteo.animally.domain.dictation.usecase

import com.github.rodrigotimoteo.animally.domain.dictation.IDictationCaptureRepository
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single

/** Updates the user-reviewed transcript while preserving its original audio. */
@Single
class UpdateDictationCaptureTranscriptUseCase(
    @Provided private val repository: IDictationCaptureRepository,
) {
    /** Returns true when a capture with [id] was updated. */
    operator fun invoke(
        id: Long,
        transcript: String,
    ): Boolean {
        val normalized = transcript.trim()
        require(normalized.isNotEmpty()) { "There is no transcript to save." }
        return repository.updateTranscript(id, normalized) > 0L
    }
}
