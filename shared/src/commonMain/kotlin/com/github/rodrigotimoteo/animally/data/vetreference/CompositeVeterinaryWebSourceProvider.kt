package com.github.rodrigotimoteo.animally.data.vetreference

import com.github.rodrigotimoteo.animally.domain.vetreference.VeterinaryWebSearchResult
import com.github.rodrigotimoteo.animally.domain.vetreference.VeterinaryWebSourceProvider
import com.github.rodrigotimoteo.animally.domain.vetreference.model.VeterinaryWebSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/**
 * Queries independent public-reference providers together and keeps a partial
 * result useful when one service is unavailable. Providers run concurrently
 * so adding a source does not add its timeout to the normal response latency.
 */
class CompositeVeterinaryWebSourceProvider(
    private val providers: List<VeterinaryWebSourceProvider>,
    private val maxResults: Int = DEFAULT_MAX_RESULTS,
) : VeterinaryWebSourceProvider {
    override suspend fun search(query: String): VeterinaryWebSearchResult =
        coroutineScope {
            val results = providers.map { provider -> async { searchSafely(provider, query) } }.awaitAll()
            val successfulResults = results.filterIsInstance<VeterinaryWebSearchResult.Success>()
            if (successfulResults.isEmpty()) {
                VeterinaryWebSearchResult.Unavailable
            } else {
                VeterinaryWebSearchResult.Success(
                    successfulResults
                        .flatMap(VeterinaryWebSearchResult.Success::sources)
                        .distinctBy(VeterinaryWebSource::url)
                        .take(maxResults.coerceIn(1, MAX_RESULTS_LIMIT)),
                )
            }
        }

    private suspend fun searchSafely(
        provider: VeterinaryWebSourceProvider,
        query: String,
    ): VeterinaryWebSearchResult =
        try {
            provider.search(query)
        } catch (ce: CancellationException) {
            throw ce
        } catch (_: Throwable) {
            VeterinaryWebSearchResult.Unavailable
        }

    private companion object {
        const val DEFAULT_MAX_RESULTS = 3
        const val MAX_RESULTS_LIMIT = 5
    }
}
