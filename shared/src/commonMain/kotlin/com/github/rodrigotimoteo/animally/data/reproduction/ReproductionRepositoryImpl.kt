package com.github.rodrigotimoteo.animally.data.reproduction

import app.cash.sqldelight.Query
import app.cash.sqldelight.db.QueryResult
import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.data.common.BasePatientRepository
import com.github.rodrigotimoteo.animally.data.common.DomainMapper
import com.github.rodrigotimoteo.animally.data.reproduction.mapper.toDomain
import com.github.rodrigotimoteo.animally.domain.reproduction.IReproductionRepository
import com.github.rodrigotimoteo.animally.domain.reproduction.model.ReproductionEvent
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single
import kotlin.time.Instant
import com.github.rodrigotimoteo.animally.data.migrations.Reproduction as DbReproduction

/**
 * Repository implementation for managing [ReproductionEvent] records.
 *
 * Extends [BasePatientRepository] for shared getByPatient/getById/insert/update/setInactive wiring.
 * The explicit trampoline overrides below look redundant (they just delegate to `super`) but are required
 * to satisfy [IReproductionRepository] with its domain-named parameters (`reproductionEvent` vs base `domain`).
 */
@Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE") // base uses `domain: ReproductionEvent`, interface uses `reproductionEvent: ReproductionEvent`
@Single(binds = [IReproductionRepository::class])
class ReproductionRepositoryImpl(
    @Provided database: AnimallyDatabase,
) : BasePatientRepository<DbReproduction, ReproductionEvent>(
        database = database,
        mapper = DomainMapper { toDomain() },
    ),
    IReproductionRepository {
    private val reproQueries: ReproductionQueries = database.reproductionQueries

    override fun selectByPatient(patientId: Long): Query<DbReproduction> = reproQueries.selectByPatient(patientId)

    override fun selectById(id: Long): Query<DbReproduction> = reproQueries.selectById(id)

    override fun doInsert(domain: ReproductionEvent): QueryResult<Long> =
        reproQueries.insert(
            patientId = domain.patientId,
            eventType = domain.eventType,
            date = domain.date,
            details = domain.details,
            initialExamFindings = domain.initialExamFindings,
            stallionName = domain.stallionName,
            breedingType = domain.breedingType,
            vetName = domain.vetName,
            notes = domain.notes,
            isActive = domain.isActive,
            createdAt = domain.createdAt,
            updatedAt = domain.updatedAt,
        )

    override fun doUpdate(domain: ReproductionEvent): QueryResult<Long> =
        reproQueries.update(
            patientId = domain.patientId,
            eventType = domain.eventType,
            date = domain.date,
            details = domain.details,
            initialExamFindings = domain.initialExamFindings,
            stallionName = domain.stallionName,
            breedingType = domain.breedingType,
            vetName = domain.vetName,
            notes = domain.notes,
            isActive = domain.isActive,
            updatedAt = domain.updatedAt,
            id = domain.id,
        )

    override fun doSetInactive(
        id: Long,
        updatedAt: Instant,
    ): QueryResult<Long> = reproQueries.setInactive(updatedAt = updatedAt, id = id)

    // --- Trampolines to BasePatientRepository ---
    // See VaccinationRepositoryImpl for rationale: required for PARAMETER_NAME_CHANGED alignment;
    // no behavior change, each delegates to super. Sorting preserved via super+sortedByDescending.

    override fun getByPatient(patientId: Long): List<ReproductionEvent> = super.getByPatient(patientId).sortedByDescending { it.date }

    override fun getById(id: Long): ReproductionEvent? = super.getById(id)

    override fun insert(reproductionEvent: ReproductionEvent): Long = super.insert(reproductionEvent)

    override fun update(reproductionEvent: ReproductionEvent): Long = super.update(reproductionEvent)

    override fun setInactive(
        id: Long,
        updatedAt: Instant,
    ): Long = super.setInactive(id, updatedAt)
}
