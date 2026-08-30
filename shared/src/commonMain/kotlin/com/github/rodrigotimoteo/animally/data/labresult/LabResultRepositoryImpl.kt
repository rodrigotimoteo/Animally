package com.github.rodrigotimoteo.animally.data.labresult

import app.cash.sqldelight.Query
import app.cash.sqldelight.db.QueryResult
import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.data.common.BasePatientRepository
import com.github.rodrigotimoteo.animally.data.common.DomainMapper
import com.github.rodrigotimoteo.animally.data.labresult.mapper.toDomain
import com.github.rodrigotimoteo.animally.domain.labresult.ILabResultRepository
import com.github.rodrigotimoteo.animally.domain.labresult.model.LabResult
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single
import kotlin.time.Instant
import com.github.rodrigotimoteo.animally.data.migrations.LabResult as DbLabResult

/**
 * Repository implementation for managing [LabResult] records.
 *
 * Extends [BasePatientRepository] for shared getByPatient/getById/insert/update/setInactive wiring.
 * The explicit trampoline overrides below look redundant (they just delegate to `super`) but are required
 * to satisfy [ILabResultRepository] with its domain-named parameters (`labResult` vs base `domain`).
 */
@Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE") // base uses `domain: LabResult`, interface uses `labResult: LabResult`
@Single(binds = [ILabResultRepository::class])
class LabResultRepositoryImpl(
    @Provided database: AnimallyDatabase,
) : BasePatientRepository<DbLabResult, LabResult>(
        database = database,
        mapper = DomainMapper { toDomain() },
    ),
    ILabResultRepository {
    private val labResultQueries: LabResultQueries = database.labResultQueries

    override fun selectByPatient(patientId: Long): Query<DbLabResult> = labResultQueries.selectByPatient(patientId)

    override fun selectById(id: Long): Query<DbLabResult> = labResultQueries.selectById(id)

    override fun doInsert(domain: LabResult): QueryResult<Long> =
        labResultQueries.insert(
            patientId = domain.patientId,
            testType = domain.testType,
            date = domain.date,
            results = domain.results,
            normalRange = domain.normalRange,
            vetName = domain.vetName,
            notes = domain.notes,
            isActive = domain.isActive,
            createdAt = domain.createdAt,
            updatedAt = domain.updatedAt,
        )

    override fun doUpdate(domain: LabResult): QueryResult<Long> =
        labResultQueries.update(
            patientId = domain.patientId,
            testType = domain.testType,
            date = domain.date,
            results = domain.results,
            normalRange = domain.normalRange,
            vetName = domain.vetName,
            notes = domain.notes,
            isActive = domain.isActive,
            updatedAt = domain.updatedAt,
            id = domain.id,
        )

    override fun doSetInactive(
        id: Long,
        updatedAt: Instant,
    ): QueryResult<Long> = labResultQueries.setInactive(updatedAt = updatedAt, id = id)

    // --- Trampolines to BasePatientRepository ---
    // See VaccinationRepositoryImpl for rationale: required for PARAMETER_NAME_CHANGED alignment;
    // no behavior change, each delegates to super. Sorting preserved via super+sortedByDescending.

    override fun getByPatient(patientId: Long): List<LabResult> =
        super
            .getByPatient(patientId)
            .sortedByDescending { it.date }

    override fun getById(id: Long): LabResult? = super.getById(id)

    override fun insert(labResult: LabResult): Long = super.insert(labResult)

    override fun update(labResult: LabResult): Long = super.update(labResult)

    override fun setInactive(
        id: Long,
        updatedAt: Instant,
    ): Long = super.setInactive(id, updatedAt)
}
