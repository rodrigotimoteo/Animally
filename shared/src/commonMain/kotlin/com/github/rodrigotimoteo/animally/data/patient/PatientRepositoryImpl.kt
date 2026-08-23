package com.github.rodrigotimoteo.animally.data.patient

import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.data.anamnese.AnamneseQueries
import com.github.rodrigotimoteo.animally.data.consultation.ConsultationQueries
import com.github.rodrigotimoteo.animally.data.dentistry.DentistryQueries
import com.github.rodrigotimoteo.animally.data.deworming.DewormingQueries
import com.github.rodrigotimoteo.animally.data.farrier.FarrierVisitQueries
import com.github.rodrigotimoteo.animally.data.gestation.GestationQueries
import com.github.rodrigotimoteo.animally.data.imaging.ImagingQueries
import com.github.rodrigotimoteo.animally.data.labresult.LabResultQueries
import com.github.rodrigotimoteo.animally.data.lameness.LamenessQueries
import com.github.rodrigotimoteo.animally.data.medication.MedicationQueries
import com.github.rodrigotimoteo.animally.data.patient.mapper.toDomain
import com.github.rodrigotimoteo.animally.data.reproduction.ReproductionQueries
import com.github.rodrigotimoteo.animally.data.repromedication.ReproMedicationQueries
import com.github.rodrigotimoteo.animally.data.substance.SubstanceQueries
import com.github.rodrigotimoteo.animally.data.surgery.SurgeryQueries
import com.github.rodrigotimoteo.animally.data.ultrasound.UltrasoundQueries
import com.github.rodrigotimoteo.animally.data.vaccination.VaccinationQueries
import com.github.rodrigotimoteo.animally.data.weight.WeightQueries
import com.github.rodrigotimoteo.animally.domain.patient.IPatientRepository
import com.github.rodrigotimoteo.animally.domain.patient.model.Patient
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single
import kotlin.time.Instant

/**
 * Repository implementation for managing [Patient] records.
 */
@Single(binds = [IPatientRepository::class])
class PatientRepositoryImpl(
    @Provided private val database: AnimallyDatabase,
) : IPatientRepository {
    private val patientQueries: PatientQueries = database.patientQueries
    private val anamneseQueries: AnamneseQueries = database.anamneseQueries
    private val consultationQueries: ConsultationQueries = database.consultationQueries
    private val dentistryQueries: DentistryQueries = database.dentistryQueries
    private val dewormingQueries: DewormingQueries = database.dewormingQueries
    private val farrierVisitQueries: FarrierVisitQueries = database.farrierVisitQueries
    private val gestationQueries: GestationQueries = database.gestationQueries
    private val imagingQueries: ImagingQueries = database.imagingQueries
    private val labResultQueries: LabResultQueries = database.labResultQueries
    private val lamenessQueries: LamenessQueries = database.lamenessQueries
    private val medicationQueries: MedicationQueries = database.medicationQueries
    private val reproMedicationQueries: ReproMedicationQueries = database.reproMedicationQueries
    private val reproductionQueries: ReproductionQueries = database.reproductionQueries
    private val substanceQueries: SubstanceQueries = database.substanceQueries
    private val surgeryQueries: SurgeryQueries = database.surgeryQueries
    private val ultrasoundQueries: UltrasoundQueries = database.ultrasoundQueries
    private val vaccinationQueries: VaccinationQueries = database.vaccinationQueries
    private val weightQueries: WeightQueries = database.weightQueries

    override fun getPatientsByOwnerId(ownerId: Long): List<Patient> =
        patientQueries
            .selectActiveByOwnerId(ownerId)
            .executeAsList()
            .map { it.toDomain() }

    override fun countPatientsByOwnerId(ownerId: Long): Long =
        patientQueries
            .countActiveByOwnerId(ownerId)
            .executeAsOne()

    override fun getPatientList(): List<Patient> =
        patientQueries
            .selectAll()
            .executeAsList()
            .map { it.toDomain() }

    override fun getPatientById(id: Long): Patient? = patientQueries.selectById(id).executeAsOneOrNull()?.toDomain()

    override fun insertPatient(patient: Patient): Long =
        database.transactionWithResult {
            patientQueries
                .insert(
                    name = patient.name,
                    species = patient.species,
                    breed = patient.breed,
                    dateOfBirth = patient.dateOfBirth,
                    gender = patient.gender,
                    microchipId = patient.microchipId,
                    ueln = patient.ueln,
                    registrationNumber = patient.registrationNumber,
                    stableLocation = patient.stableLocation,
                    photoUri = patient.photoUri,
                    notes = patient.notes,
                    ownerId = patient.ownerId,
                    isActive = patient.isActive,
                    createdAt = patient.createdAt,
                    updatedAt = patient.updatedAt,
                    cogginsTestDate = patient.cogginsTestDate,
                    cogginsResult = patient.cogginsResult,
                    cogginsExpiryDate = patient.cogginsExpiryDate,
                )
            database.commonQueries.selectLastRowId().executeAsOne()
        }

    override fun updatePatient(patient: Patient): Long =
        patientQueries
            .update(
                id = patient.id,
                name = patient.name,
                species = patient.species,
                breed = patient.breed,
                dateOfBirth = patient.dateOfBirth,
                gender = patient.gender,
                microchipId = patient.microchipId,
                ueln = patient.ueln,
                registrationNumber = patient.registrationNumber,
                stableLocation = patient.stableLocation,
                photoUri = patient.photoUri,
                notes = patient.notes,
                ownerId = patient.ownerId,
                isActive = patient.isActive,
                updatedAt = patient.updatedAt,
                cogginsTestDate = patient.cogginsTestDate,
                cogginsResult = patient.cogginsResult,
                cogginsExpiryDate = patient.cogginsExpiryDate,
            ).value

    override fun setInactive(
        id: Long,
        updatedAt: Instant,
    ): Long =
        patientQueries
            .setInactive(
                id = id,
                updatedAt = updatedAt,
            ).value

    override fun countActiveRecords(patientId: Long): Long =
        anamneseQueries.countActiveByPatient(patientId).executeAsOne() +
            consultationQueries.countActiveByPatient(patientId).executeAsOne() +
            dentistryQueries.countActiveByPatient(patientId).executeAsOne() +
            dewormingQueries.countActiveByPatient(patientId).executeAsOne() +
            farrierVisitQueries.countActiveByPatient(patientId).executeAsOne() +
            gestationQueries.countActiveByPatient(patientId).executeAsOne() +
            imagingQueries.countActiveByPatient(patientId).executeAsOne() +
            labResultQueries.countActiveByPatient(patientId).executeAsOne() +
            lamenessQueries.countActiveByPatient(patientId).executeAsOne() +
            medicationQueries.countActiveByPatient(patientId).executeAsOne() +
            reproMedicationQueries.countActiveByPatient(patientId).executeAsOne() +
            reproductionQueries.countActiveByPatient(patientId).executeAsOne() +
            substanceQueries.countActiveByPatient(patientId).executeAsOne() +
            surgeryQueries.countActiveByPatient(patientId).executeAsOne() +
            ultrasoundQueries.countActiveByPatient(patientId).executeAsOne() +
            vaccinationQueries.countActiveByPatient(patientId).executeAsOne() +
            weightQueries.countActiveByPatient(patientId).executeAsOne()
}
