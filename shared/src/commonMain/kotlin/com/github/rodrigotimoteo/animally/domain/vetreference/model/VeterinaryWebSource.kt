package com.github.rodrigotimoteo.animally.domain.vetreference.model

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
)
