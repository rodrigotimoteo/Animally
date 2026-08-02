package com.github.rodrigotimoteo.animally.data.substance

import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.data.substance.mapper.toDomain
import com.github.rodrigotimoteo.animally.domain.substance.IControlledSubstanceRepository
import com.github.rodrigotimoteo.animally.domain.substance.model.ControlledSubstance
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single
import kotlin.time.Instant

/**
 * Repository implementation for managing [ControlledSubstance] records.
 */
@Single(binds = [IControlledSubstanceRepository::class])
class ControlledSubstanceRepositoryImpl(
    @Provided private val database: AnimallyDatabase,
) : IControlledSubstanceRepository {
    private val subQueries: SubstanceQueries = database.substanceQueries

    override fun getByPatient(patientId: Long): List<ControlledSubstance> =
        subQueries
            .selectByPatient(patientId)
            .executeAsList()
            .map { it.toDomain() }
            .sortedByDescending { it.date }

    override fun getById(id: Long): ControlledSubstance? = subQueries.selectById(id).executeAsOneOrNull()?.toDomain()

    override fun insert(controlledSubstance: ControlledSubstance): Long =
        subQueries
            .insert(
                patientId = controlledSubstance.patientId,
                drugName = controlledSubstance.drugName,
                dose = controlledSubstance.dose,
                unit = controlledSubstance.unit,
                route = controlledSubstance.route,
                administeredBy = controlledSubstance.administeredBy,
                witness = controlledSubstance.witness,
                date = controlledSubstance.date,
                reason = controlledSubstance.reason,
                notes = controlledSubstance.notes,
                isActive = controlledSubstance.isActive,
                createdAt = controlledSubstance.createdAt,
                updatedAt = controlledSubstance.updatedAt,
            ).value

    override fun update(controlledSubstance: ControlledSubstance): Long =
        subQueries
            .update(
                id = controlledSubstance.id,
                patientId = controlledSubstance.patientId,
                drugName = controlledSubstance.drugName,
                dose = controlledSubstance.dose,
                unit = controlledSubstance.unit,
                route = controlledSubstance.route,
                administeredBy = controlledSubstance.administeredBy,
                witness = controlledSubstance.witness,
                date = controlledSubstance.date,
                reason = controlledSubstance.reason,
                notes = controlledSubstance.notes,
                isActive = controlledSubstance.isActive,
                updatedAt = controlledSubstance.updatedAt,
            ).value

    override fun setInactive(
        id: Long,
        updatedAt: Instant,
    ): Long =
        subQueries
            .setInactive(
                id = id,
                updatedAt = updatedAt,
            ).value
}
