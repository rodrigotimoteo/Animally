package com.github.rodrigotimoteo.animally.domain.export.pdf

import com.github.rodrigotimoteo.animally.domain.anamnese.IAnamneseRepository
import com.github.rodrigotimoteo.animally.domain.consultation.IConsultationRepository
import com.github.rodrigotimoteo.animally.domain.consultation.model.Consultation
import com.github.rodrigotimoteo.animally.domain.dentistry.IDentistryRepository
import com.github.rodrigotimoteo.animally.domain.deworming.IDewormingRepository
import com.github.rodrigotimoteo.animally.domain.export.ExportBasicRecordsUseCase
import com.github.rodrigotimoteo.animally.domain.export.ExportClinicalRecordsUseCase
import com.github.rodrigotimoteo.animally.domain.export.ExportReproductiveRecordsUseCase
import com.github.rodrigotimoteo.animally.domain.farrier.IFarrierVisitRepository
import com.github.rodrigotimoteo.animally.domain.gestation.IGestationRepository
import com.github.rodrigotimoteo.animally.domain.imaging.IImagingRepository
import com.github.rodrigotimoteo.animally.domain.labresult.ILabResultRepository
import com.github.rodrigotimoteo.animally.domain.lameness.ILamenessRepository
import com.github.rodrigotimoteo.animally.domain.medication.IMedicationRepository
import com.github.rodrigotimoteo.animally.domain.patient.IPatientRepository
import com.github.rodrigotimoteo.animally.domain.patient.model.Patient
import com.github.rodrigotimoteo.animally.domain.reproduction.IReproductionRepository
import com.github.rodrigotimoteo.animally.domain.repromedication.IReproMedicationRepository
import com.github.rodrigotimoteo.animally.domain.substance.IControlledSubstanceRepository
import com.github.rodrigotimoteo.animally.domain.surgery.ISurgeryRepository
import com.github.rodrigotimoteo.animally.domain.ultrasound.IUltrasoundRepository
import com.github.rodrigotimoteo.animally.domain.vaccination.IVaccinationRepository
import com.github.rodrigotimoteo.animally.domain.weight.IWeightRepository
import com.github.rodrigotimoteo.animally.domain.weight.model.Weight
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.matcher.any
import dev.mokkery.mock
import kotlinx.datetime.LocalDate
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Instant

class ExportPatientReportUseCaseTest {
    private val patientRepository: IPatientRepository = mock()
    private val anamneseRepository: IAnamneseRepository = mock()
    private val weightRepository: IWeightRepository = mock()
    private val consultationRepository: IConsultationRepository = mock()
    private val vaccinationRepository: IVaccinationRepository = mock()
    private val dewormingRepository: IDewormingRepository = mock()
    private val dentistryRepository: IDentistryRepository = mock()
    private val lamenessRepository: ILamenessRepository = mock()
    private val surgeryRepository: ISurgeryRepository = mock()
    private val medicationRepository: IMedicationRepository = mock()
    private val labResultRepository: ILabResultRepository = mock()
    private val imagingRepository: IImagingRepository = mock()
    private val farrierRepository: IFarrierVisitRepository = mock()
    private val reproductionRepository: IReproductionRepository = mock()
    private val ultrasoundRepository: IUltrasoundRepository = mock()
    private val gestationRepository: IGestationRepository = mock()
    private val reproMedicationRepository: IReproMedicationRepository = mock()
    private val substanceRepository: IControlledSubstanceRepository = mock()

    private val sut =
        ExportPatientReportUseCase(
            patientRepository = patientRepository,
            basicRecords =
                ExportBasicRecordsUseCase(
                    anamneseRepository,
                    weightRepository,
                    consultationRepository,
                    vaccinationRepository,
                    dewormingRepository,
                    dentistryRepository,
                ),
            clinicalRecords =
                ExportClinicalRecordsUseCase(
                    lamenessRepository,
                    surgeryRepository,
                    medicationRepository,
                    labResultRepository,
                    imagingRepository,
                    farrierRepository,
                ),
            reproductiveRecords =
                ExportReproductiveRecordsUseCase(
                    reproductionRepository,
                    ultrasoundRepository,
                    gestationRepository,
                    reproMedicationRepository,
                    substanceRepository,
                ),
        )

    private val patient =
        Patient(
            id = 1L,
            name = "Thunder",
            species = "Equine",
            breed = "Arabian",
            ueln = "826123456789012",
            ownerId = 7L,
            isActive = true,
            createdAt = Instant.fromEpochMilliseconds(0L),
            updatedAt = Instant.fromEpochMilliseconds(0L),
        )

    @BeforeTest
    fun stubEmptyRepositories() {
        every { anamneseRepository.getByPatient(any()) } returns null
        every { weightRepository.getByPatient(any()) } returns emptyList()
        every { consultationRepository.getByPatient(any()) } returns emptyList()
        every { vaccinationRepository.getByPatient(any()) } returns emptyList()
        every { dewormingRepository.getByPatient(any()) } returns emptyList()
        every { dentistryRepository.getByPatient(any()) } returns emptyList()
        every { lamenessRepository.getByPatient(any()) } returns emptyList()
        every { surgeryRepository.getByPatient(any()) } returns emptyList()
        every { medicationRepository.getByPatient(any()) } returns emptyList()
        every { labResultRepository.getByPatient(any()) } returns emptyList()
        every { imagingRepository.getByPatient(any()) } returns emptyList()
        every { farrierRepository.getByPatient(any()) } returns emptyList()
        every { reproductionRepository.getByPatient(any()) } returns emptyList()
        every { ultrasoundRepository.getByPatient(any()) } returns emptyList()
        every { gestationRepository.getByPatient(any()) } returns emptyList()
        every { reproMedicationRepository.getByPatient(any()) } returns emptyList()
        every { substanceRepository.getByPatient(any()) } returns emptyList()
    }

    private fun consultation(
        id: Long,
        date: LocalDate,
    ): Consultation =
        Consultation(
            id = id,
            patientId = patient.id,
            date = date,
            subjective = "Subjective $id",
            objective = "Objective $id",
            assessment = "Assessment $id",
            plan = "Plan $id",
            vetName = "Dr. $id",
            isActive = true,
            createdAt = Instant.fromEpochMilliseconds(0L),
            updatedAt = Instant.fromEpochMilliseconds(0L),
        )

    @Test
    fun `includes patient demographics`() {
        every { patientRepository.getPatientById(1L) } returns patient

        val report = sut(patientId = 1L, from = null, to = null)

        assertEquals("Thunder", report.patient.name)
        assertEquals("Equine", report.patient.species)
        assertEquals("Arabian", report.patient.breed)
        assertEquals("826123456789012", report.patient.ueln)
    }

    @Test
    fun `builds a section per entity with header and data rows`() {
        every { patientRepository.getPatientById(1L) } returns patient
        every { weightRepository.getByPatient(1L) } returns
            listOf(
                Weight(
                    id = 1L,
                    patientId = 1L,
                    weightKg = 520.0,
                    date = LocalDate(2024, 4, 1),
                    isActive = true,
                    createdAt = Instant.fromEpochMilliseconds(0L),
                    updatedAt = Instant.fromEpochMilliseconds(0L),
                ),
            )
        every { consultationRepository.getByPatient(1L) } returns listOf(consultation(id = 5L, date = LocalDate(2024, 6, 15)))

        val report = sut(patientId = 1L, from = null, to = null)

        val weightSection = report.sections.single { it.title == "Weight" }
        assertEquals(listOf("Id", "PatientId", "WeightKg", "Date", "Notes"), weightSection.rows.first())
        assertEquals(listOf("1", "1", "520.0", "2024-04-01", ""), weightSection.rows.last())

        val consultationSection = report.sections.single { it.title == "Consultation" }
        assertEquals("5", consultationSection.rows.last().first())
        assertEquals("Dr. 5", consultationSection.rows.last()[7])
    }

    @Test
    fun `filters dated records by the date range`() {
        every { patientRepository.getPatientById(1L) } returns patient
        every { consultationRepository.getByPatient(1L) } returns
            listOf(
                consultation(id = 1L, date = LocalDate(2024, 1, 10)),
                consultation(id = 2L, date = LocalDate(2024, 6, 10)),
            )

        val report = sut(patientId = 1L, from = LocalDate(2024, 3, 1), to = LocalDate(2024, 12, 31))

        val rows = report.sections.single { it.title == "Consultation" }.rows
        assertEquals(2, rows.size) // header + one data row
        assertEquals("2", rows.last().first())
        assertTrue(rows.none { it.first() == "1" })
    }

    @Test
    fun `omits sections for entities without records`() {
        every { patientRepository.getPatientById(1L) } returns patient

        val report = sut(patientId = 1L, from = null, to = null)

        assertEquals(0, report.sections.size)
    }

    @Test
    fun `throws when the patient is not found`() {
        every { patientRepository.getPatientById(99L) } returns null

        assertFailsWith<IllegalArgumentException> {
            sut(patientId = 99L, from = null, to = null)
        }
    }
}
