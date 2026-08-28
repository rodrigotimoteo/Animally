package com.github.rodrigotimoteo.animally.domain.dictation

import com.github.rodrigotimoteo.animally.domain.dictation.model.DictationCapture

/** Persistence boundary for the local dictation archive. */
interface IDictationCaptureRepository {
    /** Returns captures newest first. */
    fun getAll(): List<DictationCapture>

    /** Inserts one capture and returns its generated id. */
    fun insert(capture: DictationCapture): Long

    /** Returns the capture with [id], if it exists. */
    fun getById(id: Long): DictationCapture?

    /** Updates the searchable transcript without changing audio metadata. */
    fun updateTranscript(
        id: Long,
        transcript: String,
    ): Long

    /** Removes one capture and returns the number of deleted rows. */
    fun deleteById(id: Long): Long

    /** Removes every persisted capture. */
    fun deleteAll()
}
