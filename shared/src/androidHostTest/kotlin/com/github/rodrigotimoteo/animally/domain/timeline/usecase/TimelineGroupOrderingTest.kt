package com.github.rodrigotimoteo.animally.domain.timeline.usecase

import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.data.consultation.ConsultationRepositoryImpl
import com.github.rodrigotimoteo.animally.data.patient.PatientRepositoryImpl
import com.github.rodrigotimoteo.animally.data.vaccination.VaccinationRepositoryImpl
import com.github.rodrigotimoteo.animally.data.weight.WeightRepositoryImpl
import com.github.rodrigotimoteo.animally.di.database.createTestDatabase
import com.github.rodrigotimoteo.animally.domain.consultation.model.Consultation
import com.github.rodrigotimoteo.animally.domain.patient.model.Patient
import com.github.rodrigotimoteo.animally.domain.vaccination.model.Vaccination
import com.github.rodrigotimoteo.animally.domain.weight.model.Weight
import kotlinx.datetime.LocalDate
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

/**
 * Host-JVM tests for [GetTimelineUseCase] grouping order: groups sorted by date
 * descending, and entries within a group in a deterministic record-type order.
 */
class TimelineGroupOrderingTest {
    private lateinit var database: AnimallyDatabase
    private lateinit var sut: GetTimelineUseCase
    private lateinit var patientRepo: PatientRepositoryImpl
    private lateinit var weightRepo: WeightRepositoryImpl
    private lateinit var vaccinationRepo: VaccinationRepositoryImpl
    private lateinit var consultationRepo: ConsultationRepositoryImpl

    private val epoch = Instant.fromEpochMilliseconds(0L)

    @BeforeTest
    fun setup() {
        database = createTestDatabase()
        sut = GetTimelineUseCase(database)
        patientRepo = PatientRepositoryImpl(database)
        weightRepo = WeightRepositoryImpl(database)
        vaccinationRepo = VaccinationRepositoryImpl(database)
        consultationRepo = ConsultationRepositoryImpl(database)
    }

    private fun seedPatient(name: String): Long =
        patientRepo.insertPatient(
            Patient(id = 0L, name = name, createdAt = epoch, updatedAt = epoch),
        )

    @Test
    fun givenRecordsOnThreeDatesWhenFeedBuiltThenGroupsSortedByDateDescending() {
        val patientId = seedPatient("Thunder")
        seedWeight(patientId, LocalDate(2026, 1, 10))
        seedVaccination(patientId, "Tetanus", LocalDate(2026, 3, 1))
        seedConsultation(patientId, LocalDate(2026, 2, 15))

        val feed = sut(patientId)

        assertEquals(
            listOf(LocalDate(2026, 3, 1), LocalDate(2026, 2, 15), LocalDate(2026, 1, 10)),
            feed.groups.map { it.date },
            "groups must be ordered newest first",
        )
    }

    @Test
    fun givenMultipleRecordTypesOnSameDateWhenFeedBuiltThenEntriesInDeterministicTypeOrder() {
        val patientId = seedPatient("Thunder")
        // Insert in an order different from the use case's collection order to prove determinism.
        seedVaccination(patientId, "Tetanus", LocalDate(2026, 1, 10))
        seedConsultation(patientId, LocalDate(2026, 1, 10))
        seedWeight(patientId, LocalDate(2026, 1, 10))

        val feed = sut(patientId)

        val group = feed.groups.single()
        assertEquals(LocalDate(2026, 1, 10), group.date)
        assertEquals(
            listOf("Weight", "Consultation", "Vaccination"),
            group.entries.map { it.recordType },
            "same-date entries must follow the use case's fixed clinical-then-preventive type order",
        )
    }

    @Test
    fun givenSameDateAcrossTwoPatientsWhenGlobalFeedBuiltThenGroupHoldsBothPatients() {
        val thunderId = seedPatient("Thunder")
        val stormId = seedPatient("Storm")
        seedWeight(thunderId, LocalDate(2026, 1, 10))
        seedVaccination(stormId, "Influenza", LocalDate(2026, 1, 10))

        val feed = sut()

        val group = feed.groups.single()
        assertEquals(LocalDate(2026, 1, 10), group.date)
        assertEquals(listOf("Weight", "Vaccination"), group.entries.map { it.recordType })
        assertEquals(listOf("Thunder", "Storm"), group.entries.map { it.patientName })
    }

    private fun seedWeight(
        patientId: Long,
        date: LocalDate,
    ) {
        weightRepo.insert(
            Weight(id = 0L, patientId = patientId, weightKg = 500.0, date = date, createdAt = epoch, updatedAt = epoch),
        )
    }

    private fun seedVaccination(
        patientId: Long,
        vaccineName: String,
        date: LocalDate,
    ) {
        vaccinationRepo.insert(
            Vaccination(
                id = 0L,
                patientId = patientId,
                vaccineName = vaccineName,
                dateAdministered = date,
                createdAt = epoch,
                updatedAt = epoch,
            ),
        )
    }

    private fun seedConsultation(
        patientId: Long,
        date: LocalDate,
    ) {
        consultationRepo.insert(
            Consultation(
                id = 0L,
                patientId = patientId,
                date = date,
                subjective = "Owner reports issue",
                objective = "Findings noted",
                assessment = "Assessment text",
                plan = "Rest",
                createdAt = epoch,
                updatedAt = epoch,
            ),
        )
    }
}
