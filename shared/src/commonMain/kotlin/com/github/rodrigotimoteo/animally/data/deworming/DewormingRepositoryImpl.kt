package com.github.rodrigotimoteo.animally.data.deworming

import app.cash.sqldelight.Query
import app.cash.sqldelight.db.QueryResult
import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.data.common.BasePatientRepository
import com.github.rodrigotimoteo.animally.data.common.DomainMapper
import com.github.rodrigotimoteo.animally.data.deworming.mapper.toDomain
import com.github.rodrigotimoteo.animally.domain.deworming.IDewormingRepository
import com.github.rodrigotimoteo.animally.domain.deworming.model.Deworming
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single
import kotlin.time.Instant
import com.github.rodrigotimoteo.animally.data.migrations.Deworming as DbDeworming

/**
 * Repository implementation for managing [Deworming] records.
 *
 * Extends [BasePatientRepository] for shared getByPatient/getById/insert/update/setInactive wiring.
 * The explicit trampoline overrides below look redundant (they just delegate to `super`) but are required
 * to satisfy [IDewormingRepository] with its domain-named parameters (`deworming` vs base `domain`).
 */
@Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE") // base uses `domain: Deworming`, interface uses `deworming: Deworming`
@Single(binds = [IDewormingRepository::class])
class DewormingRepositoryImpl(
    @Provided database: AnimallyDatabase,
) : BasePatientRepository<DbDeworming, Deworming>(
        database = database,
        mapper = DomainMapper { toDomain() },
    ),
    IDewormingRepository {
    private val dewormingQueries: DewormingQueries = database.dewormingQueries

    override fun selectByPatient(patientId: Long): Query<DbDeworming> = dewormingQueries.selectByPatient(patientId)

    override fun selectById(id: Long): Query<DbDeworming> = dewormingQueries.selectById(id)

    override fun doInsert(domain: Deworming): QueryResult<Long> =
        dewormingQueries.insert(
            patientId = domain.patientId,
            product = domain.product,
            dateAdministered = domain.dateAdministered,
            nextDueDate = domain.nextDueDate,
            dose = domain.dose,
            vetName = domain.vetName,
            notes = domain.notes,
            isActive = domain.isActive,
            createdAt = domain.createdAt,
            updatedAt = domain.updatedAt,
        )

    override fun doUpdate(domain: Deworming): QueryResult<Long> =
        dewormingQueries.update(
            patientId = domain.patientId,
            product = domain.product,
            dateAdministered = domain.dateAdministered,
            nextDueDate = domain.nextDueDate,
            dose = domain.dose,
            vetName = domain.vetName,
            notes = domain.notes,
            isActive = domain.isActive,
            updatedAt = domain.updatedAt,
            id = domain.id,
        )

    override fun doSetInactive(
        id: Long,
        updatedAt: Instant,
    ): QueryResult<Long> = dewormingQueries.setInactive(updatedAt = updatedAt, id = id)

    // --- Trampolines to BasePatientRepository ---
    // See VaccinationRepositoryImpl for rationale: required for PARAMETER_NAME_CHANGED alignment;
    // no behavior change, each delegates to super.

    override fun getByPatient(patientId: Long): List<Deworming> = super.getByPatient(patientId)

    override fun getById(id: Long): Deworming? = super.getById(id)

    override fun insert(deworming: Deworming): Long = super.insert(deworming)

    override fun update(deworming: Deworming): Long = super.update(deworming)

    override fun setInactive(
        id: Long,
        updatedAt: Instant,
    ): Long = super.setInactive(id, updatedAt)
}
