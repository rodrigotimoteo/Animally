package com.github.rodrigotimoteo.animally.presentation.assistant

import com.github.rodrigotimoteo.animally.llm.EnAssistantStrings
import com.github.rodrigotimoteo.animally.llm.PtAssistantStrings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FollowUpSuggestionsTest {
    @Test
    fun `given vaccination citation when derived then booster suggestion`() {
        assertEquals(
            listOf(EnAssistantStrings.followUpNextBooster),
            FollowUpSuggestions.forCitations(listOf("VACCINATION")),
        )
    }

    @Test
    fun `given gestation and weight citations when derived then both suggestions in order`() {
        val suggestions = FollowUpSuggestions.forCitations(listOf("GESTATION", "WEIGHT"))

        assertEquals(
            listOf(EnAssistantStrings.followUpGestationDay, EnAssistantStrings.followUpWeightTrend),
            suggestions,
        )
    }

    @Test
    fun `given duplicate citations when derived then deduplicated`() {
        assertEquals(
            listOf(EnAssistantStrings.followUpNextBooster),
            FollowUpSuggestions.forCitations(listOf("VACCINATION", "VACCINATION")),
        )
    }

    @Test
    fun `given more than three mapped citations when derived then capped at three`() {
        val suggestions =
            FollowUpSuggestions.forCitations(
                listOf("VACCINATION", "GESTATION", "WEIGHT", "FARRIER_VISIT"),
            )

        assertEquals(3, suggestions.size)
        assertEquals(EnAssistantStrings.followUpNextBooster, suggestions.first())
    }

    @Test
    fun `given nothing cited when derived then default exploration set`() {
        val suggestions = FollowUpSuggestions.forCitations(emptyList())

        assertEquals(
            listOf(
                EnAssistantStrings.followUpDefaultPatients,
                EnAssistantStrings.followUpDefaultTreatments,
                EnAssistantStrings.followUpDefaultDates,
            ),
            suggestions,
        )
    }

    @Test
    fun `given unmapped citations only when derived then default exploration set`() {
        val suggestions = FollowUpSuggestions.forCitations(listOf("LAB_RESULT", "SURGERY"))

        assertTrue(suggestions.contains(EnAssistantStrings.followUpDefaultPatients))
        assertEquals(3, suggestions.size)
    }

    @Test
    fun `given PT strings when derived then localized suggestions`() {
        assertEquals(
            listOf(PtAssistantStrings.followUpNextBooster),
            FollowUpSuggestions.forCitations(listOf("VACCINATION"), PtAssistantStrings),
        )
    }
}
