package com.github.rodrigotimoteo.animally.data.lameness

import app.cash.sqldelight.Query
import app.cash.sqldelight.db.QueryResult
import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.data.common.BasePatientRepository
import com.github.rodrigotimoteo.animally.data.common.DomainMapper
import com.github.rodrigotimoteo.animally.data.lameness.mapper.toDomain
import com.github.rodrigotimoteo.animally.domain.lameness.ILamenessRepository
import com.github.rodrigotimoteo.animally.domain.lameness.model.Lameness
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single
import kotlin.time.Instant
import com.github.rodrigotimoteo.animally.data.migrations.Lameness as DbLameness

/**
 * Repository implementation for managing [Lameness] records.
 *
 * Extends [BasePatientRepository] for shared getByPatient/getById/insert/update/setInactive wiring.
 * The explicit trampoline overrides below look redundant (they just delegate to `super`) but are required
 * to satisfy [ILamenessRepository] with its domain-named parameters (`lameness` vs base `domain`).
 */
@Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE") // base uses `domain: Lameness`, interface uses `lameness: Lameness`
@Single(binds = [ILamenessRepository::class])
class LamenessRepositoryImpl(
    @Provided database: AnimallyDatabase,
) : BasePatientRepository<DbLameness, Lameness>(
        database = database,
        mapper = DomainMapper { toDomain() },
    ),
    ILamenessRepository {
    private val lamenessQueries: LamenessQueries = database.lamenessQueries

    override fun selectByPatient(patientId: Long): Query<DbLameness> = lamenessQueries.selectByPatient(patientId)

    override fun selectById(id: Long): Query<DbLameness> = lamenessQueries.selectById(id)

    override fun doInsert(domain: Lameness): QueryResult<Long> =
        lamenessQueries.insert(
            patientId = domain.patientId,
            date = domain.date,
            gradeAAEP = domain.gradeAAEP.toLong(),
            limbLocation = domain.limbLocation,
            flexionTest = domain.flexionTest,
            diagnosis = domain.diagnosis,
            treatment = domain.treatment,
            vetName = domain.vetName,
            notes = domain.notes,
            isActive = domain.isActive,
            createdAt = domain.createdAt,
            updatedAt = domain.updatedAt,
        )

    override fun doUpdate(domain: Lameness): QueryResult<Long> =
        lamenessQueries.update(
            patientId = domain.patientId,
            date = domain.date,
            gradeAAEP = domain.gradeAAEP.toLong(),
            limbLocation = domain.limbLocation,
            flexionTest = domain.flexionTest,
            diagnosis = domain.diagnosis,
            treatment = domain.treatment,
            vetName = domain.vetName,
            notes = domain.notes,
            isActive = domain.isActive,
            updatedAt = domain.updatedAt,
            id = domain.id,
        )

    override fun doSetInactive(
        id: Long,
        updatedAt: Instant,
    ): QueryResult<Long> = lamenessQueries.setInactive(updatedAt = updatedAt, id = id)

    // --- Trampolines to BasePatientRepository ---
    // See VaccinationRepositoryImpl for rationale: required for PARAMETER_NAME_CHANGED alignment;
    // no behavior change, each delegates to super. Sorting preserved via super+sortedByDescending.

    override fun getByPatient(patientId: Long): List<Lameness> =
        super
            .getByPatient(patientId)
            .sortedByDescending { it.date }

    override fun getById(id: Long): Lameness? = super.getById(id)

    override fun insert(lameness: Lameness): Long = super.insert(lameness)

    override fun update(lameness: Lameness): Long = super.update(lameness)

    override fun setInactive(
        id: Long,
        updatedAt: Instant,
    ): Long = super.setInactive(id, updatedAt)
}
