package com.github.rodrigotimoteo.animally.data.anamnese

import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.di.database.createTestDatabase
import com.github.rodrigotimoteo.animally.domain.anamnese.model.Anamnese
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Instant

class AnamneseRepositoryImplTest {
    private lateinit var database: AnimallyDatabase
    private lateinit var sut: AnamneseRepositoryImpl

    @BeforeTest
    fun setup() {
        database = createTestDatabase()
        sut = AnamneseRepositoryImpl(database)
    }

    private fun newAnamnese(
        patientId: Long,
        generalHistory: String = "history",
        chronicConditions: String = "conditions",
        allergies: String = "allergies",
        updatedAt: Instant = Instant.fromEpochMilliseconds(0L),
    ): Anamnese =
        Anamnese(
            id = 0L,
            patientId = patientId,
            generalHistory = generalHistory,
            chronicConditions = chronicConditions,
            allergies = allergies,
            createdAt = Instant.fromEpochMilliseconds(0L),
            updatedAt = updatedAt,
        )

    @Test
    fun `when patient has no anamnese then returns null`() {
        assertNull(sut.getByPatient(999L))
    }

    @Test
    fun `when saving then stores and retrieves all fields`() {
        val id = sut.save(newAnamnese(patientId = 1L))

        val result = sut.getByPatient(1L)
        assertNotNull(result)
        assertEquals(id, assertNotNull(result).id)
        assertEquals("history", result.generalHistory)
        assertEquals("conditions", result.chronicConditions)
        assertEquals("allergies", result.allergies)
    }

    @Test
    fun `when saving twice for same patient then upserts to one row`() {
        val firstId = sut.save(newAnamnese(patientId = 1L))

        val secondId =
            sut.save(
                newAnamnese(
                    patientId = 1L,
                    generalHistory = "updated history",
                    allergies = "penicillin",
                    updatedAt = Instant.fromEpochMilliseconds(100L),
                ),
            )

        assertEquals(firstId, secondId)
        val result = sut.getByPatient(1L)
        assertNotNull(result)
        assertEquals("updated history", assertNotNull(result).generalHistory)
        assertEquals("penicillin", result.allergies)
    }

    @Test
    fun `when saving for different patients then creates separate rows`() {
        val firstId = sut.save(newAnamnese(patientId = 1L))
        val secondId = sut.save(newAnamnese(patientId = 2L))

        assertNotNull(sut.getByPatient(1L))
        assertNotNull(sut.getByPatient(2L))
        assertEquals(firstId, assertNotNull(sut.getByPatient(1L)).id)
        assertEquals(secondId, assertNotNull(sut.getByPatient(2L)).id)
    }
}
