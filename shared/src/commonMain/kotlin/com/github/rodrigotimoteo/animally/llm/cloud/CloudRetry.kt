package com.github.rodrigotimoteo.animally.llm.cloud

import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess

/**
 * Retry / transport-error delegate extracted from CloudRagLlmEngine.
 * Currently validates HTTP success and compacts error bodies for
 * surfaced messages. Retry policy can be introduced here without touching
 * the streaming facade.
 */
internal object CloudRetry {
    suspend fun ensureSuccessful(response: HttpResponse) {
        if (response.status.isSuccess()) return
        val detail = response.bodyAsText().compactCloudError()
        val suffix = detail.takeIf(String::isNotBlank)?.let { ": $it" }.orEmpty()
        error("Cloud LLM request failed: HTTP ${response.status.value}$suffix")
    }

    fun String.compactCloudError(): String = replace(Regex("\\s+"), " ").trim().take(MAX_ERROR_DETAIL_CHARS)

    const val MAX_ERROR_DETAIL_CHARS = 240
}
