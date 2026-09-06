package com.github.rodrigotimoteo.animally.domain.dictation

/**
 * Domain port for app-owned audio and attachment file cleanup.
 *
 * The data layer implements this via platform-specific `FileStorage` so
 * [com.github.rodrigotimoteo.animally.domain.dictation.usecase.DeleteDictationCaptureUseCase]
 * no longer depends on `data.storage.FileStorage` directly.
 */
interface DictationFilePort {
    /**
     * Deletes an app-owned audio or attachment file at [path].
     *
     * @return `true` when a file was removed.
     */
    fun delete(path: String): Boolean
}
