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
    private val reproMedQueries: ReproMedicationQueries = database.reproMedicationQueries

    override fun getByPatient(patientId: Long): List<ReproMedication> =
        reproMedQueries
            .selectByPatient(patientId)
            .executeAsList()
            .map { it.toDomain() }
            .sortedByDescending { it.dateAdministered }

    override fun getById(id: Long): ReproMedication? = reproMedQueries.selectById(id).executeAsOneOrNull()?.toDomain()

    override fun insert(reproMedication: ReproMedication): Long =
        reproMedQueries
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
        reproMedQueries
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
        reproMedQueries
            .setInactive(
                id = id,
                updatedAt = updatedAt,
            ).value
}
