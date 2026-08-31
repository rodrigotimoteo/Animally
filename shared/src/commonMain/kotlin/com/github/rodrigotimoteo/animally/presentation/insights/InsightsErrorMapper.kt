package com.github.rodrigotimoteo.animally.presentation.insights

/**
 * Shared error mapper for Insights presentation layer.
 * Prevents leaking SQL/driver internals to UI and centralizes fallback handling.
 */
object InsightsErrorMapper {
    fun map(
        throwable: Throwable,
        fallback: String,
    ): String {
        val raw = throwable.message?.trim()
        if (raw.isNullOrBlank()) return fallback
        val lower = raw.lowercase()
        if (lower.contains("sql") || lower.contains("sqlite") || lower.contains("driver")) {
            return fallback
        }
        return raw.ifBlank { fallback }
    }
}
