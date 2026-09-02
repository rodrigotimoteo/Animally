package com.github.rodrigotimoteo.animally.domain.vetreference.model

import io.ktor.http.URLBuilder
import io.ktor.http.URLProtocol
import kotlinx.serialization.Serializable

/**
 * A public veterinary reference fetched for a general educational answer.
 *
 * This type is deliberately separate from [com.github.rodrigotimoteo.animally.domain.search.model.SearchResult]:
 * local patient records and public web references have different privacy and
 * navigation rules.
 */
@Serializable
data class VeterinaryWebSource(
    /** Stable source identifier, such as `pubmed:12345678`. */
    val sourceId: String,
    val title: String,
    val publisher: String,
    val url: String,
    val excerpt: String,
    val publishedYear: String? = null,
) {
    /** True only for source-card URLs emitted by the trusted veterinary providers. */
    internal fun hasTrustedUrl(): Boolean {
        if (url.isBlank() || url.any(Char::isWhitespace)) return false
        val parsed = runCatching { URLBuilder(url).build() }.getOrNull() ?: return false
        return parsed.protocol == URLProtocol.HTTPS &&
            parsed.host.lowercase() in TRUSTED_VETERINARY_HOSTS &&
            parsed.port == URLProtocol.HTTPS.defaultPort &&
            parsed.user == null &&
            parsed.password == null
    }
}

// Keep this list aligned with the absolute URLs produced by the MSD, PubMed,
// and Europe PMC source providers.
private val TRUSTED_VETERINARY_HOSTS =
    setOf(
        "www.msdvetmanual.com",
        "pubmed.ncbi.nlm.nih.gov",
        "europepmc.org",
    )
