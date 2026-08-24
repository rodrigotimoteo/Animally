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
        // Possessive apostrophes are stripped in clean() ("Thunder's" ->
        // "Thunders"): the repository sanitizer would otherwise split the
        // token at the apostrophe into a junk prefix plus a stray "s*" term.
        // "farrier" matches the hoof-care synonym group, so its remaining
        // members are appended as extra OR-terms (deliberate recall gain).
        assertEquals(
            "Thunders* OR last* OR farrier* OR visit* OR shod* OR shoeing* OR shoes* OR trim*",
            AssistantPrompts.toFtsOrQuery("When was Thunder's last farrier visit?"),
        )
    }

    @Test
    fun `given possessive name when enriched then internal apostrophe stripped`() {
        // Regression lock: "Thunder's" must become one clean token so the
        // OR-retry leg matches instead of emitting junk "s*" terms.
        assertEquals(
            "Thunders last farrier visit",
            AssistantPrompts.enrichQuery("What was Thunder's last farrier visit?"),
        )
    }

    @Test
    fun `given curly apostrophe when enriched then stripped too`() {
        assertEquals(
            "Bellas gestation",
            AssistantPrompts.enrichQuery("Bella’s gestation"),
        )
    }

    @Test
    fun `given query without synonym matches when toFtsOrQuery then no expansion appended`() {
        assertEquals(
            "follicle* OR ultrasound*",
            AssistantPrompts.toFtsOrQuery("follicle ultrasound"),
        )
    }

    @Test
    fun `given more than two synonym groups when toFtsOrQuery then only first two expanded`() {
        // "colic" (group 5) and "shod" (group 2) both match; declared order
        // caps expansion at the FIRST two groups: hoof care + vaccination.
        val query = AssistantPrompts.toFtsOrQuery("colic shod vaccination")
        assertTrue(query.contains("shoeing*"), "first matched group must expand")
        assertTrue(query.contains("vaccine*"), "second matched group must expand")
        assertTrue(!query.contains("abdominal"), "third group must be dropped by the cap")
    }

    @Test
    fun `given multi word synonym member when expanded then quoted phrase emitted`() {
        // "in foal" must survive as a starred quoted phrase so the repository
        // sanitizer keeps the exact word sequence instead of starring "in".
        val query = AssistantPrompts.toFtsOrQuery("gestation")
        assertTrue(query.contains("\"in foal\"*"), "multi-word synonyms need phrase quoting: $query")
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
