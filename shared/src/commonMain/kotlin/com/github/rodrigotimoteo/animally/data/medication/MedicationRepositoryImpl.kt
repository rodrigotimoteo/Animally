package com.github.rodrigotimoteo.animally.data.medication

import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.data.medication.mapper.toDomain
import com.github.rodrigotimoteo.animally.domain.medication.IMedicationRepository
import com.github.rodrigotimoteo.animally.domain.medication.model.Medication
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single
import kotlin.time.Instant

/**
 * Repository implementation for managing [Medication] records.
 */
@Single(binds = [IMedicationRepository::class])
class MedicationRepositoryImpl(
    @Provided private val database: AnimallyDatabase,
) : IMedicationRepository {
    private val medicationQueries: MedicationQueries = database.medicationQueries

    override fun getByPatient(patientId: Long): List<Medication> =
        medicationQueries
            .selectByPatient(patientId)
            .executeAsList()
            .map { it.toDomain() }
            .sortedByDescending { it.startDate }

    override fun getById(id: Long): Medication? = medicationQueries.selectById(id).executeAsOneOrNull()?.toDomain()

    override fun insert(medication: Medication): Long =
        medicationQueries
            .insert(
                patientId = medication.patientId,
                name = medication.name,
                dosage = medication.dosage,
                route = medication.route,
                frequency = medication.frequency,
                startDate = medication.startDate,
                endDate = medication.endDate,
                prescribedBy = medication.prescribedBy,
                notes = medication.notes,
                isActive = medication.isActive,
                createdAt = medication.createdAt,
                updatedAt = medication.updatedAt,
            ).value

    override fun update(medication: Medication): Long =
        medicationQueries
            .update(
                id = medication.id,
                patientId = medication.patientId,
                name = medication.name,
                dosage = medication.dosage,
                route = medication.route,
                frequency = medication.frequency,
                startDate = medication.startDate,
                endDate = medication.endDate,
                prescribedBy = medication.prescribedBy,
                notes = medication.notes,
                isActive = medication.isActive,
                updatedAt = medication.updatedAt,
            ).value

    override fun setInactive(
        id: Long,
        updatedAt: Instant,
    ): Long =
        medicationQueries
            .setInactive(
                id = id,
                updatedAt = updatedAt,
            ).value
}
