package com.github.rodrigotimoteo.animally.domain.dictation.model

import kotlin.time.Instant

/** A completed voice capture and its optional platform audio artifact. */
data class DictationCapture(
    val id: Long = 0L,
    val transcript: String,
    val audioPath: String?,
    val durationMillis: Long?,
    val capturedAt: Instant,
)
