package com.github.rodrigotimoteo.animally.llm.cloud

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Outcome of a models-list fetch against an OpenAI-compatible endpoint.
 */
sealed interface CloudModelsResult {
    /** The endpoint answered with a (possibly empty) model list. */
    data class Success(
        val models: List<String>,
    ) : CloudModelsResult

    /** HTTP 401/403: the API key was rejected. */
    data object Unauthorized : CloudModelsResult

    /** Network failure, non-401 HTTP error, or unparseable body. */
    data class Failure(
        val message: String,
    ) : CloudModelsResult
}

@Serializable
private data class ModelsListResponse(
    val data: List<ModelInfo> = emptyList(),
)

@Serializable
private data class ModelInfo(
    val id: String,
)

/**
 * Parses an OpenAI models-list payload (`{"data":[{"id":"..."}]}`) into sorted,
 * de-duplicated model ids. Unknown keys are ignored; a missing `data` array
 * yields an empty list. Throws on structurally invalid JSON so callers can
 * distinguish "no models" from "not a models endpoint".
 *
 * Pure function so contract tests drive the exact production decode path.
 */
fun parseCloudModelsList(jsonText: String): List<String> {
    val json =
        Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }
    return json
        .decodeFromString<ModelsListResponse>(jsonText)
        .data
        .map { it.id }
        .filter(String::isNotBlank)
        .distinct()
        .sorted()
}

/**
 * Fetches the `/models` list from an OpenAI-compatible endpoint using the same
 * [HttpClient] engine wiring as [CloudRagLlmEngine]. The Authorization header is
 * only sent when [apiKey] is non-blank, so local runtimes (Ollama / LM Studio)
 * work keyless.
 */
class CloudModelCatalog(
    private val httpClient: HttpClient,
) {
    /**
     * GETs `{baseUrl}/models` and decodes the response. [baseUrl] is the same
     * value stored in settings (e.g. `https://api.openai.com/v1`).
     */
    suspend fun fetch(
        baseUrl: String,
        apiKey: String?,
    ): CloudModelsResult {
        val url = cloudModelsUrl(baseUrl)
        return try {
            val response =
                httpClient.get(url) {
                    apiKey?.takeIf(String::isNotBlank)?.let { header(HttpHeaders.Authorization, "Bearer $it") }
                }
            val code = response.status.value
            when {
                code == HTTP_UNAUTHORIZED || code == HTTP_FORBIDDEN -> CloudModelsResult.Unauthorized
                !isSuccessStatus(code) -> CloudModelsResult.Failure("HTTP $code")
                else -> CloudModelsResult.Success(parseCloudModelsList(response.bodyAsText()))
            }
        } catch (_: kotlinx.serialization.SerializationException) {
            CloudModelsResult.Failure("Unexpected response format")
        } catch (e: Exception) {
            CloudModelsResult.Failure(e.message ?: "Network error")
        }
    }

    private companion object {
        const val HTTP_UNAUTHORIZED = 401
        const val HTTP_FORBIDDEN = 403

        private fun isSuccessStatus(code: Int): Boolean = code in 200..299
    }
}

/**
 * Builds the OpenAI-compatible models URL from either an API root or a full
 * chat-completions URL. Settings presets store API roots, while the default
 * OpenAI value historically stored the full chat endpoint; both forms remain
 * valid user input.
 */
internal fun cloudModelsUrl(baseUrl: String): String {
    val normalized = baseUrl.trim().trimEnd('/')
    return when {
        normalized.endsWith("/models") -> normalized
        normalized.endsWith("/chat/completions") ->
            normalized.removeSuffix("/chat/completions") + "/models"
        else -> "$normalized/models"
    }
}

/**
 * Builds the chat-completions URL from either an API root or a full endpoint.
 * This is important for provider presets such as OpenCode Go, whose stored
 * value is `/v1` but whose request endpoint is `/v1/chat/completions`.
 */
internal fun cloudChatCompletionsUrl(baseUrl: String): String {
    val normalized = baseUrl.trim().trimEnd('/')
    return if (normalized.endsWith("/chat/completions")) {
        normalized
    } else {
        "$normalized/chat/completions"
    }
}
