package com.github.rodrigotimoteo.animally.data.vetreference

import com.github.rodrigotimoteo.animally.domain.vetreference.VeterinaryWebSearchResult
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MsdVeterinaryWebSourceProviderTest {
    @Test
    fun `parses and deduplicates trusted MSD topic results`() =
        runBlocking {
            val client =
                HttpClient(
                    MockEngine { request ->
                        assertTrue(request.url.toString().contains("q=laminitis"))
                        assertTrue(request.url.toString().contains("model=SearchResult"))
                        respond(
                            content =
                                """
                                {"data":{"response":{"docs":[
                                  {"titlecomputed_t":"<i>Laminitis</i> in Horses","summarycomputed_t":"Laminitis is inflammation of the <b>laminae</b>.","relativeurlcomputed_s":"/musculoskeletal-system/disorders-of-the-foot-in-horses/laminitis-in-horses","updateddatecomputed_tdt":"2024-06-07T00:00:00Z"},
                                  {"titlecomputed_t":"Laminitis in Horses","summarycomputed_t":"Duplicate result","relativeurlcomputed_s":"/musculoskeletal-system/disorders-of-the-foot-in-horses/laminitis-in-horses","updateddatecomputed_tdt":"2024-06-07T00:00:00Z"},
                                  {"titlecomputed_t":"Untrusted result","summarycomputed_t":"Do not admit this result","relativeurlcomputed_s":"https://example.com/not-msd"}
                                ]}}}
                                """.trimIndent(),
                            headers = headersOf("Content-Type", ContentType.Application.Json.toString()),
                        )
                    },
                )
            try {
                val result = MsdVeterinaryWebSourceProvider(client).search("What is laminitis in horses?")
                val source = (result as VeterinaryWebSearchResult.Success).sources.single()
                assertEquals("msd:musculoskeletal-system/disorders-of-the-foot-in-horses/laminitis-in-horses", source.sourceId)
                assertEquals("Laminitis in Horses", source.title)
                assertEquals("Laminitis is inflammation of the laminae.", source.excerpt)
                assertEquals("MSD Veterinary Manual", source.publisher)
                assertEquals("2024", source.publishedYear)
                assertEquals(
                    "https://www.msdvetmanual.com/musculoskeletal-system/disorders-of-the-foot-in-horses/laminitis-in-horses",
                    source.url,
                )
            } finally {
                client.close()
            }
        }

    @Test
    fun `provider returns unavailable for an unsuccessful response`() =
        runBlocking {
            val client =
                HttpClient(
                    MockEngine {
                        respond(
                            content = "service unavailable",
                            status = HttpStatusCode.ServiceUnavailable,
                        )
                    },
                )
            try {
                assertEquals(
                    VeterinaryWebSearchResult.Unavailable,
                    MsdVeterinaryWebSourceProvider(client).search("What is laminitis?"),
                )
            } finally {
                client.close()
            }
        }
}
