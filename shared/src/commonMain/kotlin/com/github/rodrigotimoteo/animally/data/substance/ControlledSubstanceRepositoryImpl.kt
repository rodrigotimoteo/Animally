package com.github.rodrigotimoteo.animally.data.substance

import app.cash.sqldelight.Query
import app.cash.sqldelight.db.QueryResult
import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.data.common.BasePatientRepository
import com.github.rodrigotimoteo.animally.data.common.DomainMapper
import com.github.rodrigotimoteo.animally.data.substance.mapper.toDomain
import com.github.rodrigotimoteo.animally.domain.substance.IControlledSubstanceRepository
import com.github.rodrigotimoteo.animally.domain.substance.model.ControlledSubstance
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single
import kotlin.time.Instant
import com.github.rodrigotimoteo.animally.data.migrations.Substance as DbSubstance

/**
 * Repository implementation for managing [ControlledSubstance] records.
 *
 * Extends [BasePatientRepository] for shared getByPatient/getById/insert/update/setInactive wiring.
 * The explicit trampoline overrides below look redundant (they just delegate to `super`) but are required
 * to satisfy [IControlledSubstanceRepository] with its domain-named parameters (`controlledSubstance` vs base `domain`).
 */
@Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE") // base uses `domain: ControlledSubstance`, interface uses `controlledSubstance: ControlledSubstance`
@Single(binds = [IControlledSubstanceRepository::class])
class ControlledSubstanceRepositoryImpl(
    @Provided database: AnimallyDatabase,
) : BasePatientRepository<DbSubstance, ControlledSubstance>(
        database = database,
        mapper = DomainMapper { toDomain() },
    ),
    IControlledSubstanceRepository {
    private val substanceQueries: SubstanceQueries = database.substanceQueries

    override fun selectByPatient(patientId: Long): Query<DbSubstance> = substanceQueries.selectByPatient(patientId)

    override fun selectById(id: Long): Query<DbSubstance> = substanceQueries.selectById(id)

    override fun doInsert(domain: ControlledSubstance): QueryResult<Long> =
        substanceQueries.insert(
            patientId = domain.patientId,
            drugName = domain.drugName,
            dose = domain.dose,
            unit = domain.unit,
            route = domain.route,
            administeredBy = domain.administeredBy,
            witness = domain.witness,
            date = domain.date,
            reason = domain.reason,
            notes = domain.notes,
            isActive = domain.isActive,
            createdAt = domain.createdAt,
            updatedAt = domain.updatedAt,
        )

    override fun doUpdate(domain: ControlledSubstance): QueryResult<Long> =
        substanceQueries.update(
            patientId = domain.patientId,
            drugName = domain.drugName,
            dose = domain.dose,
            unit = domain.unit,
            route = domain.route,
            administeredBy = domain.administeredBy,
            witness = domain.witness,
            date = domain.date,
            reason = domain.reason,
            notes = domain.notes,
            isActive = domain.isActive,
            updatedAt = domain.updatedAt,
            id = domain.id,
        )

    override fun doSetInactive(
        id: Long,
        updatedAt: Instant,
    ): QueryResult<Long> = substanceQueries.setInactive(updatedAt = updatedAt, id = id)

    // --- Trampolines to BasePatientRepository ---
    // See VaccinationRepositoryImpl for rationale: required for PARAMETER_NAME_CHANGED alignment;
    // no behavior change, each delegates to super. Sorting preserved via super+sortedByDescending.

    override fun getByPatient(patientId: Long): List<ControlledSubstance> = super.getByPatient(patientId).sortedByDescending { it.date }

    override fun getById(id: Long): ControlledSubstance? = super.getById(id)

    override fun insert(controlledSubstance: ControlledSubstance): Long = super.insert(controlledSubstance)

    override fun update(controlledSubstance: ControlledSubstance): Long = super.update(controlledSubstance)

    override fun setInactive(
        id: Long,
        updatedAt: Instant,
    ): Long = super.setInactive(id, updatedAt)
}
