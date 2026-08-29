package com.github.rodrigotimoteo.animally.presentation.assistant

import com.github.rodrigotimoteo.animally.domain.search.model.SearchResult
import kotlin.test.Test
import kotlin.test.assertEquals

class AssistantSourceGroupTest {
    @Test
    fun `cited records are grouped once per patient and patient source is preferred`() {
        val sources =
            listOf(
                source(patientId = 7, name = "Lua do Pinhal", type = "WEIGHT", id = 11),
                source(patientId = 7, name = "Lua do Pinhal", type = "PATIENT", id = 7),
                source(patientId = 9, name = "Orion do Vale", type = "LAB_RESULT", id = 20),
            )

        val groups = sourceGroupsForDisplay(sources)

        assertEquals(2, groups.size)
        assertEquals(listOf(7L, 9L), groups.map(AssistantSourceGroup::patientId))
        assertEquals("PATIENT", groups.first().primarySource.recordType)
        assertEquals(2, groups.first().recordCount)
        assertEquals("Orion do Vale", groups.last().patientName)
    }

    @Test
    fun `empty sources produce no groups`() {
        assertEquals(emptyList(), sourceGroupsForDisplay(emptyList()))
    }

    @Test
    fun `owner and patient with the same numeric id stay separate`() {
        val sources =
            listOf(
                source(patientId = 7, name = "Lua do Pinhal", type = "PATIENT", id = 7),
                source(patientId = 7, name = "Inês Martins", type = "OWNER", id = 7),
            )

        val groups = sourceGroupsForDisplay(sources)

        assertEquals(2, groups.size)
        assertEquals(listOf("Lua do Pinhal", "Inês Martins"), groups.map(AssistantSourceGroup::patientName))
    }

    private fun source(
        patientId: Long,
        name: String,
        type: String,
        id: Long,
    ): SearchResult =
        SearchResult(
            patientId = patientId,
            patientName = name,
            breed = null,
            microchipId = null,
            recordType = type,
            recordId = id,
            date = null,
            snippet = "",
        )
}
