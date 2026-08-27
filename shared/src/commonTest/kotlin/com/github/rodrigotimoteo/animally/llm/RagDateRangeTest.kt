package com.github.rodrigotimoteo.animally.llm

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RagDateRangeTest {
    private val today = LocalDate(2026, 8, 24)

    @Test
    fun `current month ends today rather than including future rows`() {
        assertEquals(
            RagDateRange(LocalDate(2026, 8, 1), today),
            RagDateRangeIntent.resolve("What happened this month?", today),
        )
    }

    @Test
    fun `previous month is the complete preceding calendar month`() {
        assertEquals(
            RagDateRange(LocalDate(2026, 7, 1), LocalDate(2026, 7, 31)),
            RagDateRangeIntent.resolve("What happened last month?", today),
        )
    }

    @Test
    fun `portuguese current week is recognized`() {
        assertEquals(
            RagDateRange(LocalDate(2026, 8, 24), today),
            RagDateRangeIntent.resolve("O que aconteceu nesta semana?", today),
        )
    }

    @Test
    fun `portuguese current month is recognized`() {
        assertEquals(
            RagDateRange(LocalDate(2026, 8, 1), today),
            RagDateRangeIntent.resolve("O que aconteceu este mês?", today),
        )
    }

    @Test
    fun `unrelated question has no date range`() {
        assertNull(RagDateRangeIntent.resolve("Why is the sky blue?", today))
    }
}
