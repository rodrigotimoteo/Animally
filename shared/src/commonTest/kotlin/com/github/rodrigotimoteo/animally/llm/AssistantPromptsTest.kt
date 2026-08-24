package com.github.rodrigotimoteo.animally.llm

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AssistantPromptsTest {
    @Test
    fun `given question with leading filler when enriched then filler tokens dropped`() {
        assertEquals(
            "last tetanus vaccination Thunder",
            AssistantPrompts.enrichQuery("What was the last tetanus vaccination for Thunder?"),
        )
    }

    @Test
    fun `given punctuation on filler tokens when enriched then tokens still dropped`() {
        assertEquals(
            "colic surgery outcomes",
            AssistantPrompts.enrichQuery("Did the colic surgery have any outcomes?"),
        )
    }

    @Test
    fun `given all-filler query when enriched then original query returned`() {
        assertEquals("Is the a?", AssistantPrompts.enrichQuery("Is the a?"))
    }

    @Test
    fun `given content words when enriched then query unchanged`() {
        assertEquals("tendon injury grade 2", AssistantPrompts.enrichQuery("tendon injury grade 2"))
    }

    @Test
    fun `given pregnancy pronoun question when enriched then only content word kept`() {
        // Regression lock: "She is pregnant" must reduce to "pregnant" so the
        // gestation vocabulary in the FTS index can match it.
        assertEquals("pregnant", AssistantPrompts.enrichQuery("She is pregnant"))
    }

    @Test
    fun `given conversational lead-in when enriched then only subject kept`() {
        // Regression lock for the patient-question failure: "tell"/"about"/"me"
        // are conversational filler and must not enter the FTS query.
        assertEquals("Thunder", AssistantPrompts.enrichQuery("Tell me about Thunder"))
    }

    @Test
    fun `given natural question when toFtsOrQuery then starred OR expression`() {
        // Apostrophes and multi-word tokens are left as-is here; the
        // repository's sanitizer splits them into prefix terms downstream.
        assertEquals(
            "Thunder's* OR last* OR farrier* OR visit*",
            AssistantPrompts.toFtsOrQuery("When was Thunder's last farrier visit?"),
        )
    }

    @Test
    fun `given all-filler query when toFtsOrQuery then empty string`() {
        assertEquals("", AssistantPrompts.toFtsOrQuery("How is she?"))
    }

    @Test
    fun `given portuguese question when detected then true`() {
        assertTrue(AssistantPrompts.isPortugueseQuery("Quantos pacientes tenho?"))
        assertTrue(AssistantPrompts.isPortugueseQuery("Qual é a gestação da Bella?"))
    }

    @Test
    fun `given english question when detected then false`() {
        assertTrue(!AssistantPrompts.isPortugueseQuery("Tell me about Thunder"))
        assertTrue(!AssistantPrompts.isPortugueseQuery("When was the last farrier visit?"))
    }
}
