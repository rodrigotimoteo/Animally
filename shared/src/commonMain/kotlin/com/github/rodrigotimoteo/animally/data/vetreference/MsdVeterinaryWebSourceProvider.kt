package com.github.rodrigotimoteo.animally.data.vetreference

import com.github.rodrigotimoteo.animally.domain.vetreference.VeterinaryWebQuery
import com.github.rodrigotimoteo.animally.domain.vetreference.VeterinaryWebSearchResult
import com.github.rodrigotimoteo.animally.domain.vetreference.VeterinaryWebSourceProvider
import com.github.rodrigotimoteo.animally.domain.vetreference.model.VeterinaryWebSource
import io.ktor.client.HttpClient
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.URLBuilder
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.time.Duration.Companion.seconds

/**
 * Public, read-only search backed by the MSD Veterinary Manual.
 *
 * The site is a veterinary reference published by MSD/Merck. Its public
 * search API returns short, reviewed-topic summaries and relative article
 * paths; only those fields are admitted into the assistant context.
 */
class MsdVeterinaryWebSourceProvider(
    private val httpClient: HttpClient,
    private val maxResults: Int = DEFAULT_MAX_RESULTS,
) : VeterinaryWebSourceProvider {
    override suspend fun search(query: String): VeterinaryWebSearchResult {
        val safeQuery = VeterinaryWebQuery.extractTopic(query) ?: return VeterinaryWebSearchResult.Success(emptyList())
        val urlBuilder = URLBuilder(SEARCH_ENDPOINT)
        urlBuilder.parameters.append("q", safeQuery)
        urlBuilder.parameters.append("rows", maxResults.coerceIn(1, MAX_RESULTS_LIMIT).toString())
        urlBuilder.parameters.append("start", "0")
        urlBuilder.parameters.append("model", "SearchResult")
        urlBuilder.parameters.append("language", "en")
        return try {
            withTimeoutOrNull(REQUEST_TIMEOUT_SECONDS.seconds) {
                val response =
                    httpClient.get(urlBuilder.build()) {
                        accept(ContentType.Application.Json)
                    }
                if (!response.status.isSuccess()) return@withTimeoutOrNull VeterinaryWebSearchResult.Unavailable
                VeterinaryWebSearchResult.Success(parseMsdSources(response.bodyAsText(), maxResults))
            } ?: VeterinaryWebSearchResult.Unavailable
        } catch (ce: CancellationException) {
            throw ce
        } catch (_: Throwable) {
            VeterinaryWebSearchResult.Unavailable
        }
    }

    private companion object {
        const val SEARCH_ENDPOINT = "https://www.msdvetmanual.com/api/search/search"
        const val DEFAULT_MAX_RESULTS = 3
        const val REQUEST_TIMEOUT_SECONDS = 12L
    }
}

/** Parses only the MSD fields used by the app; unknown response fields are ignored. */
internal fun parseMsdSources(
    jsonText: String,
    maxResults: Int = 3,
): List<VeterinaryWebSource> {
    val json = Json { ignoreUnknownKeys = true }
    val document = runCatching { json.parseToJsonElement(jsonText) as? JsonObject }.getOrNull() ?: return emptyList()
    val docs =
        runCatching {
            document["data"]
                ?.jsonObject
                ?.get("response")
                ?.jsonObject
                ?.get("docs")
                ?.jsonArray
        }.getOrNull() ?: emptyList<JsonObject>()

    return docs
        .mapNotNull { it as? JsonObject }
        .mapNotNull(::toMsdVeterinaryWebSource)
        .distinctBy(VeterinaryWebSource::url)
        .take(maxResults.coerceIn(1, MAX_RESULTS_LIMIT))
}

private fun toMsdVeterinaryWebSource(result: JsonObject): VeterinaryWebSource? {
    val relativePath = trustedMsdRelativePath(result)
    val title =
        result
            .firstString("titlecomputed_t", "title_t")
            ?.cleanReferenceText()
            ?.take(MAX_TITLE_CHARS)
            ?.takeIf(String::isNotBlank)
    val excerpt =
        result
            .firstString("summarycomputed_t", "summary_t", "descriptioncomputed_t", "description_t")
            ?.cleanReferenceText()
            ?.take(MAX_EXCERPT_CHARS)
            ?.takeIf(String::isNotBlank)
    return if (relativePath == null || title == null || excerpt == null) {
        null
    } else {
        VeterinaryWebSource(
            sourceId = "msd:${relativePath.removePrefix("/")}",
            title = title,
            publisher = "MSD Veterinary Manual",
            url = "$MSD_BASE_URL$relativePath",
            excerpt = excerpt,
            publishedYear = result.stringValue("updateddatecomputed_tdt")?.yearOnly(),
        )
    }
}

private fun trustedMsdRelativePath(result: JsonObject): String? {
    val rawPath = result.stringValue("relativeurlcomputed_s")?.trim() ?: return null
    return rawPath
        .takeIf { it.startsWith("/") && !it.startsWith("//") }
        ?.substringBefore('?')
        ?.substringBefore('#')
        ?.takeIf { it.length > MIN_PATH_LENGTH }
}

private fun JsonObject.firstString(vararg keys: String): String? = keys.firstNotNullOfOrNull(::stringValue)

private fun JsonObject.stringValue(key: String): String? = this[key]?.jsonPrimitive?.contentOrNull

private fun String.yearOnly(): String? = take(YEAR_LENGTH).takeIf { it.length == YEAR_LENGTH && it.all(Char::isDigit) }

private const val MSD_BASE_URL = "https://www.msdvetmanual.com"
private const val MAX_RESULTS_LIMIT = 5
private const val MAX_TITLE_CHARS = 240
private const val MAX_EXCERPT_CHARS = 1200
private const val MIN_PATH_LENGTH = 1
private const val YEAR_LENGTH = 4
