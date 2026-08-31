package com.github.rodrigotimoteo.animally.data.farrier

import app.cash.sqldelight.Query
import app.cash.sqldelight.db.QueryResult
import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.data.common.BasePatientRepository
import com.github.rodrigotimoteo.animally.data.common.DomainMapper
import com.github.rodrigotimoteo.animally.data.farrier.mapper.toDomain
import com.github.rodrigotimoteo.animally.domain.farrier.IFarrierVisitRepository
import com.github.rodrigotimoteo.animally.domain.farrier.model.FarrierVisit
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single
import kotlin.time.Instant
import com.github.rodrigotimoteo.animally.data.migrations.FarrierVisit as DbFarrierVisit

/**
 * Repository implementation for managing [FarrierVisit] records.
 */
@Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE") // base uses `value: FarrierVisit`, interface uses `farrierVisit: FarrierVisit`
@Single(binds = [IFarrierVisitRepository::class])
class FarrierVisitRepositoryImpl(
    @Provided database: AnimallyDatabase,
) : BasePatientRepository<DbFarrierVisit, FarrierVisit>(
        database = database,
        mapper = DomainMapper { toDomain() },
    ),
    IFarrierVisitRepository {
    private val farrierVisitQueries: FarrierVisitQueries = database.farrierVisitQueries

    override fun selectByPatient(patientId: Long): Query<DbFarrierVisit> = farrierVisitQueries.selectByPatient(patientId)

    override fun selectById(id: Long): Query<DbFarrierVisit> = farrierVisitQueries.selectById(id)

    override fun doInsert(domain: FarrierVisit): QueryResult<Long> =
        farrierVisitQueries.insert(
            patientId = domain.patientId,
            date = domain.date,
            trimOrShoe = domain.trimOrShoe,
            shoeType = domain.shoeType,
            findings = domain.findings,
            nextDueDate = domain.nextDueDate,
            farrier = domain.farrier,
            notes = domain.notes,
            isActive = domain.isActive,
            createdAt = domain.createdAt,
            updatedAt = domain.updatedAt,
        )

    override fun doUpdate(domain: FarrierVisit): QueryResult<Long> =
        farrierVisitQueries.update(
            patientId = domain.patientId,
            date = domain.date,
            trimOrShoe = domain.trimOrShoe,
            shoeType = domain.shoeType,
            findings = domain.findings,
            nextDueDate = domain.nextDueDate,
            farrier = domain.farrier,
            notes = domain.notes,
            isActive = domain.isActive,
            updatedAt = domain.updatedAt,
            id = domain.id,
        )

    override fun doSetInactive(
        id: Long,
        updatedAt: Instant,
    ): QueryResult<Long> = farrierVisitQueries.setInactive(updatedAt = updatedAt, id = id)
}
