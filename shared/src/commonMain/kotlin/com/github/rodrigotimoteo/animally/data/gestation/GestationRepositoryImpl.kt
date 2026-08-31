package com.github.rodrigotimoteo.animally.data.gestation

import app.cash.sqldelight.Query
import app.cash.sqldelight.db.QueryResult
import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.data.common.BasePatientRepository
import com.github.rodrigotimoteo.animally.data.common.DomainMapper
import com.github.rodrigotimoteo.animally.data.gestation.mapper.toDomain
import com.github.rodrigotimoteo.animally.domain.gestation.IGestationRepository
import com.github.rodrigotimoteo.animally.domain.gestation.model.Gestation
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single
import kotlin.time.Instant
import com.github.rodrigotimoteo.animally.data.migrations.Gestation as DbGestation

/**
 * Repository implementation for managing [Gestation] records.
 */
@Single(binds = [IGestationRepository::class])
class GestationRepositoryImpl(
    @Provided database: AnimallyDatabase,
) : BasePatientRepository<DbGestation, Gestation>(
        database = database,
        mapper = DomainMapper { toDomain() },
    ),
    IGestationRepository {
    private val gestationQueries: GestationQueries = database.gestationQueries

    override fun selectByPatient(patientId: Long): Query<DbGestation> = gestationQueries.selectByPatient(patientId)

    override fun selectById(id: Long): Query<DbGestation> = gestationQueries.selectById(id)

    override fun doInsert(domain: Gestation): QueryResult<Long> =
        gestationQueries.insert(
            patientId = domain.patientId,
            breedingDate = domain.breedingDate,
            expectedDueDate = domain.expectedDueDate,
            gestationDays = domain.gestationDays.toLong(),
            status = domain.status,
            fetalCount = domain.fetalCount?.toLong(),
            lastCheckDate = domain.lastCheckDate,
            notes = domain.notes,
            isActive = domain.isActive,
            createdAt = domain.createdAt,
            updatedAt = domain.updatedAt,
        )

    override fun doUpdate(domain: Gestation): QueryResult<Long> =
        gestationQueries.update(
            patientId = domain.patientId,
            breedingDate = domain.breedingDate,
            expectedDueDate = domain.expectedDueDate,
            gestationDays = domain.gestationDays.toLong(),
            status = domain.status,
            fetalCount = domain.fetalCount?.toLong(),
            lastCheckDate = domain.lastCheckDate,
            notes = domain.notes,
            isActive = domain.isActive,
            updatedAt = domain.updatedAt,
            id = domain.id,
        )

    override fun doSetInactive(
        id: Long,
        updatedAt: Instant,
    ): QueryResult<Long> = gestationQueries.setInactive(updatedAt = updatedAt, id = id)

    override fun insert(gestation: Gestation): Long = super.insertDomain(gestation)

    override fun update(gestation: Gestation): Long = super.updateDomain(gestation)
}
