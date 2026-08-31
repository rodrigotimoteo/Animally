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
 */
@Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE") // base uses `value: Dentistry`, interface uses `dentistry: Dentistry`
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
}
