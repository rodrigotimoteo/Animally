package com.github.rodrigotimoteo.animally.domain.timeline.usecase

import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.data.consultation.ConsultationRepositoryImpl
import com.github.rodrigotimoteo.animally.data.deworming.DewormingRepositoryImpl
import com.github.rodrigotimoteo.animally.data.labresult.LabResultRepositoryImpl
import com.github.rodrigotimoteo.animally.data.lameness.LamenessRepositoryImpl
import com.github.rodrigotimoteo.animally.data.medication.MedicationRepositoryImpl
import com.github.rodrigotimoteo.animally.data.patient.PatientRepositoryImpl
import com.github.rodrigotimoteo.animally.data.surgery.SurgeryRepositoryImpl
import com.github.rodrigotimoteo.animally.data.vaccination.VaccinationRepositoryImpl
import com.github.rodrigotimoteo.animally.data.weight.WeightRepositoryImpl
import com.github.rodrigotimoteo.animally.di.database.createTestDatabase
import com.github.rodrigotimoteo.animally.domain.consultation.model.Consultation
import com.github.rodrigotimoteo.animally.domain.deworming.model.Deworming
import com.github.rodrigotimoteo.animally.domain.labresult.model.LabResult
import com.github.rodrigotimoteo.animally.domain.lameness.model.Lameness
import com.github.rodrigotimoteo.animally.domain.medication.model.Medication
import com.github.rodrigotimoteo.animally.domain.patient.model.Patient
import com.github.rodrigotimoteo.animally.domain.surgery.model.Surgery
import com.github.rodrigotimoteo.animally.domain.vaccination.model.Vaccination
import com.github.rodrigotimoteo.animally.domain.weight.model.Weight
import kotlinx.datetime.LocalDate
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class GetTimelineUseCaseTest {
    private lateinit var database: AnimallyDatabase
    private lateinit var sut: GetTimelineUseCase
    private lateinit var patientRepo: PatientRepositoryImpl
    private lateinit var weightRepo: WeightRepositoryImpl
    private lateinit var dewormingRepo: DewormingRepositoryImpl
    private lateinit var vaccinationRepo: VaccinationRepositoryImpl
    private lateinit var consultationRepo: ConsultationRepositoryImpl
    private lateinit var lamenessRepo: LamenessRepositoryImpl
    private lateinit var surgeryRepo: SurgeryRepositoryImpl
    private lateinit var labResultRepo: LabResultRepositoryImpl
    private lateinit var medicationRepo: MedicationRepositoryImpl

    private val epoch = Instant.fromEpochMilliseconds(0L)

    @BeforeTest
    fun setup() {
        database = createTestDatabase()
        sut = GetTimelineUseCase(database)
        patientRepo = PatientRepositoryImpl(database)
        weightRepo = WeightRepositoryImpl(database)
        dewormingRepo = DewormingRepositoryImpl(database)
        vaccinationRepo = VaccinationRepositoryImpl(database)
        consultationRepo = ConsultationRepositoryImpl(database)
        lamenessRepo = LamenessRepositoryImpl(database)
        surgeryRepo = SurgeryRepositoryImpl(database)
        labResultRepo = LabResultRepositoryImpl(database)
        medicationRepo = MedicationRepositoryImpl(database)
    }

    private fun seedPatient(name: String): Long {
        patientRepo.insertPatient(
            Patient(
                id = 0L,
                name = name,
                createdAt = epoch,
                updatedAt = epoch,
            ),
        )
        return patientRepo.getPatientList().single { it.name == name }.id
    }

    private fun seedVaccination(
        patientId: Long,
        name: String,
        date: LocalDate,
    ) {
        vaccinationRepo.insert(
            Vaccination(
                id = 0L,
                patientId = patientId,
                vaccineName = name,
                dateAdministered = date,
                createdAt = epoch,
                updatedAt = epoch,
            ),
        )
    }

    private fun seedWeight(
        patientId: Long,
        weightKg: Double,
        date: LocalDate,
    ) {
        weightRepo.insert(
            Weight(
                id = 0L,
                patientId = patientId,
                weightKg = weightKg,
                date = date,
                createdAt = epoch,
                updatedAt = epoch,
            ),
        )
    }

    private fun seedConsultation(
        patientId: Long,
        date: LocalDate,
        assessment: String,
    ) {
        consultationRepo.insert(
            Consultation(
                id = 0L,
                patientId = patientId,
                date = date,
                subjective = "Owner reports lameness",
                objective = "Moderate lameness",
                assessment = assessment,
                plan = "Rest",
                createdAt = epoch,
                updatedAt = epoch,
            ),
        )
    }

    @Test
    fun `when database is empty then global and patient feeds are empty`() {
        val global = sut()
        assertNull(global.patientId)
        assertNull(global.patientName)
        assertTrue(global.groups.isEmpty())

        val patient = sut(999L)
        assertEquals(999L, patient.patientId)
        assertNull(patient.patientName)
        assertTrue(patient.groups.isEmpty())
    }

    @Test
    fun `when patient has no records then feed has no groups`() {
        val patientId = seedPatient("Charlie")

        val feed = sut(patientId)

        assertEquals(patientId, feed.patientId)
        assertEquals("Charlie", feed.patientName)
        assertTrue(feed.groups.isEmpty())
    }

    @Test
    fun `when records exist then groups are sorted by date descending and share dates are grouped`() {
        val patientId = seedPatient("Charlie")
        seedVaccination(patientId, "Tetanus", LocalDate(2024, 1, 15))
        seedWeight(patientId, 520.0, LocalDate(2024, 1, 15))
        seedConsultation(patientId, LocalDate(2024, 5, 1), "Laminitis resolved")

        val feed = sut(patientId)

        assertEquals(listOf(LocalDate(2024, 5, 1), LocalDate(2024, 1, 15)), feed.groups.map { it.date })

        val consultationGroup = feed.groups[0].entries.single()
        assertEquals("Consultation", consultationGroup.title)
        assertEquals("Laminitis resolved", consultationGroup.subtitle)

        val sameDayEntries = feed.groups[1].entries
        assertEquals(listOf("Weight", "Vaccination"), sameDayEntries.map { it.title })
        assertEquals(listOf("520.0 kg", "Tetanus"), sameDayEntries.map { it.subtitle })
        assertEquals(patientId, sameDayEntries.first().patientId)
        assertEquals("Charlie", sameDayEntries.first().patientName)
    }

    @Test
    fun `when subtitles for clinical record types then mapped to human readable summaries`() {
        val patientId = seedPatient("Charlie")
        dewormingRepo.insert(
            Deworming(
                id = 0L,
                patientId = patientId,
                product = "Panacur",
                dateAdministered = LocalDate(2024, 2, 1),
                createdAt = epoch,
                updatedAt = epoch,
            ),
        )
        lamenessRepo.insert(
            Lameness(
                id = 0L,
                patientId = patientId,
                date = LocalDate(2024, 3, 1),
                gradeAAEP = 2,
                createdAt = epoch,
                updatedAt = epoch,
            ),
        )
        surgeryRepo.insert(
            Surgery(
                id = 0L,
                patientId = patientId,
                date = LocalDate(2024, 4, 1),
                type = "Castration",
                createdAt = epoch,
                updatedAt = epoch,
            ),
        )
        labResultRepo.insert(
            LabResult(
                id = 0L,
                patientId = patientId,
                testType = "CBC",
                date = LocalDate(2024, 6, 1),
                createdAt = epoch,
                updatedAt = epoch,
            ),
        )

        val titles = sut(patientId).groups.flatMap { it.entries.map { entry -> entry.title } }
        val subtitles = sut(patientId).groups.flatMap { it.entries.map { entry -> entry.subtitle } }

        assertEquals(listOf("Lab Result", "Surgery", "Lameness", "Deworming"), titles)
        assertEquals(listOf("CBC", "Castration", "Grade 2", "Panacur"), subtitles)
    }

    @Test
    fun `when medication has no start date then skipped from feed`() {
        val patientId = seedPatient("Charlie")
        medicationRepo.insert(
            Medication(
                id = 0L,
                patientId = patientId,
                name = "Phenylbutazone",
                dosage = "2g",
                startDate = null,
                createdAt = epoch,
                updatedAt = epoch,
            ),
        )
        medicationRepo.insert(
            Medication(
                id = 0L,
                patientId = patientId,
                name = "Flunixin",
                dosage = "1g",
                startDate = LocalDate(2024, 7, 1),
                createdAt = epoch,
                updatedAt = epoch,
            ),
        )

        val entries = sut(patientId).groups.flatMap { it.entries }

        assertEquals(1, entries.size)
        assertEquals("Medication", entries.single().title)
        assertEquals("Flunixin", entries.single().subtitle)
        assertEquals(LocalDate(2024, 7, 1), entries.single().date)
    }

    @Test
    fun `when global feed then entries carry their patient name across patients`() {
        val charlieId = seedPatient("Charlie")
        val bellaId = seedPatient("Bella")
        seedVaccination(charlieId, "Tetanus", LocalDate(2024, 1, 15))
        seedVaccination(bellaId, "Influenza", LocalDate(2024, 2, 15))

        val feed = sut()

        assertNull(feed.patientId)
        assertNull(feed.patientName)
        assertEquals(listOf(LocalDate(2024, 2, 15), LocalDate(2024, 1, 15)), feed.groups.map { it.date })
        val entries = feed.groups.flatMap { it.entries }
        assertEquals(2, entries.size)
        assertEquals("Bella", entries[0].patientName)
        assertEquals("Charlie", entries[1].patientName)
        assertEquals(setOf("Tetanus", "Influenza"), entries.map { it.subtitle }.toSet())
    }
}
