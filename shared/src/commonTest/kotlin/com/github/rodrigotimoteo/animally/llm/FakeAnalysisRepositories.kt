package com.github.rodrigotimoteo.animally.llm

import com.github.rodrigotimoteo.animally.domain.deworming.IDewormingRepository
import com.github.rodrigotimoteo.animally.domain.deworming.model.Deworming
import com.github.rodrigotimoteo.animally.domain.farrier.IFarrierVisitRepository
import com.github.rodrigotimoteo.animally.domain.farrier.model.FarrierVisit
import com.github.rodrigotimoteo.animally.domain.gestation.IGestationRepository
import com.github.rodrigotimoteo.animally.domain.gestation.model.Gestation
import com.github.rodrigotimoteo.animally.domain.patient.IPatientRepository
import com.github.rodrigotimoteo.animally.domain.patient.model.Patient
import com.github.rodrigotimoteo.animally.domain.vaccination.IVaccinationRepository
import com.github.rodrigotimoteo.animally.domain.vaccination.model.Vaccination
import com.github.rodrigotimoteo.animally.domain.weight.IWeightRepository
import com.github.rodrigotimoteo.animally.domain.weight.model.Weight
import kotlinx.datetime.LocalDate
import kotlin.time.Instant

/** Shared epoch for fixture timestamps - values are irrelevant to the SUTs. */
internal val FIXTURE_INSTANT: Instant = Instant.fromEpochSeconds(0L)

/**
 * In-memory repository fakes for the analysis-mode tests. Only the read
 * paths used by AnalysisContextBuilder carry behavior; write paths throw so
 * accidental dependencies surface loudly.
 */
internal class FakePatientRepository(
    var patients: List<Patient> = emptyList(),
) : IPatientRepository {
    override fun getPatientsByOwnerId(ownerId: Long): List<Patient> = error("unused in tests")

    override fun countPatientsByOwnerId(ownerId: Long): Long = error("unused in tests")

    override fun getPatientList(): List<Patient> = patients

    override fun getPatientById(id: Long): Patient? = patients.firstOrNull { it.id == id }

    override fun insertPatient(patient: Patient): Long = error("unused in tests")

    override fun updatePatient(patient: Patient): Long = error("unused in tests")

    override fun setInactive(
        id: Long,
        updatedAt: Instant,
    ): Long = error("unused in tests")

    override fun countActiveRecords(patientId: Long): Long = error("unused in tests")

    override fun patientNames(): List<String> = patients.map(Patient::name)
}

internal class FakeWeightRepository(
    var entries: List<Weight> = emptyList(),
) : IWeightRepository {
    override fun getByPatient(patientId: Long): List<Weight> = entries.filter { it.patientId == patientId }

    override fun getById(id: Long): Weight? = entries.firstOrNull { it.id == id }

    override fun insert(weight: Weight): Long = error("unused in tests")

    override fun update(weight: Weight): Long = error("unused in tests")

    override fun setInactive(
        id: Long,
        updatedAt: Instant,
    ): Long = error("unused in tests")
}

internal class FakeVaccinationRepository(
    var entries: List<Vaccination> = emptyList(),
) : IVaccinationRepository {
    override fun getByPatient(patientId: Long): List<Vaccination> = entries.filter { it.patientId == patientId }

    override fun getById(id: Long): Vaccination? = entries.firstOrNull { it.id == id }

    override fun insert(vaccination: Vaccination): Long = error("unused in tests")

    override fun update(vaccination: Vaccination): Long = error("unused in tests")

    override fun setInactive(
        id: Long,
        updatedAt: Instant,
    ): Long = error("unused in tests")
}

internal class FakeDewormingRepository(
    var entries: List<Deworming> = emptyList(),
) : IDewormingRepository {
    override fun getByPatient(patientId: Long): List<Deworming> = entries.filter { it.patientId == patientId }

    override fun getById(id: Long): Deworming? = entries.firstOrNull { it.id == id }

    override fun insert(deworming: Deworming): Long = error("unused in tests")

    override fun update(deworming: Deworming): Long = error("unused in tests")

    override fun setInactive(
        id: Long,
        updatedAt: Instant,
    ): Long = error("unused in tests")
}

internal class FakeFarrierVisitRepository(
    var entries: List<FarrierVisit> = emptyList(),
) : IFarrierVisitRepository {
    override fun getByPatient(patientId: Long): List<FarrierVisit> = entries.filter { it.patientId == patientId }

    override fun getById(id: Long): FarrierVisit? = entries.firstOrNull { it.id == id }

    override fun insert(farrierVisit: FarrierVisit): Long = error("unused in tests")

    override fun update(farrierVisit: FarrierVisit): Long = error("unused in tests")

    override fun setInactive(
        id: Long,
        updatedAt: Instant,
    ): Long = error("unused in tests")
}

internal class FakeGestationRepository(
    var entries: List<Gestation> = emptyList(),
) : IGestationRepository {
    override fun getByPatient(patientId: Long): List<Gestation> = entries.filter { it.patientId == patientId }

    override fun getById(id: Long): Gestation? = entries.firstOrNull { it.id == id }

    override fun insert(gestation: Gestation): Long = error("unused in tests")

    override fun update(gestation: Gestation): Long = error("unused in tests")

    override fun setInactive(
        id: Long,
        updatedAt: Instant,
    ): Long = error("unused in tests")
}

/** Bundle of fakes wired into one AnalysisContextBuilder for tests. */
internal class FakeAnalysisRepos {
    val patients = FakePatientRepository()
    val weights = FakeWeightRepository()
    val vaccinations = FakeVaccinationRepository()
    val dewormings = FakeDewormingRepository()
    val farrierVisits = FakeFarrierVisitRepository()
    val gestations = FakeGestationRepository()

    // Positional ctor args keep each fixture a single expression line, which
    // is what the ktlint function-signature/multiline-expression rules want.
    val builder =
        AnalysisContextBuilder(
            patientRepository = patients,
            weightRepository = weights,
            vaccinationRepository = vaccinations,
            dewormingRepository = dewormings,
            farrierVisitRepository = farrierVisits,
            gestationRepository = gestations,
        )
}

internal fun testPatient(
    id: Long,
    name: String,
): Patient = Patient(id = id, name = name, createdAt = FIXTURE_INSTANT, updatedAt = FIXTURE_INSTANT)

internal fun testWeight(
    id: Long,
    patientId: Long,
    kg: Double,
    date: LocalDate,
): Weight = Weight(id, patientId, kg, date, null, true, FIXTURE_INSTANT, FIXTURE_INSTANT)

internal fun testVaccination(
    id: Long,
    patientId: Long,
    vaccineName: String,
    administered: LocalDate,
    nextDue: LocalDate? = null,
): Vaccination = Vaccination(id, patientId, vaccineName, administered, nextDue, null, null, null, null, true, FIXTURE_INSTANT, FIXTURE_INSTANT)

internal fun testDeworming(
    id: Long,
    patientId: Long,
    product: String,
    administered: LocalDate,
    nextDue: LocalDate? = null,
): Deworming = Deworming(id, patientId, product, administered, nextDue, null, null, null, true, FIXTURE_INSTANT, FIXTURE_INSTANT)

internal fun testFarrierVisit(
    id: Long,
    patientId: Long,
    date: LocalDate,
    nextDue: LocalDate? = null,
): FarrierVisit = FarrierVisit(id, patientId, date, null, null, null, nextDue, null, null, true, FIXTURE_INSTANT, FIXTURE_INSTANT)

internal fun testGestation(
    id: Long,
    patientId: Long,
    breedingDate: LocalDate,
    expectedDueDate: LocalDate,
    status: String = "Active",
): Gestation = Gestation(id, patientId, breedingDate, expectedDueDate, 0, status, null, null, null, true, FIXTURE_INSTANT, FIXTURE_INSTANT)
