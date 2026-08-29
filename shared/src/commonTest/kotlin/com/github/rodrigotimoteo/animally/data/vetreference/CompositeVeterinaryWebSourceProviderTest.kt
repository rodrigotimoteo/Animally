package com.github.rodrigotimoteo.animally.data.vetreference

import com.github.rodrigotimoteo.animally.domain.vetreference.VeterinaryWebSearchResult
import com.github.rodrigotimoteo.animally.domain.vetreference.VeterinaryWebSourceProvider
import com.github.rodrigotimoteo.animally.domain.vetreference.model.VeterinaryWebSource
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

private class StubVeterinaryWebSourceProvider(
    private val result: VeterinaryWebSearchResult,
) : VeterinaryWebSourceProvider {
    override suspend fun search(query: String): VeterinaryWebSearchResult = result
}

class CompositeVeterinaryWebSourceProviderTest {
    private val europePmcSource =
        VeterinaryWebSource(
            sourceId = "pubmed:1",
            title = "A Europe PMC result",
            publisher = "PubMed / Europe PMC",
            url = "https://pubmed.ncbi.nlm.nih.gov/1/",
            excerpt = "A useful abstract.",
        )
    private val msdSource =
        VeterinaryWebSource(
            sourceId = "msd:laminitis-in-horses",
            title = "Laminitis in Horses",
            publisher = "MSD Veterinary Manual",
            url = "https://www.msdvetmanual.com/laminitis-in-horses",
            excerpt = "A reviewed veterinary summary.",
        )

    @Test
    fun `keeps available sources when another provider is unavailable and deduplicates URLs`() =
        runTest {
            val provider =
                CompositeVeterinaryWebSourceProvider(
                    providers =
                        listOf(
                            StubVeterinaryWebSourceProvider(
                                VeterinaryWebSearchResult.Success(listOf(msdSource, msdSource)),
                            ),
                            StubVeterinaryWebSourceProvider(VeterinaryWebSearchResult.Unavailable),
                        ),
                )

            assertEquals(
                VeterinaryWebSearchResult.Success(listOf(msdSource)),
                provider.search("What is laminitis in horses?"),
            )
        }

    @Test
    fun `returns unavailable only when every provider is unavailable`() =
        runTest {
            val provider =
                CompositeVeterinaryWebSourceProvider(
                    providers =
                        listOf(
                            StubVeterinaryWebSourceProvider(VeterinaryWebSearchResult.Unavailable),
                            StubVeterinaryWebSourceProvider(VeterinaryWebSearchResult.Unavailable),
                        ),
                )

            assertEquals(VeterinaryWebSearchResult.Unavailable, provider.search("What is laminitis?"))
        }

    @Test
    fun `preserves provider order so MSD is preferred for UI cards`() =
        runTest {
            val provider =
                CompositeVeterinaryWebSourceProvider(
                    providers =
                        listOf(
                            StubVeterinaryWebSourceProvider(VeterinaryWebSearchResult.Success(listOf(msdSource))),
                            StubVeterinaryWebSourceProvider(VeterinaryWebSearchResult.Success(listOf(europePmcSource))),
                        ),
                )

            val result = provider.search("What is laminitis?") as VeterinaryWebSearchResult.Success
            assertEquals(listOf(msdSource, europePmcSource), result.sources)
        }
}
