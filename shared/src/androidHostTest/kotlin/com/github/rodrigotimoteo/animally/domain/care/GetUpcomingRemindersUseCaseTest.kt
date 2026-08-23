package com.github.rodrigotimoteo.animally.domain.care

import com.github.rodrigotimoteo.animally.data.customreminder.CustomReminderRepositoryImpl
import com.github.rodrigotimoteo.animally.data.farrier.FarrierVisitRepositoryImpl
import com.github.rodrigotimoteo.animally.data.gestation.GestationRepositoryImpl
import com.github.rodrigotimoteo.animally.data.patient.PatientRepositoryImpl
import com.github.rodrigotimoteo.animally.data.vaccination.VaccinationRepositoryImpl
import com.github.rodrigotimoteo.animally.di.database.createTestDatabase
import com.github.rodrigotimoteo.animally.domain.customreminder.model.CustomReminder
import com.github.rodrigotimoteo.animally.domain.farrier.model.FarrierVisit
import com.github.rodrigotimoteo.animally.domain.gestation.model.Gestation
import com.github.rodrigotimoteo.animally.domain.patient.model.Patient
import com.github.rodrigotimoteo.animally.domain.vaccination.model.Vaccination
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Instant

/**
 * Host-JVM tests for the Care Due aggregation: window filtering, overdue
 * flags, and date-ascending ordering across record types.
 */
class GetUpcomingRemindersUseCaseTest {
    private lateinit var sut: GetUpcomingRemindersUseCase
    private lateinit var patientRepo: PatientRepositoryImpl
    private lateinit var vaccinationRepo: VaccinationRepositoryImpl
    private lateinit var farrierRepo: FarrierVisitRepositoryImpl
    private lateinit var gestationRepo: GestationRepositoryImpl
    private lateinit var customReminderRepo: CustomReminderRepositoryImpl

    private val today = LocalDate(2026, 8, 23)
    private val epoch = Instant.fromEpochMilliseconds(0L)

    @BeforeTest
    fun setup() {
        val database = createTestDatabase()
        patientRepo = PatientRepositoryImpl(database)
        vaccinationRepo = VaccinationRepositoryImpl(database)
        farrierRepo = FarrierVisitRepositoryImpl(database)
        gestationRepo = GestationRepositoryImpl(database)
        customReminderRepo = CustomReminderRepositoryImpl(database)
        sut =
            GetUpcomingRemindersUseCase(
                vaccinationRepository = vaccinationRepo,
                dentistryRepository =
                    com.github.rodrigotimoteo.animally.data.dentistry
                        .DentistryRepositoryImpl(database),
                farrierVisitRepository = farrierRepo,
                gestationRepository = gestationRepo,
                customReminderRepository = customReminderRepo,
            )
    }

    @Test
    fun `given mixed due dates when invoked then returns in-window items sorted ascending with overdue flags`() {
        val patientId =
            patientRepo.insertPatient(
                Patient(id = 0L, name = "Thunder", createdAt = epoch, updatedAt = epoch),
            )
        seedVaccination(patientId, "Tetanus", today.minus(DatePeriod(days = 3)))
        seedFarrier(patientId, today.plus(DatePeriod(days = 10)))
        seedCustomReminder(patientId, "Annual check", today.plus(DatePeriod(days = 60)))
        seedGestation(patientId, today.plus(DatePeriod(days = 20)))

        val items = sut(patientId, today)

        assertEquals(3, items.size, "custom reminder beyond the 30-day window must be filtered out")
        assertEquals(listOf("Vaccination", "Farrier", "Gestation"), items.map { it.typeLabel })
        assertTrue(items[0].overdue, "vaccination due 3 days ago is overdue")
        assertFalse(items[1].overdue)
        assertFalse(items[2].overdue)
        assertEquals("Tetanus", items[0].title)
        assertEquals(today.minus(DatePeriod(days = 3)), items[0].dueDate)
    }

    @Test
    fun `given item due exactly on horizon when invoked then included`() {
        val patientId =
            patientRepo.insertPatient(
                Patient(id = 0L, name = "Storm", createdAt = epoch, updatedAt = epoch),
            )
        seedFarrier(patientId, today.plus(DatePeriod(days = 30)))

        val items = sut(patientId, today)

        assertEquals(1, items.size, "the horizon day itself is inside the inclusive window")
    }

    @Test
    fun `given no due records when invoked then empty list`() {
        val patientId =
            patientRepo.insertPatient(
                Patient(id = 0L, name = "Empty", createdAt = epoch, updatedAt = epoch),
            )

        assertEquals(0, sut(patientId, today).size)
    }

    private fun seedVaccination(
        patientId: Long,
        vaccineName: String,
        nextDueDate: LocalDate,
    ) {
        vaccinationRepo.insert(
            Vaccination(
                id = 0L,
                patientId = patientId,
                vaccineName = vaccineName,
                dateAdministered = today.minus(DatePeriod(days = 365)),
                nextDueDate = nextDueDate,
                createdAt = epoch,
                updatedAt = epoch,
            ),
        )
    }

    private fun seedFarrier(
        patientId: Long,
        nextDueDate: LocalDate,
    ) {
        farrierRepo.insert(
            FarrierVisit(
                id = 0L,
                patientId = patientId,
                date = today.minus(DatePeriod(days = 30)),
                trimOrShoe = "Full shoeing",
                nextDueDate = nextDueDate,
                createdAt = epoch,
                updatedAt = epoch,
            ),
        )
    }

    private fun seedGestation(
        patientId: Long,
        expectedDueDate: LocalDate,
    ) {
        gestationRepo.insert(
            Gestation(
                id = 0L,
                patientId = patientId,
                breedingDate = today.minus(DatePeriod(days = 320)),
                expectedDueDate = expectedDueDate,
                gestationDays = 320,
                status = "In foal",
                createdAt = epoch,
                updatedAt = epoch,
            ),
        )
    }

    private fun seedCustomReminder(
        patientId: Long,
        title: String,
        dueDate: LocalDate,
    ) {
        customReminderRepo.insert(
            CustomReminder(
                id = 0L,
                patientId = patientId,
                title = title,
                dueDate = dueDate,
                createdAt = epoch,
                updatedAt = epoch,
            ),
        )
    }
}
