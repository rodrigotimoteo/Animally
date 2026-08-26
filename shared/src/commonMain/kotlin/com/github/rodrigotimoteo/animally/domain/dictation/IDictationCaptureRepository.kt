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

    /** Removes one capture and returns the number of deleted rows. */
    fun deleteById(id: Long): Long

    /** Removes every persisted capture. */
    fun deleteAll()
}
