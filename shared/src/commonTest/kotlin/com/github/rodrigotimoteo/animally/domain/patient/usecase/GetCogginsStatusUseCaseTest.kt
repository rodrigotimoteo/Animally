package com.github.rodrigotimoteo.animally.domain.patient.usecase

import com.github.rodrigotimoteo.animally.domain.patient.IPatientRepository
import com.github.rodrigotimoteo.animally.domain.patient.model.Patient
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.mock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class GetCogginsStatusUseCaseTest {
    private val patientRepositoryMock: IPatientRepository = mock()

    private lateinit var sut: GetCogginsStatusUseCase

    private val today = LocalDate(2025, 1, 15)

    @BeforeTest
    fun setup() {
        sut = GetCogginsStatusUseCase(patientRepositoryMock)
    }

    private fun newPatient(
        id: Long,
        cogginsExpiryDate: LocalDate?,
    ): Patient =
        Patient(
            id = id,
            name = "Horse $id",
            cogginsExpiryDate = cogginsExpiryDate,
            createdAt = Instant.fromEpochMilliseconds(0L),
            updatedAt = Instant.fromEpochMilliseconds(0L),
        )

    @Test
    fun `expiry within lead days is expiring soon`() {
        val patient = newPatient(1L, today.plus(DatePeriod(days = 5)))
        every { patientRepositoryMock.getPatientList() } returns listOf(patient)

        val result = sut(today = today)

        assertEquals(1, result.size)
        assertEquals(CogginsStatus.EXPIRING_SOON, result.single().status)
        assertEquals(patient, result.single().patient)
    }

    @Test
    fun `expiry today is expiring soon`() {
        val patient = newPatient(1L, today)
        every { patientRepositoryMock.getPatientList() } returns listOf(patient)

        val result = sut(today = today)

        assertEquals(CogginsStatus.EXPIRING_SOON, result.single().status)
    }

    @Test
    fun `expiry exactly on the lead boundary is expiring soon`() {
        val patient = newPatient(1L, today.plus(DatePeriod(days = 30)))
        every { patientRepositoryMock.getPatientList() } returns listOf(patient)

        val result = sut(today = today)

        assertEquals(CogginsStatus.EXPIRING_SOON, result.single().status)
    }

    @Test
    fun `expiry in the past is overdue`() {
        val patient = newPatient(1L, today.plus(DatePeriod(days = -1)))
        every { patientRepositoryMock.getPatientList() } returns listOf(patient)

        val result = sut(today = today)

        assertEquals(CogginsStatus.OVERDUE, result.single().status)
    }

    @Test
    fun `expiry far in the future is excluded`() {
        val patient = newPatient(1L, today.plus(DatePeriod(days = 31)))
        every { patientRepositoryMock.getPatientList() } returns listOf(patient)

        val result = sut(today = today)

        assertEquals(emptyList(), result)
    }

    @Test
    fun `patient without coggins expiry is excluded`() {
        val patient = newPatient(1L, null)
        every { patientRepositoryMock.getPatientList() } returns listOf(patient)

        val result = sut(today = today)

        assertEquals(emptyList(), result)
    }

    @Test
    fun `mixed patients only return overdue and expiring soon`() {
        val withoutExpiry = newPatient(1L, null)
        val overdue = newPatient(2L, today.plus(DatePeriod(days = -3)))
        val expiringSoon = newPatient(3L, today.plus(DatePeriod(days = 10)))
        val valid = newPatient(4L, today.plus(DatePeriod(days = 60)))
        every { patientRepositoryMock.getPatientList() } returns
            listOf(valid, withoutExpiry, expiringSoon, overdue)

        val result = sut(today = today)

        assertEquals(listOf(overdue.id, expiringSoon.id), result.map { it.patient.id })
    }

    @Test
    fun `results are sorted overdue first then by soonest expiry`() {
        val expiringLater = newPatient(1L, today.plus(DatePeriod(days = 20)))
        val overdueOlder = newPatient(2L, today.plus(DatePeriod(days = -10)))
        val expiringSoon = newPatient(3L, today.plus(DatePeriod(days = 5)))
        val overdueRecent = newPatient(4L, today.plus(DatePeriod(days = -1)))
        every { patientRepositoryMock.getPatientList() } returns
            listOf(expiringSoon, overdueOlder, expiringLater, overdueRecent)

        val result = sut(today = today)

        assertEquals(
            listOf(overdueOlder.id, overdueRecent.id, expiringSoon.id, expiringLater.id),
            result.map { it.patient.id },
        )
        assertEquals(CogginsStatus.OVERDUE, result[0].status)
        assertEquals(CogginsStatus.OVERDUE, result[1].status)
        assertEquals(CogginsStatus.EXPIRING_SOON, result[2].status)
        assertEquals(CogginsStatus.EXPIRING_SOON, result[3].status)
    }

    @Test
    fun `custom lead days widens the expiring soon window`() {
        val patient = newPatient(1L, today.plus(DatePeriod(days = 45)))
        every { patientRepositoryMock.getPatientList() } returns listOf(patient)

        val result = sut(leadDays = 60, today = today)

        assertEquals(CogginsStatus.EXPIRING_SOON, result.single().status)
    }

    @Test
    fun `custom lead days excludes far future expiry`() {
        val patient = newPatient(1L, today.plus(DatePeriod(days = 61)))
        every { patientRepositoryMock.getPatientList() } returns listOf(patient)

        val result = sut(leadDays = 60, today = today)

        assertEquals(emptyList(), result)
    }
}
