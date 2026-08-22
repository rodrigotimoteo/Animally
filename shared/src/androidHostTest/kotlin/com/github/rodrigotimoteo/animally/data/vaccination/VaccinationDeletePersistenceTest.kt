package com.github.rodrigotimoteo.animally.data.vaccination

import com.github.rodrigotimoteo.animally.di.database.createTestDatabase
import com.github.rodrigotimoteo.animally.domain.vaccination.model.Vaccination
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Clock

/**
 * Host-JVM tests for the vaccination soft-delete persistence chain:
 * insert -> setInactive -> getByPatient must exclude the deleted row.
 */
class VaccinationDeletePersistenceTest {
    private fun repository() = VaccinationRepositoryImpl(createTestDatabase())

    private fun vaccination(patientId: Long = 1L): Vaccination =
        Vaccination(
            id = 0L,
            patientId = patientId,
            vaccineName = "Tetanus",
            dateAdministered = LocalDate.parse("2026-08-01"),
            vetName = null,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now(),
        )

    @Test
    fun `given inserted vaccination when setInactive then getByPatient excludes it`() {
        val repo = repository()
        val savedId = repo.insert(vaccination())

        assertEquals(1, repo.getByPatient(1L).size)

        repo.setInactive(savedId, Clock.System.now())

        assertTrue(repo.getByPatient(1L).isEmpty(), "soft-deleted row must not reappear")
    }

    @Test
    fun `given soft-deleted row when queried by id then row is not returned`() {
        val repo = repository()
        val savedId = repo.insert(vaccination())

        repo.setInactive(savedId, Clock.System.now())

        // selectById is active-only: soft-deleted rows are invisible everywhere.
        assertEquals(null, repo.getById(savedId))
    }
}
