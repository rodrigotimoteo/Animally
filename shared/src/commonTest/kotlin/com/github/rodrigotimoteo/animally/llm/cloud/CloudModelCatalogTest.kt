package com.github.rodrigotimoteo.animally.llm.cloud

import kotlinx.serialization.SerializationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CloudModelsListParserTest {
    @Test
    fun `parses openai models list payload`() {
        val payload =
            """
            {
              "object": "list",
              "data": [
                {"id": "gpt-4o-mini", "object": "model", "owned_by": "openai"},
                {"id": "gpt-4o", "object": "model", "owned_by": "openai"}
              ]
            }
            """.trimIndent()

        assertEquals(listOf("gpt-4o", "gpt-4o-mini"), parseCloudModelsList(payload))
    }

    @Test
    fun `sorts and deduplicates ids`() {
        val payload =
            """{"data":[{"id":"b"},{"id":"a"},{"id":"b"},{"id":""}]}"""

        assertEquals(listOf("a", "b"), parseCloudModelsList(payload))
    }

    @Test
    fun `missing data array yields empty list`() {
        assertEquals(emptyList(), parseCloudModelsList("""{"object":"list"}"""))
    }

    @Test
    fun `empty data array yields empty list`() {
        assertEquals(emptyList(), parseCloudModelsList("""{"data":[]}"""))
    }

    @Test
    fun `unknown keys are ignored`() {
        val payload =
            """{"data":[{"id":"m1","created":1700000000,"context_length":8192,"extra":{"x":1}}]}"""

        assertEquals(listOf("m1"), parseCloudModelsList(payload))
    }

    @Test
    fun `malformed json throws serialization exception`() {
        assertFailsWith<SerializationException> { parseCloudModelsList("not-json") }
    }

    @Test
    fun `non-list data field throws`() {
        assertFailsWith<SerializationException> { parseCloudModelsList("""{"data":"oops"}""") }
    }
}

class CloudLlmProviderPresetTest {
    @Test
    fun `preset base urls match provider endpoints`() {
        assertEquals("https://api.openai.com/v1", CloudLlmProviderPreset.OPENAI.baseUrl)
        assertEquals("https://openrouter.ai/api/v1", CloudLlmProviderPreset.OPENROUTER.baseUrl)
        assertEquals("https://api.groq.com/openai/v1", CloudLlmProviderPreset.GROQ.baseUrl)
        assertEquals("https://api.together.xyz/v1", CloudLlmProviderPreset.TOGETHER.baseUrl)
        assertEquals("https://opencode.ai/zen/v1", CloudLlmProviderPreset.ZEN.baseUrl)
        assertEquals("https://opencode.ai/zen/go/v1", CloudLlmProviderPreset.OPENCODE_GO.baseUrl)
        assertEquals("http://localhost:11434/v1", CloudLlmProviderPreset.OLLAMA.baseUrl)
        assertEquals("http://localhost:1234/v1", CloudLlmProviderPreset.LM_STUDIO.baseUrl)
    }

    @Test
    fun `local presets need no api key`() {
        assertTrue(CloudLlmProviderPreset.OPENAI.requiresApiKey)
        assertTrue(CloudLlmProviderPreset.OPENROUTER.requiresApiKey)
        assertTrue(CloudLlmProviderPreset.GROQ.requiresApiKey)
        assertTrue(CloudLlmProviderPreset.TOGETHER.requiresApiKey)
        assertTrue(CloudLlmProviderPreset.ZEN.requiresApiKey)
        assertTrue(CloudLlmProviderPreset.OPENCODE_GO.requiresApiKey)
        assertTrue(!CloudLlmProviderPreset.OLLAMA.requiresApiKey)
        assertTrue(!CloudLlmProviderPreset.LM_STUDIO.requiresApiKey)
    }

    @Test
    fun `local presets hidden on mobile others visible`() {
        assertTrue(!CloudLlmProviderPreset.OLLAMA.visibleOnMobile)
        assertTrue(!CloudLlmProviderPreset.LM_STUDIO.visibleOnMobile)
        CloudLlmProviderPreset.entries
            .filter { it != CloudLlmProviderPreset.OLLAMA && it != CloudLlmProviderPreset.LM_STUDIO }
            .forEach { preset -> assertTrue(preset.visibleOnMobile, preset.name) }
    }

    @Test
    fun `fromId resolves known ids`() {
        assertEquals(CloudLlmProviderPreset.GROQ, CloudLlmProviderPreset.fromId("groq"))
        assertEquals(CloudLlmProviderPreset.LM_STUDIO, CloudLlmProviderPreset.fromId("lm_studio"))
        assertEquals(CloudLlmProviderPreset.ZEN, CloudLlmProviderPreset.fromId("zen"))
        assertEquals(CloudLlmProviderPreset.OPENCODE_GO, CloudLlmProviderPreset.fromId("opencode_go"))
    }

    @Test
    fun `fromId falls back to custom for unknown or blank ids`() {
        assertEquals(CloudLlmProviderPreset.CUSTOM, CloudLlmProviderPreset.fromId("nope"))
        assertEquals(CloudLlmProviderPreset.CUSTOM, CloudLlmProviderPreset.fromId(null))
        assertEquals(CloudLlmProviderPreset.CUSTOM, CloudLlmProviderPreset.fromId(""))
    }

    @Test
    fun `fromBaseUrl maps stored urls back to presets ignoring trailing slash`() {
        assertEquals(
            CloudLlmProviderPreset.OPENROUTER,
            CloudLlmProviderPreset.fromBaseUrl("https://openrouter.ai/api/v1/"),
        )
        assertEquals(
            CloudLlmProviderPreset.OLLAMA,
            CloudLlmProviderPreset.fromBaseUrl("http://localhost:11434/v1"),
        )
        assertEquals(
            CloudLlmProviderPreset.ZEN,
            CloudLlmProviderPreset.fromBaseUrl("https://opencode.ai/zen/v1"),
        )
        assertEquals(
            CloudLlmProviderPreset.ZEN,
            CloudLlmProviderPreset.fromBaseUrl("https://opencode.ai/zen/v1/"),
        )
        assertEquals(
            CloudLlmProviderPreset.OPENCODE_GO,
            CloudLlmProviderPreset.fromBaseUrl("https://opencode.ai/zen/go/v1"),
        )
        assertEquals(
            CloudLlmProviderPreset.OPENCODE_GO,
            CloudLlmProviderPreset.fromBaseUrl("https://opencode.ai/zen/go/v1/"),
        )
    }

    @Test
    fun `fromBaseUrl returns null for unmatched urls`() {
        assertNull(CloudLlmProviderPreset.fromBaseUrl("https://my-proxy.example.com/v1"))
        assertNull(CloudLlmProviderPreset.fromBaseUrl(null))
        assertNull(CloudLlmProviderPreset.fromBaseUrl("  "))
    }
}
