package com.github.rodrigotimoteo.animally.domain.care

import com.github.rodrigotimoteo.animally.domain.customreminder.ICustomReminderRepository
import com.github.rodrigotimoteo.animally.domain.dentistry.IDentistryRepository
import com.github.rodrigotimoteo.animally.domain.farrier.IFarrierVisitRepository
import com.github.rodrigotimoteo.animally.domain.gestation.IGestationRepository
import com.github.rodrigotimoteo.animally.domain.gestation.model.Gestation
import com.github.rodrigotimoteo.animally.domain.vaccination.IVaccinationRepository
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.matcher.any
import dev.mokkery.mock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

/**
 * Common tests for the Care Due gestation filtering: only pregnancies that
 * have neither been completed (foaled) nor failed may surface an expected
 * foaling item. Other sources are stubbed empty; their aggregation is covered
 * by the host-JVM suite.
 */
class GetUpcomingRemindersGestationFilterTest {
    private val vaccinationRepo: IVaccinationRepository = mock()
    private val dentistryRepo: IDentistryRepository = mock()
    private val farrierRepo: IFarrierVisitRepository = mock()
    private val gestationRepo: IGestationRepository = mock()
    private val customReminderRepo: ICustomReminderRepository = mock()

    private lateinit var sut: GetUpcomingRemindersUseCase

    private val today = LocalDate(2026, 8, 23)

    @BeforeTest
    fun setup() {
        every { vaccinationRepo.getByPatient(any()) } returns emptyList()
        every { dentistryRepo.getByPatient(any()) } returns emptyList()
        every { farrierRepo.getByPatient(any()) } returns emptyList()
        every { customReminderRepo.getByPatient(any()) } returns emptyList()
        sut =
            GetUpcomingRemindersUseCase(
                vaccinationRepository = vaccinationRepo,
                dentistryRepository = dentistryRepo,
                farrierVisitRepository = farrierRepo,
                gestationRepository = gestationRepo,
                customReminderRepository = customReminderRepo,
            )
    }

    private fun gestation(status: String) =
        Gestation(
            id = 1L,
            patientId = 1L,
            breedingDate = today.minus(DatePeriod(days = 320)),
            expectedDueDate = today.plus(DatePeriod(days = 10)),
            gestationDays = 320,
            status = status,
            createdAt = Instant.fromEpochMilliseconds(0L),
            updatedAt = Instant.fromEpochMilliseconds(0L),
        )

    @Test
    fun `given active gestation when invoked then expected foaling item appears`() {
        every { gestationRepo.getByPatient(any()) } returns listOf(gestation("Active"))

        val items = sut(1L, today)

        assertEquals(1, items.size)
        assertEquals("Gestation", items.single().typeLabel)
    }

    @Test
    fun `given completed gestation when invoked then no foaling item appears`() {
        every { gestationRepo.getByPatient(any()) } returns listOf(gestation("Completed"))

        assertEquals(0, sut(1L, today).size)
    }

    @Test
    fun `given failed gestation when invoked then no foaling item appears`() {
        every { gestationRepo.getByPatient(any()) } returns listOf(gestation("Failed"))

        assertEquals(0, sut(1L, today).size)
    }
}
