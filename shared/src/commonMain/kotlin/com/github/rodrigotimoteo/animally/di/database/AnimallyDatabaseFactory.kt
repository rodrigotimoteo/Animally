package com.github.rodrigotimoteo.animally.di.database

import app.cash.sqldelight.db.SqlDriver
import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.data.adapters.InstantAdapter
import com.github.rodrigotimoteo.animally.data.adapters.LocalDateAdapter
import com.github.rodrigotimoteo.animally.data.migrations.Anamnese
import com.github.rodrigotimoteo.animally.data.migrations.Consultation
import com.github.rodrigotimoteo.animally.data.migrations.Dentistry
import com.github.rodrigotimoteo.animally.data.migrations.Deworming
import com.github.rodrigotimoteo.animally.data.migrations.FarrierVisit
import com.github.rodrigotimoteo.animally.data.migrations.Gestation
import com.github.rodrigotimoteo.animally.data.migrations.Imaging
import com.github.rodrigotimoteo.animally.data.migrations.LabResult
import com.github.rodrigotimoteo.animally.data.migrations.Lameness
import com.github.rodrigotimoteo.animally.data.migrations.Medication
import com.github.rodrigotimoteo.animally.data.migrations.Owner
import com.github.rodrigotimoteo.animally.data.migrations.Patient
import com.github.rodrigotimoteo.animally.data.migrations.ReproMedication
import com.github.rodrigotimoteo.animally.data.migrations.Reproduction
import com.github.rodrigotimoteo.animally.data.migrations.Substance
import com.github.rodrigotimoteo.animally.data.migrations.Surgery
import com.github.rodrigotimoteo.animally.data.migrations.Ultrasound
import com.github.rodrigotimoteo.animally.data.migrations.Vaccination
import com.github.rodrigotimoteo.animally.data.migrations.Weight

/**
 * Factory object for creating an instance of [AnimallyDatabase] with the appropriate adapters for
 * date and time types.
 *
 * @author rodrigotimoteo
 */
object AnimallyDatabaseFactory {
    /**
     * Creates an instance of [AnimallyDatabase] with the appropriate adapters for date and time types.
     *
     * @param driver The [SqlDriver] used to interact with the database.
     * @return An instance of [AnimallyDatabase].
     */
    fun create(driver: SqlDriver): AnimallyDatabase =
        AnimallyDatabase(
            driver = driver,
            AnamneseAdapter =
                Anamnese.Adapter(
                    createdAtAdapter = InstantAdapter,
                    updatedAtAdapter = InstantAdapter,
                ),
            ConsultationAdapter =
                Consultation.Adapter(
                    dateAdapter = LocalDateAdapter,
                    nextVisitDateAdapter = LocalDateAdapter,
                    createdAtAdapter = InstantAdapter,
                    updatedAtAdapter = InstantAdapter,
                ),
            DentistryAdapter =
                Dentistry.Adapter(
                    dateAdapter = LocalDateAdapter,
                    nextDueDateAdapter = LocalDateAdapter,
                    createdAtAdapter = InstantAdapter,
                    updatedAtAdapter = InstantAdapter,
                ),
            DewormingAdapter =
                Deworming.Adapter(
                    dateAdministeredAdapter = LocalDateAdapter,
                    nextDueDateAdapter = LocalDateAdapter,
                    createdAtAdapter = InstantAdapter,
                    updatedAtAdapter = InstantAdapter,
                ),
            FarrierVisitAdapter =
                FarrierVisit.Adapter(
                    dateAdapter = LocalDateAdapter,
                    nextDueDateAdapter = LocalDateAdapter,
                    createdAtAdapter = InstantAdapter,
                    updatedAtAdapter = InstantAdapter,
                ),
            GestationAdapter =
                Gestation.Adapter(
                    breedingDateAdapter = LocalDateAdapter,
                    expectedDueDateAdapter = LocalDateAdapter,
                    lastCheckDateAdapter = LocalDateAdapter,
                    createdAtAdapter = InstantAdapter,
                    updatedAtAdapter = InstantAdapter,
                ),
            ImagingAdapter =
                Imaging.Adapter(
                    dateAdapter = LocalDateAdapter,
                    createdAtAdapter = InstantAdapter,
                    updatedAtAdapter = InstantAdapter,
                ),
            LabResultAdapter =
                LabResult.Adapter(
                    dateAdapter = LocalDateAdapter,
                    createdAtAdapter = InstantAdapter,
                    updatedAtAdapter = InstantAdapter,
                ),
            LamenessAdapter =
                Lameness.Adapter(
                    dateAdapter = LocalDateAdapter,
                    createdAtAdapter = InstantAdapter,
                    updatedAtAdapter = InstantAdapter,
                ),
            MedicationAdapter =
                Medication.Adapter(
                    startDateAdapter = LocalDateAdapter,
                    endDateAdapter = LocalDateAdapter,
                    createdAtAdapter = InstantAdapter,
                    updatedAtAdapter = InstantAdapter,
                ),
            OwnerAdapter =
                Owner.Adapter(
                    createdAtAdapter = InstantAdapter,
                    updatedAtAdapter = InstantAdapter,
                ),
            PatientAdapter =
                Patient.Adapter(
                    dateOfBirthAdapter = LocalDateAdapter,
                    createdAtAdapter = InstantAdapter,
                    updatedAtAdapter = InstantAdapter,
                ),
            ReproMedicationAdapter =
                ReproMedication.Adapter(
                    dateAdministeredAdapter = LocalDateAdapter,
                    createdAtAdapter = InstantAdapter,
                    updatedAtAdapter = InstantAdapter,
                ),
            ReproductionAdapter =
                Reproduction.Adapter(
                    dateAdapter = LocalDateAdapter,
                    createdAtAdapter = InstantAdapter,
                    updatedAtAdapter = InstantAdapter,
                ),
            SubstanceAdapter =
                Substance.Adapter(
                    dateAdapter = LocalDateAdapter,
                    createdAtAdapter = InstantAdapter,
                    updatedAtAdapter = InstantAdapter,
                ),
            SurgeryAdapter =
                Surgery.Adapter(
                    dateAdapter = LocalDateAdapter,
                    createdAtAdapter = InstantAdapter,
                    updatedAtAdapter = InstantAdapter,
                ),
            UltrasoundAdapter =
                Ultrasound.Adapter(
                    dateAdapter = LocalDateAdapter,
                    createdAtAdapter = InstantAdapter,
                    updatedAtAdapter = InstantAdapter,
                ),
            VaccinationAdapter =
                Vaccination.Adapter(
                    dateAdministeredAdapter = LocalDateAdapter,
                    nextDueDateAdapter = LocalDateAdapter,
                    createdAtAdapter = InstantAdapter,
                    updatedAtAdapter = InstantAdapter,
                ),
            WeightAdapter =
                Weight.Adapter(
                    dateAdapter = LocalDateAdapter,
                    createdAtAdapter = InstantAdapter,
                    updatedAtAdapter = InstantAdapter,
                ),
        )
}
