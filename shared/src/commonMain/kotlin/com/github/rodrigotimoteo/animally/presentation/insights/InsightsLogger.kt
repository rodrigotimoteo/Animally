package com.github.rodrigotimoteo.animally.presentation.insights

import org.koin.core.annotation.Single

/**
 * Simple logger for Insights presentation layer.
 * Keeps raw error diagnostics out of UI while preserving them for diagnostics.
 * Injected via Koin; commonMain delegates to platform shim (Android Log.e / NSLog) so errors surface in Logcat/Console.
 */
interface InsightsLogger {
    fun e(
        throwable: Throwable,
        message: String,
    )

    fun e(
        message: String,
        throwable: Throwable? = null,
    )
}

internal expect fun platformLogError(
    tag: String,
    message: String,
    throwable: Throwable?,
)

@Single(binds = [InsightsLogger::class])
class DefaultInsightsLogger : InsightsLogger {
    override fun e(
        throwable: Throwable,
        message: String,
    ) {
        platformLogError(TAG, message, throwable)
    }

    override fun e(
        message: String,
        throwable: Throwable?,
    ) {
        if (throwable != null) {
            e(throwable, message)
        } else {
            platformLogError(TAG, message, null)
        }
    }

    private companion object {
        const val TAG = "Insights"
    }
}

/**
 * Test/no-op logger that discards messages. Use in tests to avoid platform dependencies.
 */
object NoOpInsightsLogger : InsightsLogger {
    override fun e(
        throwable: Throwable,
        message: String,
    ) = Unit

    override fun e(
        message: String,
        throwable: Throwable?,
    ) = Unit
}
