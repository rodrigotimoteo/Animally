package com.github.rodrigotimoteo.animally.presentation.insights

import android.util.Log

internal actual fun platformLogError(
    tag: String,
    message: String,
    throwable: Throwable?,
) {
    try {
        if (throwable != null) {
            Log.e(tag, message, throwable)
        } else {
            Log.e(tag, message)
        }
    } catch (_: Throwable) {
        // Fallback for host-unit tests where android.util.Log is stubbed
        java.util.logging.Logger
            .getLogger(tag)
            .severe("$message ${throwable?.stackTraceToString() ?: ""}")
    }
}
