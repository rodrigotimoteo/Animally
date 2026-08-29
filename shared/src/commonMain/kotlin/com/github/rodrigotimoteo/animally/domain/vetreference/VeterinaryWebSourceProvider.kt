package com.github.rodrigotimoteo.animally.domain.vetreference

import com.github.rodrigotimoteo.animally.domain.vetreference.model.VeterinaryWebSource

/** Result of a public-reference lookup, kept distinct from a valid empty result. */
sealed interface VeterinaryWebSearchResult {
    /** The provider was reached; [sources] contains the best verified matches. */
    data class Success(
        val sources: List<VeterinaryWebSource>,
    ) : VeterinaryWebSearchResult

    /** The provider could not be reached or returned an unusable response. */
    data object Unavailable : VeterinaryWebSearchResult
}

/** Shared boundary for optional, read-only veterinary reference search. */
interface VeterinaryWebSourceProvider {
    /**
     * Searches using a privacy-filtered topic. Implementations must not send
     * patient names, owner details, record text, or identifiers downstream.
     */
    suspend fun search(query: String): VeterinaryWebSearchResult
}
