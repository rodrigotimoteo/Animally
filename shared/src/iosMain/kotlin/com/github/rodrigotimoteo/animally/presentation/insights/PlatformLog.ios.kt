package com.github.rodrigotimoteo.animally.presentation.insights

import platform.Foundation.NSLog

internal actual fun platformLogError(
    tag: String,
    message: String,
    throwable: Throwable?,
) {
    val throwablePart = throwable?.let { " throwable=${it.message} ${it.stackTraceToString()}" } ?: ""
    NSLog("[%@] %@: %@%@", tag, "ERROR", message, throwablePart)
}
