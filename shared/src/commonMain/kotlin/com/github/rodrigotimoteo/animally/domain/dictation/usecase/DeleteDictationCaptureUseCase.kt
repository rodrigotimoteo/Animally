package com.github.rodrigotimoteo.animally.domain.dictation.usecase

import com.github.rodrigotimoteo.animally.domain.dictation.DictationFilePort
import com.github.rodrigotimoteo.animally.domain.dictation.IDictationCaptureRepository
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single

/** Deletes one capture and best-effort cleans up its platform audio file. */
@Single
class DeleteDictationCaptureUseCase(
    @Provided private val repository: IDictationCaptureRepository,
    @Provided private val filePort: DictationFilePort,
) {
    /** Returns `true` when a database row was removed. */
    operator fun invoke(id: Long): Boolean {
        val capture = repository.getById(id) ?: return false
        val deleted = repository.deleteById(id) > 0L
        if (deleted) {
            capture.audioPath?.let { path ->
                runCatching { filePort.delete(path) }
            }
        }
        return deleted
    }
}
