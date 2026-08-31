package com.github.rodrigotimoteo.animally.presentation.insights

internal actual fun platformLogError(
    tag: String,
    message: String,
    throwable: Throwable?,
) {
    val throwablePart = throwable?.let { " ${it.stackTraceToString()}" } ?: ""
    java.util.logging.Logger
        .getLogger(tag)
        .severe("ERROR: $message$throwablePart")
}
