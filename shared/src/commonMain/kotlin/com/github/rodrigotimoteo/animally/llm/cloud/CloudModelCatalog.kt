package com.github.rodrigotimoteo.animally.llm.cloud

import io.ktor.client.HttpClient
import io.ktor.client.plugins.timeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.URLBuilder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.seconds

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
 * sent only for HTTPS endpoints; explicitly configured local runtimes (Ollama /
 * LM Studio) stay keyless even if a stale hosted key remains in settings.
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
        allowInsecureLocalEndpoint: Boolean = false,
    ): CloudModelsResult {
        if (!isValidCloudBaseUrl(baseUrl, allowInsecureLocalEndpoint)) {
            return CloudModelsResult.Failure(
                "Endpoint must use HTTPS; HTTP is limited to explicitly configured local runtimes",
            )
        }
        val url = cloudModelsUrl(baseUrl)
        return try {
            withTimeoutOrNull(MODEL_FETCH_TIMEOUT_SECONDS.seconds) {
                val response =
                    httpClient.get(url) {
                        apiKey
                            ?.takeIf(String::isNotBlank)
                            ?.takeUnless { isInsecureCloudBaseUrl(baseUrl) }
                            ?.let { header(HttpHeaders.Authorization, "Bearer $it") }
                        timeout {
                            connectTimeoutMillis = MODEL_CONNECT_TIMEOUT_MILLIS
                            requestTimeoutMillis = MODEL_FETCH_TIMEOUT_MILLIS
                            socketTimeoutMillis = MODEL_FETCH_TIMEOUT_MILLIS
                        }
                    }
                val code = response.status.value
                when {
                    code == HTTP_UNAUTHORIZED || code == HTTP_FORBIDDEN -> CloudModelsResult.Unauthorized
                    !isSuccessStatus(code) -> CloudModelsResult.Failure("HTTP $code")
                    else -> CloudModelsResult.Success(parseCloudModelsList(response.bodyAsText()))
                }
            } ?: CloudModelsResult.Failure("Request timed out")
        } catch (_: kotlinx.serialization.SerializationException) {
            CloudModelsResult.Failure("Unexpected response format")
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            CloudModelsResult.Failure(e.message ?: "Network error")
        }
    }

    private companion object {
        const val HTTP_UNAUTHORIZED = 401
        const val HTTP_FORBIDDEN = 403
        const val MODEL_FETCH_TIMEOUT_SECONDS = 15L
        const val MODEL_FETCH_TIMEOUT_MILLIS = MODEL_FETCH_TIMEOUT_SECONDS * 1_000L
        const val MODEL_CONNECT_TIMEOUT_MILLIS = 5_000L

        private fun isSuccessStatus(code: Int): Boolean = code in 200..299
    }
}

/**
 * Accepts HTTPS for hosted endpoints and HTTP only for exact loopback hosts used
 * by local runtimes. Keep this policy in the cloud transport package so direct
 * catalog callers cannot bypass the settings-layer readiness check.
 */
internal fun isValidCloudBaseUrl(
    value: String,
    allowInsecureLocalEndpoint: Boolean = false,
): Boolean {
    val normalized = value.trim()
    if (!ABSOLUTE_HTTP_URL_REGEX.matches(normalized)) return false
    val url = runCatching { URLBuilder(normalized).build() }.getOrNull() ?: return false
    val host = url.host.lowercase()
    return when (url.protocol.name.lowercase()) {
        "https" -> host.isNotBlank()
        "http" -> allowInsecureLocalEndpoint && host in LOOPBACK_HOSTS
        else -> false
    }
}

/** Returns true for syntactically valid HTTP URLs, which must remain keyless. */
internal fun isInsecureCloudBaseUrl(value: String): Boolean {
    val normalized = value.trim()
    if (!ABSOLUTE_HTTP_URL_REGEX.matches(normalized)) return false
    val url = runCatching { URLBuilder(normalized).build() }.getOrNull() ?: return false
    return url.protocol.name.equals("http", ignoreCase = true)
}

private val ABSOLUTE_HTTP_URL_REGEX = Regex("^https?://[^\\s/?#]+(?:[/?#][^\\s]*)?$", RegexOption.IGNORE_CASE)
private val LOOPBACK_HOSTS = setOf("localhost", "127.0.0.1", "::1", "[::1]")

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
        normalized.endsWith(CHAT_COMPLETIONS_PATH) ->
            normalized.removeSuffix(CHAT_COMPLETIONS_PATH) + "/models"
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
    return if (normalized.endsWith(CHAT_COMPLETIONS_PATH)) {
        normalized
    } else {
        normalized + CHAT_COMPLETIONS_PATH
    }
}

private const val CHAT_COMPLETIONS_PATH = "/chat/completions"
