package com.github.rodrigotimoteo.animally.data.repromedication

import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.data.repromedication.mapper.toDomain
import com.github.rodrigotimoteo.animally.domain.repromedication.IReproMedicationRepository
import com.github.rodrigotimoteo.animally.domain.repromedication.model.ReproMedication
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single
import kotlin.time.Instant

/**
 * Repository implementation for managing [ReproMedication] records.
 */
@Single(binds = [IReproMedicationRepository::class])
class ReproMedicationRepositoryImpl(
    @Provided private val database: AnimallyDatabase,
) : IReproMedicationRepository {
    private val reproMedicationQueries: ReproMedicationQueries = database.reproMedicationQueries

    override fun getByPatient(patientId: Long): List<ReproMedication> =
        reproMedicationQueries
            .selectByPatient(patientId)
            .executeAsList()
            .map { it.toDomain() }
            .sortedByDescending { it.dateAdministered }

    @Suppress("MaxLineLength")
    override fun getById(id: Long): ReproMedication? = reproMedicationQueries.selectById(id).executeAsOneOrNull()?.toDomain()

    override fun insert(reproMedication: ReproMedication): Long =
        reproMedicationQueries
            .insert(
                patientId = reproMedication.patientId,
                medication = reproMedication.medication,
                dateAdministered = reproMedication.dateAdministered,
                dosage = reproMedication.dosage,
                purpose = reproMedication.purpose,
                vetName = reproMedication.vetName,
                notes = reproMedication.notes,
                isActive = reproMedication.isActive,
                createdAt = reproMedication.createdAt,
                updatedAt = reproMedication.updatedAt,
            ).value

    override fun update(reproMedication: ReproMedication): Long =
        reproMedicationQueries
            .update(
                id = reproMedication.id,
                patientId = reproMedication.patientId,
                medication = reproMedication.medication,
                dateAdministered = reproMedication.dateAdministered,
                dosage = reproMedication.dosage,
                purpose = reproMedication.purpose,
                vetName = reproMedication.vetName,
                notes = reproMedication.notes,
                isActive = reproMedication.isActive,
                updatedAt = reproMedication.updatedAt,
            ).value

    override fun setInactive(
        id: Long,
        updatedAt: Instant,
    ): Long =
        reproMedicationQueries
            .setInactive(
                id = id,
                updatedAt = updatedAt,
            ).value
}
