package com.github.rodrigotimoteo.animally.domain.search.usecase

import com.github.rodrigotimoteo.animally.domain.search.model.SearchResult
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/**
 * Direct tests for the retrieval policy shared by GenerateRagResponseUseCase
 * and the golden-set harness, so the weak-retry/dedup contract cannot drift
 * between the two callers.
 */
class RetrievalPolicyTest {
    private fun hit(
        type: String = "VACCINATION",
        id: Long,
    ) = SearchResult(
        patientId = 7L,
        patientName = "Thunder",
        breed = null,
        microchipId = null,
        recordType = type,
        recordId = id,
        date = LocalDate(2024, 5, 1),
        snippet = "snippet",
    )

    @Test
    fun `given strong AND leg when merging then OR retry is never queried`() {
        val strong = (1L..RetrievalPolicy.WEAK_RESULT_THRESHOLD).map { id -> hit(id = id) }
        var retryQueried = false

        val merged =
            RetrievalPolicy.mergeWeakRetry(strong) {
                retryQueried = true
                emptyList()
            }

        assertEquals(strong, merged)
        assertFalse(retryQueried, "strong AND leg must short-circuit the lazy OR provider")
    }

    @Test
    fun `given weak AND leg when merging then retry hits append deduplicated by record identity`() {
        val andLeg = listOf(hit(id = 1), hit(type = "GESTATION", id = 1))
        val retryLeg = listOf(hit(id = 1), hit(type = "FARRIER_VISIT", id = 2))

        val merged = RetrievalPolicy.mergeWeakRetry(andLeg) { retryLeg }

        assertEquals(
            listOf("VACCINATION#1", "GESTATION#1", "FARRIER_VISIT#2"),
            merged.map { "${it.recordType}#${it.recordId}" },
            "retry duplicate of an AND-leg record must be dropped; new records appended in order",
        )
    }

    @Test
    fun `given custom threshold when merging then boundary follows the parameter`() {
        val two = (1L..2L).map { id -> hit(id = id) }
        var retryQueried = false

        RetrievalPolicy.mergeWeakRetry(two, threshold = 2) {
            retryQueried = true
            emptyList()
        }

        assertFalse(retryQueried, "exactly-threshold results are strong - no retry")
    }
}
