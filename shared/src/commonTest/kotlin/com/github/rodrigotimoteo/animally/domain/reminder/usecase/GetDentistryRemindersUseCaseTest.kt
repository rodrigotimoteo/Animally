package com.github.rodrigotimoteo.animally.domain.reminder.usecase

import com.github.rodrigotimoteo.animally.domain.dentistry.IDentistryRepository
import com.github.rodrigotimoteo.animally.domain.dentistry.model.Dentistry
import com.github.rodrigotimoteo.animally.domain.patient.IPatientRepository
import com.github.rodrigotimoteo.animally.domain.patient.model.Patient
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.mock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class GetDentistryRemindersUseCaseTest {
    private val dentistryRepositoryMock: IDentistryRepository = mock()

    private val patientRepositoryMock: IPatientRepository = mock()

    private lateinit var sut: GetDentistryRemindersUseCase

    private val today = LocalDate(2025, 1, 15)

    @BeforeTest
    fun setup() {
        sut = GetDentistryRemindersUseCase(dentistryRepositoryMock, patientRepositoryMock)
    }

    private fun newPatient(id: Long): Patient =
        Patient(
            id = id,
            name = "Horse $id",
            createdAt = Instant.fromEpochMilliseconds(0L),
            updatedAt = Instant.fromEpochMilliseconds(0L),
        )

    private fun newDentistry(
        id: Long,
        patientId: Long,
        nextDueDate: LocalDate?,
        isActive: Boolean = true,
    ): Dentistry =
        Dentistry(
            id = id,
            patientId = patientId,
            date = today,
            nextDueDate = nextDueDate,
            isActive = isActive,
            createdAt = Instant.fromEpochMilliseconds(0L),
            updatedAt = Instant.fromEpochMilliseconds(0L),
        )

    private fun stubPatientWithDentistry(
        patientId: Long,
        records: List<Dentistry>,
    ) {
        every { patientRepositoryMock.getPatientList() } returns listOf(newPatient(patientId))
        every { dentistryRepositoryMock.getByPatient(patientId) } returns records
    }

    @Test
    fun `upcoming dentistry record is included with resolved patient name`() {
        val patient = newPatient(1L)
        val record = newDentistry(1L, patient.id, today.plus(DatePeriod(days = 90)))
        stubPatientWithDentistry(patient.id, listOf(record))

        val result = sut(today = today)

        assertEquals(1, result.size)
        assertEquals(patient.id, result.single().patientId)
        assertEquals(patient.name, result.single().patientName)
        assertEquals("Dentistry", result.single().recordType)
        assertEquals("Dental check", result.single().title)
        assertEquals(today.plus(DatePeriod(days = 90)), result.single().dueDate)
    }

    @Test
    fun `overdue dentistry record is included`() {
        val patient = newPatient(1L)
        val record = newDentistry(1L, patient.id, today.minus(DatePeriod(days = 10)))
        stubPatientWithDentistry(patient.id, listOf(record))

        val result = sut(today = today)

        assertEquals(1, result.size)
        assertEquals(today.minus(DatePeriod(days = 10)), result.single().dueDate)
    }

    @Test
    fun `due today is included`() {
        val patient = newPatient(1L)
        val record = newDentistry(1L, patient.id, today)
        stubPatientWithDentistry(patient.id, listOf(record))

        val result = sut(today = today)

        assertEquals(1, result.size)
        assertEquals(today, result.single().dueDate)
    }

    @Test
    fun `results are sorted by due date`() {
        val patient = newPatient(1L)
        val later = newDentistry(1L, patient.id, today.plus(DatePeriod(days = 270)))
        val overdue = newDentistry(2L, patient.id, today.minus(DatePeriod(days = 30)))
        val soon = newDentistry(3L, patient.id, today.plus(DatePeriod(days = 90)))
        stubPatientWithDentistry(patient.id, listOf(later, overdue, soon))

        val result = sut(today = today)

        assertEquals(
            listOf(
                today.minus(DatePeriod(days = 30)),
                today.plus(DatePeriod(days = 90)),
                today.plus(DatePeriod(days = 270)),
            ),
            result.map { it.dueDate },
        )
    }

    @Test
    fun `inactive dentistry record is excluded`() {
        val patient = newPatient(1L)
        val inactive = newDentistry(1L, patient.id, today.plus(DatePeriod(days = 90)), isActive = false)
        stubPatientWithDentistry(patient.id, listOf(inactive))

        val result = sut(today = today)

        assertEquals(emptyList(), result)
    }

    @Test
    fun `dentistry record without next due date is excluded`() {
        val patient = newPatient(1L)
        val withoutDueDate = newDentistry(1L, patient.id, null)
        stubPatientWithDentistry(patient.id, listOf(withoutDueDate))

        val result = sut(today = today)

        assertEquals(emptyList(), result)
    }

    @Test
    fun `multiple patients are aggregated`() {
        val patientOne = newPatient(1L)
        val patientTwo = newPatient(2L)
        val recordOne = newDentistry(1L, patientOne.id, today.plus(DatePeriod(days = 90)))
        val recordTwo = newDentistry(2L, patientTwo.id, today.plus(DatePeriod(days = 30)))
        every { patientRepositoryMock.getPatientList() } returns listOf(patientOne, patientTwo)
        every { dentistryRepositoryMock.getByPatient(patientOne.id) } returns listOf(recordOne)
        every { dentistryRepositoryMock.getByPatient(patientTwo.id) } returns listOf(recordTwo)

        val result = sut(today = today)

        assertEquals(listOf(patientTwo.id, patientOne.id), result.map { it.patientId })
    }

    @Test
    fun `empty patient list yields no reminders`() {
        every { patientRepositoryMock.getPatientList() } returns emptyList()

        val result = sut(today = today)

        assertEquals(emptyList(), result)
    }
}
