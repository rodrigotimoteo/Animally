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
 */
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

    override fun insert(lameness: Lameness): Long = super.insertDomain(lameness)

    override fun update(lameness: Lameness): Long = super.updateDomain(lameness)
}
