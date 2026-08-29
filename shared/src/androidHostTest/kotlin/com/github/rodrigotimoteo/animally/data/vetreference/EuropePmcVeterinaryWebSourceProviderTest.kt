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

class EuropePmcVeterinaryWebSourceProviderTest {
    @Test
    fun `parses a cited Europe PMC result and cleans abstract markup`() =
        runBlocking {
            val client =
                HttpClient(
                    MockEngine { request ->
                        assertTrue(request.url.toString().contains("laminitis"))
                        assertTrue(request.url.toString().contains("horse"))
                        respond(
                            content =
                                """
                                {"resultList":{"result":[{"id":"123456","source":"MED","pmid":"123456","title":"<i>Laminitis</i> in horses","abstractText":"A &amp; useful <b>abstract</b>.","pubYear":"2024"}]}}
                                """.trimIndent(),
                            headers = headersOf("Content-Type", ContentType.Application.Json.toString()),
                        )
                    },
                )
            try {
                val result = EuropePmcVeterinaryWebSourceProvider(client).search("What is laminitis in horses?")
                val source = (result as VeterinaryWebSearchResult.Success).sources.single()
                assertEquals("pubmed:123456", source.sourceId)
                assertEquals("Laminitis in horses", source.title)
                assertEquals("A & useful abstract.", source.excerpt)
                assertEquals("https://pubmed.ncbi.nlm.nih.gov/123456/", source.url)
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
                            content = "not found",
                            status = HttpStatusCode.NotFound,
                        )
                    },
                )
            try {
                assertEquals(
                    VeterinaryWebSearchResult.Unavailable,
                    EuropePmcVeterinaryWebSourceProvider(client).search("What is laminitis?"),
                )
            } finally {
                client.close()
            }
        }
}
