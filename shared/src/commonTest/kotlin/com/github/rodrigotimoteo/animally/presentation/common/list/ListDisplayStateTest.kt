package com.github.rodrigotimoteo.animally.presentation.common.list

import kotlin.test.Test
import kotlin.test.assertEquals

class ListDisplayStateTest {
    private val records = (1..7).map { "Record $it" }

    @Test
    fun `blank search stays collapsed until expanded`() {
        val displayState = ListDisplayState()

        assertEquals(records.take(COLLAPSED_LIST_LIMIT), records.visibleForListDisplay(displayState))
        assertEquals(records, records.visibleForListDisplay(displayState.copy(isExpanded = true)))
    }

    @Test
    fun `active search reveals every matching record`() {
        val displayState = ListDisplayState(searchQuery = "record")

        assertEquals(records, records.visibleForListDisplay(displayState))
    }

    @Test
    fun `search terms match independently and ignore case`() {
        val filtered =
            records.filterBySearch("RECORD 7") { record ->
                record
            }

        assertEquals(listOf("Record 7"), filtered)
    }

    @Test
    fun `null and blank queries do not filter records`() {
        assertEquals(records, records.filterBySearch(null) { it })
        assertEquals(records, records.filterBySearch("   ") { it })
    }
}
