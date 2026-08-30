package com.github.rodrigotimoteo.animally.data.dentistry

import app.cash.sqldelight.Query
import app.cash.sqldelight.db.QueryResult
import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.data.common.BasePatientRepository
import com.github.rodrigotimoteo.animally.data.common.DomainMapper
import com.github.rodrigotimoteo.animally.data.dentistry.mapper.toDomain
import com.github.rodrigotimoteo.animally.domain.dentistry.IDentistryRepository
import com.github.rodrigotimoteo.animally.domain.dentistry.model.Dentistry
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single
import kotlin.time.Instant
import com.github.rodrigotimoteo.animally.data.migrations.Dentistry as DbDentistry

/**
 * Repository implementation for managing [Dentistry] records.
 *
 * Extends [BasePatientRepository] for shared getByPatient/getById/insert/update/setInactive wiring.
 * The explicit trampoline overrides below look redundant (they just delegate to `super`) but are required
 * to satisfy [IDentistryRepository] with its domain-named parameters (`dentistry` vs base `domain`).
 */
@Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE") // base uses `domain: Dentistry`, interface uses `dentistry: Dentistry`
@Single(binds = [IDentistryRepository::class])
class DentistryRepositoryImpl(
    @Provided database: AnimallyDatabase,
) : BasePatientRepository<DbDentistry, Dentistry>(
        database = database,
        mapper = DomainMapper { toDomain() },
    ),
    IDentistryRepository {
    private val dentistryQueries: DentistryQueries = database.dentistryQueries

    override fun selectByPatient(patientId: Long): Query<DbDentistry> = dentistryQueries.selectByPatient(patientId)

    override fun selectById(id: Long): Query<DbDentistry> = dentistryQueries.selectById(id)

    override fun doInsert(domain: Dentistry): QueryResult<Long> =
        dentistryQueries.insert(
            patientId = domain.patientId,
            date = domain.date,
            findings = domain.findings,
            treatment = domain.treatment,
            nextDueDate = domain.nextDueDate,
            vetName = domain.vetName,
            notes = domain.notes,
            isActive = domain.isActive,
            createdAt = domain.createdAt,
            updatedAt = domain.updatedAt,
        )

    override fun doUpdate(domain: Dentistry): QueryResult<Long> =
        dentistryQueries.update(
            patientId = domain.patientId,
            date = domain.date,
            findings = domain.findings,
            treatment = domain.treatment,
            nextDueDate = domain.nextDueDate,
            vetName = domain.vetName,
            notes = domain.notes,
            isActive = domain.isActive,
            updatedAt = domain.updatedAt,
            id = domain.id,
        )

    override fun doSetInactive(
        id: Long,
        updatedAt: Instant,
    ): QueryResult<Long> = dentistryQueries.setInactive(updatedAt = updatedAt, id = id)

    // --- Trampolines to BasePatientRepository ---
    // See VaccinationRepositoryImpl for rationale: required for PARAMETER_NAME_CHANGED alignment;
    // no behavior change, each delegates to super.

    override fun getByPatient(patientId: Long): List<Dentistry> = super.getByPatient(patientId)

    override fun getById(id: Long): Dentistry? = super.getById(id)

    override fun insert(dentistry: Dentistry): Long = super.insert(dentistry)

    override fun update(dentistry: Dentistry): Long = super.update(dentistry)

    override fun setInactive(
        id: Long,
        updatedAt: Instant,
    ): Long = super.setInactive(id, updatedAt)
}
