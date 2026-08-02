package com.github.rodrigotimoteo.animally.domain.reminder.usecase

import com.github.rodrigotimoteo.animally.domain.patient.IPatientRepository
import com.github.rodrigotimoteo.animally.domain.patient.model.Patient
import com.github.rodrigotimoteo.animally.domain.vaccination.IVaccinationRepository
import com.github.rodrigotimoteo.animally.domain.vaccination.model.Vaccination
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

class GetVaccinationRemindersUseCaseTest {
    private val vaccinationRepositoryMock: IVaccinationRepository = mock()

    private val patientRepositoryMock: IPatientRepository = mock()

    private lateinit var sut: GetVaccinationRemindersUseCase

    private val today = LocalDate(2025, 1, 15)

    @BeforeTest
    fun setup() {
        sut = GetVaccinationRemindersUseCase(vaccinationRepositoryMock, patientRepositoryMock)
    }

    private fun newPatient(id: Long): Patient =
        Patient(
            id = id,
            name = "Horse $id",
            createdAt = Instant.fromEpochMilliseconds(0L),
            updatedAt = Instant.fromEpochMilliseconds(0L),
        )

    private fun newVaccination(
        id: Long,
        patientId: Long,
        vaccineName: String,
        nextDueDate: LocalDate?,
        isActive: Boolean = true,
    ): Vaccination =
        Vaccination(
            id = id,
            patientId = patientId,
            vaccineName = vaccineName,
            dateAdministered = today,
            nextDueDate = nextDueDate,
            isActive = isActive,
            createdAt = Instant.fromEpochMilliseconds(0L),
            updatedAt = Instant.fromEpochMilliseconds(0L),
        )

    private fun stubPatientWithVaccinations(
        patientId: Long,
        vaccinations: List<Vaccination>,
    ) {
        every { patientRepositoryMock.getPatientList() } returns listOf(newPatient(patientId))
        every { vaccinationRepositoryMock.getByPatient(patientId) } returns vaccinations
    }

    @Test
    fun `upcoming vaccination is included with resolved patient name`() {
        val patient = newPatient(1L)
        val vaccination = newVaccination(1L, patient.id, "Tetanus", today.plus(DatePeriod(days = 30)))
        stubPatientWithVaccinations(patient.id, listOf(vaccination))

        val result = sut(today = today)

        assertEquals(1, result.size)
        assertEquals(patient.id, result.single().patientId)
        assertEquals(patient.name, result.single().patientName)
        assertEquals("Vaccination", result.single().recordType)
        assertEquals("Tetanus", result.single().title)
        assertEquals(today.plus(DatePeriod(days = 30)), result.single().dueDate)
    }

    @Test
    fun `overdue vaccination is included`() {
        val patient = newPatient(1L)
        val vaccination = newVaccination(1L, patient.id, "Influenza", today.minus(DatePeriod(days = 5)))
        stubPatientWithVaccinations(patient.id, listOf(vaccination))

        val result = sut(today = today)

        assertEquals(1, result.size)
        assertEquals(today.minus(DatePeriod(days = 5)), result.single().dueDate)
    }

    @Test
    fun `due today is included`() {
        val patient = newPatient(1L)
        val vaccination = newVaccination(1L, patient.id, "Tetanus", today)
        stubPatientWithVaccinations(patient.id, listOf(vaccination))

        val result = sut(today = today)

        assertEquals(1, result.size)
        assertEquals(today, result.single().dueDate)
    }

    @Test
    fun `results are sorted by due date`() {
        val patient = newPatient(1L)
        val later = newVaccination(1L, patient.id, "Tetanus", today.plus(DatePeriod(days = 20)))
        val overdue = newVaccination(2L, patient.id, "Influenza", today.minus(DatePeriod(days = 3)))
        val soon = newVaccination(3L, patient.id, "West Nile", today.plus(DatePeriod(days = 5)))
        stubPatientWithVaccinations(patient.id, listOf(later, overdue, soon))

        val result = sut(today = today)

        assertEquals(listOf("Influenza", "West Nile", "Tetanus"), result.map { it.title })
        assertEquals(
            listOf(
                today.minus(DatePeriod(days = 3)),
                today.plus(DatePeriod(days = 5)),
                today.plus(DatePeriod(days = 20)),
            ),
            result.map { it.dueDate },
        )
    }

    @Test
    fun `inactive vaccination is excluded`() {
        val patient = newPatient(1L)
        val inactive = newVaccination(1L, patient.id, "Tetanus", today.plus(DatePeriod(days = 10)), isActive = false)
        stubPatientWithVaccinations(patient.id, listOf(inactive))

        val result = sut(today = today)

        assertEquals(emptyList(), result)
    }

    @Test
    fun `vaccination without next due date is excluded`() {
        val patient = newPatient(1L)
        val withoutDueDate = newVaccination(1L, patient.id, "Tetanus", null)
        stubPatientWithVaccinations(patient.id, listOf(withoutDueDate))

        val result = sut(today = today)

        assertEquals(emptyList(), result)
    }

    @Test
    fun `multiple patients are aggregated`() {
        val patientOne = newPatient(1L)
        val patientTwo = newPatient(2L)
        val vaccinationOne = newVaccination(1L, patientOne.id, "Tetanus", today.plus(DatePeriod(days = 10)))
        val vaccinationTwo = newVaccination(2L, patientTwo.id, "Influenza", today.plus(DatePeriod(days = 3)))
        every { patientRepositoryMock.getPatientList() } returns listOf(patientOne, patientTwo)
        every { vaccinationRepositoryMock.getByPatient(patientOne.id) } returns listOf(vaccinationOne)
        every { vaccinationRepositoryMock.getByPatient(patientTwo.id) } returns listOf(vaccinationTwo)

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
