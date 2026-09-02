package com.github.rodrigotimoteo.animally.domain.assistant.model

import com.github.rodrigotimoteo.animally.domain.vetreference.model.VeterinaryWebSource
import kotlin.test.Test
import kotlin.test.assertEquals

class AssistantWebSourcesCodecTest {
    @Test
    fun `decode keeps trusted HTTPS provider source URLs`() {
        val sources =
            listOf(
                source("https://www.msdvetmanual.com/laminitis-in-horses"),
                source("https://pubmed.ncbi.nlm.nih.gov/123456/"),
                source("https://europepmc.org/article/MED/123456"),
            )

        val restored = AssistantWebSourcesCodec.decode(AssistantWebSourcesCodec.encode(sources))

        assertEquals(sources, restored)
    }

    @Test
    fun `decode drops non HTTPS untrusted and malformed source URLs individually`() {
        val sources =
            listOf(
                source("http://www.msdvetmanual.com/unsafe"),
                source("https://www.msdvetmanual.com.evil.example/unsafe"),
                source("https://evil.example/unsafe"),
                source("https://pubmed.ncbi.nlm.nih.gov:8443/unsafe"),
                source("https://user:password@europepmc.org/unsafe"),
                source("not a URL"),
                source("https://pubmed.ncbi.nlm.nih.gov/123456/"),
            )

        val restored = AssistantWebSourcesCodec.decode(AssistantWebSourcesCodec.encode(sources))

        assertEquals(listOf("https://pubmed.ncbi.nlm.nih.gov/123456/"), restored.map { it.url })
    }

    private fun source(url: String) =
        VeterinaryWebSource(
            sourceId = url,
            title = "Veterinary reference",
            publisher = "Test provider",
            url = url,
            excerpt = "Reviewed veterinary information.",
        )
}
