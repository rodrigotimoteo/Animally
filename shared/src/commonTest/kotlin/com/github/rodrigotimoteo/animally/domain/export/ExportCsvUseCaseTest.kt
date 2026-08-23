package com.github.rodrigotimoteo.animally.domain.export

import com.github.rodrigotimoteo.animally.domain.anamnese.IAnamneseRepository
import com.github.rodrigotimoteo.animally.domain.consultation.IConsultationRepository
import com.github.rodrigotimoteo.animally.domain.consultation.model.Consultation
import com.github.rodrigotimoteo.animally.domain.dentistry.IDentistryRepository
import com.github.rodrigotimoteo.animally.domain.deworming.IDewormingRepository
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
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Instant

class ExportCsvUseCaseTest {
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

    private val basicRecords =
        ExportBasicRecordsUseCase(
            anamneseRepository,
            weightRepository,
            consultationRepository,
            vaccinationRepository,
            dewormingRepository,
            dentistryRepository,
        )

    private val clinicalRecords =
        ExportClinicalRecordsUseCase(
            lamenessRepository,
            surgeryRepository,
            medicationRepository,
            labResultRepository,
            imagingRepository,
            farrierRepository,
        )

    private val reproductiveRecords =
        ExportReproductiveRecordsUseCase(
            reproductionRepository,
            ultrasoundRepository,
            gestationRepository,
            reproMedicationRepository,
            substanceRepository,
        )

    private val sut =
        ExportCsvUseCase(
            patientRepository,
            basicRecords,
            clinicalRecords,
            reproductiveRecords,
            CsvExporter(),
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

    private val patient =
        Patient(
            id = 1L,
            name = "Thunder",
            species = "Equine",
            breed = "Arabian",
            ownerId = 7L,
            isActive = true,
            createdAt = Instant.fromEpochMilliseconds(0L),
            updatedAt = Instant.fromEpochMilliseconds(0L),
        )

    private val secondPatient =
        Patient(
            id = 2L,
            name = "Storm",
            species = "Equine",
            isActive = true,
            createdAt = Instant.fromEpochMilliseconds(0L),
            updatedAt = Instant.fromEpochMilliseconds(0L),
        )

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
    fun `exports patient row and consultation rows for a single patient`() {
        every { patientRepository.getPatientById(1L) } returns patient
        every { consultationRepository.getByPatient(1L) } returns listOf(consultation(id = 5L, date = LocalDate(2024, 6, 15)))

        val csv = sut(patientId = 1L, from = null, to = null)

        assertTrue(csv.contains("# Patient"))
        assertTrue(csv.contains("Patient,1,Thunder"))
        assertTrue(csv.contains("# Consultation"))
        assertTrue(csv.contains("Consultation,5,1,2024-06-15,Subjective 5,Objective 5,Assessment 5,Plan 5,Dr. 5,"))
    }

    @Test
    fun `prepends a utf8 bom so excel detects the encoding`() {
        every { patientRepository.getPatientById(1L) } returns patient

        val csv = sut(patientId = 1L, from = null, to = null)

        assertTrue(csv.startsWith(CsvFormatter.UTF8_BOM))
    }

    @Test
    fun `exports every patient when no patient id is given`() {
        every { patientRepository.getPatientList() } returns listOf(patient, secondPatient)
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
        every { weightRepository.getByPatient(2L) } returns
            listOf(
                Weight(
                    id = 2L,
                    patientId = 2L,
                    weightKg = 480.5,
                    date = LocalDate(2024, 5, 1),
                    isActive = true,
                    createdAt = Instant.fromEpochMilliseconds(0L),
                    updatedAt = Instant.fromEpochMilliseconds(0L),
                ),
            )

        val csv = sut(patientId = null, from = null, to = null)

        assertTrue(csv.contains("Patient,1,Thunder"))
        assertTrue(csv.contains("Patient,2,Storm"))
        assertTrue(csv.contains("520.0"))
        assertTrue(csv.contains("480.5"))
    }

    @Test
    fun `filters dated records by the date range`() {
        every { patientRepository.getPatientById(1L) } returns patient
        every { consultationRepository.getByPatient(1L) } returns
            listOf(
                consultation(id = 1L, date = LocalDate(2024, 1, 10)),
                consultation(id = 2L, date = LocalDate(2024, 6, 10)),
            )

        val csv = sut(patientId = 1L, from = LocalDate(2024, 3, 1), to = LocalDate(2024, 12, 31))

        assertTrue(csv.contains("Consultation,2,1,2024-06-10"))
        assertFalse(csv.contains("Consultation,1,1,2024-01-10"))
    }

    @Test
    fun `returns empty string when the patient is not found`() {
        every { patientRepository.getPatientById(99L) } returns null

        assertEquals("", sut(patientId = 99L, from = null, to = null))
    }
}
