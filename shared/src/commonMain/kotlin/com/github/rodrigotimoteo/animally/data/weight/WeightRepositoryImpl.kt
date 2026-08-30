package com.github.rodrigotimoteo.animally.data.weight

import app.cash.sqldelight.Query
import app.cash.sqldelight.db.QueryResult
import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.data.common.BasePatientRepository
import com.github.rodrigotimoteo.animally.data.common.DomainMapper
import com.github.rodrigotimoteo.animally.data.weight.mapper.toDomain
import com.github.rodrigotimoteo.animally.domain.weight.IWeightRepository
import com.github.rodrigotimoteo.animally.domain.weight.model.Weight
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single
import kotlin.time.Instant
import com.github.rodrigotimoteo.animally.data.migrations.Weight as DbWeight

/**
 * Repository implementation for managing [Weight] records.
 *
 * Extends [BasePatientRepository] for shared getByPatient/getById/insert/update/setInactive wiring.
 * The explicit trampoline overrides below look redundant (they just delegate to `super`) but are required
 * to satisfy [IWeightRepository] with its domain-named parameters (`weight` vs base `domain`).
 */
@Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE") // base uses `domain: Weight`, interface uses `weight: Weight`
@Single(binds = [IWeightRepository::class])
class WeightRepositoryImpl(
    @Provided database: AnimallyDatabase,
) : BasePatientRepository<DbWeight, Weight>(
        database = database,
        mapper = DomainMapper { toDomain() },
    ),
    IWeightRepository {
    private val weightQueries: WeightQueries = database.weightQueries

    override fun selectByPatient(patientId: Long): Query<DbWeight> = weightQueries.selectByPatient(patientId)

    override fun selectById(id: Long): Query<DbWeight> = weightQueries.selectById(id)

    override fun doInsert(domain: Weight): QueryResult<Long> =
        weightQueries.insert(
            patientId = domain.patientId,
            weightKg = domain.weightKg,
            date = domain.date,
            notes = domain.notes,
            isActive = domain.isActive,
            createdAt = domain.createdAt,
            updatedAt = domain.updatedAt,
        )

    override fun doUpdate(domain: Weight): QueryResult<Long> =
        weightQueries.update(
            patientId = domain.patientId,
            weightKg = domain.weightKg,
            date = domain.date,
            notes = domain.notes,
            isActive = domain.isActive,
            updatedAt = domain.updatedAt,
            id = domain.id,
        )

    override fun doSetInactive(
        id: Long,
        updatedAt: Instant,
    ): QueryResult<Long> = weightQueries.setInactive(updatedAt = updatedAt, id = id)

    // --- Trampolines to BasePatientRepository ---
    // See VaccinationRepositoryImpl for rationale: required for PARAMETER_NAME_CHANGED alignment;
    // no behavior change, each delegates to super. Sorting preserved via super+sortedByDescending.

    override fun getByPatient(patientId: Long): List<Weight> = super.getByPatient(patientId).sortedByDescending { it.date }

    override fun getById(id: Long): Weight? = super.getById(id)

    override fun insert(weight: Weight): Long = super.insert(weight)

    override fun update(weight: Weight): Long = super.update(weight)

    override fun setInactive(
        id: Long,
        updatedAt: Instant,
    ): Long = super.setInactive(id, updatedAt)
}
