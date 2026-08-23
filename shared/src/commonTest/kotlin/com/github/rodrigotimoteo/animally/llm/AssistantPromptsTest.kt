package com.github.rodrigotimoteo.animally.llm

import kotlin.test.Test
import kotlin.test.assertEquals

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
}
