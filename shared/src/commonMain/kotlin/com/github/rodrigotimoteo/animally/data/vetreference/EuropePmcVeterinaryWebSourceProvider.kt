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
 * Public, read-only veterinary reference search backed by Europe PMC.
 *
 * Europe PMC exposes a documented JSON search API without requiring an app
 * credential. Only normalized veterinary topic terms are sent; patient names,
 * owner details, record text, and identifiers are rejected before the request.
 */
class EuropePmcVeterinaryWebSourceProvider(
    private val httpClient: HttpClient,
    private val maxResults: Int = DEFAULT_MAX_RESULTS,
) : VeterinaryWebSourceProvider {
    override suspend fun search(query: String): VeterinaryWebSearchResult {
        val safeQuery = VeterinaryWebQuery.extractTopic(query) ?: return VeterinaryWebSearchResult.Success(emptyList())
        val urlBuilder = URLBuilder(SEARCH_ENDPOINT)
        urlBuilder.parameters.append("query", "($safeQuery) AND (horse OR equine OR veterinary)")
        urlBuilder.parameters.append("format", "json")
        urlBuilder.parameters.append("resultType", "core")
        urlBuilder.parameters.append("pageSize", maxResults.coerceIn(1, MAX_RESULTS_LIMIT).toString())
        urlBuilder.parameters.append("sort", "CITED desc")
        val url = urlBuilder.build()
        return try {
            withTimeoutOrNull(REQUEST_TIMEOUT_SECONDS.seconds) {
                val response =
                    httpClient.get(url) {
                        accept(ContentType.Application.Json)
                    }
                if (!response.status.isSuccess()) return@withTimeoutOrNull VeterinaryWebSearchResult.Unavailable
                VeterinaryWebSearchResult.Success(parseEuropePmcSources(response.bodyAsText(), maxResults))
            } ?: VeterinaryWebSearchResult.Unavailable
        } catch (ce: CancellationException) {
            throw ce
        } catch (_: Throwable) {
            VeterinaryWebSearchResult.Unavailable
        }
    }

    private companion object {
        const val SEARCH_ENDPOINT = "https://www.ebi.ac.uk/europepmc/webservices/rest/search"
        const val DEFAULT_MAX_RESULTS = 3
        const val REQUEST_TIMEOUT_SECONDS = 12L
    }
}

/** Parses only the Europe PMC fields used by the app; unknown response fields are ignored. */
internal fun parseEuropePmcSources(
    jsonText: String,
    maxResults: Int = 3,
): List<VeterinaryWebSource> {
    val json = Json { ignoreUnknownKeys = true }
    val document = runCatching { json.parseToJsonElement(jsonText) }.getOrNull() ?: return emptyList()
    val resultList = document.jsonObject["resultList"]?.jsonObject
    val results = resultList?.get("result")?.jsonArray.orEmpty()

    return results
        .mapNotNull { it as? JsonObject }
        .mapNotNull(::toVeterinaryWebSource)
        .distinctBy(VeterinaryWebSource::url)
        .take(maxResults.coerceIn(1, MAX_RESULTS_LIMIT))
}

private fun toVeterinaryWebSource(result: JsonObject): VeterinaryWebSource? {
    val title =
        result
            .stringValue("title")
            ?.cleanReferenceText()
            ?.takeIf(String::isNotBlank)
    val excerpt =
        result
            .stringValue("abstractText")
            ?.cleanReferenceText()
            ?.take(MAX_EXCERPT_CHARS)
            ?.takeIf(String::isNotBlank)
    val pmid = result.stringValue("pmid")?.takeIf(String::isNotBlank)
    val id = result.stringValue("id")?.takeIf(String::isNotBlank)
    val source = result.stringValue("source")?.takeIf(String::isNotBlank)
    val sourceId = pmid?.let { "pubmed:$it" } ?: id?.let { "europepmc:${source.orEmpty()}:$it" }
    if (title == null || excerpt == null || sourceId == null) return null
    val url =
        pmid?.let { "https://pubmed.ncbi.nlm.nih.gov/$it/" }
            ?: "https://europepmc.org/article/${source ?: "MED"}/$id"
    return VeterinaryWebSource(
        sourceId = sourceId,
        title = title,
        publisher = "PubMed / Europe PMC",
        url = url,
        excerpt = excerpt,
        publishedYear = result.stringValue("pubYear")?.takeIf(String::isNotBlank),
    )
}

private fun JsonObject.stringValue(key: String): String? = this[key]?.jsonPrimitive?.contentOrNull

/** Removes markup from abstracts before any text is placed in an LLM prompt or UI. */
internal fun String.cleanReferenceText(): String =
    replace(Regex("(?is)<script[^>]*>.*?</script>"), " ")
        .replace(Regex("(?is)<style[^>]*>.*?</style>"), " ")
        .replace(Regex("<[^>]+>"), " ")
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace(Regex("\\s+"), " ")
        .replace(Regex("\\s+([.,!?;:)\\]])"), "$1")
        .trim()

private const val MAX_EXCERPT_CHARS = 1200
private const val MAX_RESULTS_LIMIT = 5
