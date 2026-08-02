package com.github.rodrigotimoteo.animally.data.farrier

import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.data.farrier.mapper.toDomain
import com.github.rodrigotimoteo.animally.domain.farrier.IFarrierVisitRepository
import com.github.rodrigotimoteo.animally.domain.farrier.model.FarrierVisit
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single
import kotlin.time.Instant

/**
 * Repository implementation for managing [FarrierVisit] records.
 */
@Single(binds = [IFarrierVisitRepository::class])
class FarrierVisitRepositoryImpl(
    @Provided private val database: AnimallyDatabase,
) : IFarrierVisitRepository {
    private val farrierVisitQueries: FarrierVisitQueries = database.farrierVisitQueries

    override fun getByPatient(patientId: Long): List<FarrierVisit> =
        farrierVisitQueries
            .selectByPatient(patientId)
            .executeAsList()
            .map { it.toDomain() }

    override fun getById(id: Long): FarrierVisit? = farrierVisitQueries.selectById(id).executeAsOneOrNull()?.toDomain()

    override fun insert(farrierVisit: FarrierVisit): Long =
        farrierVisitQueries
            .insert(
                patientId = farrierVisit.patientId,
                date = farrierVisit.date,
                trimOrShoe = farrierVisit.trimOrShoe,
                shoeType = farrierVisit.shoeType,
                findings = farrierVisit.findings,
                nextDueDate = farrierVisit.nextDueDate,
                farrier = farrierVisit.farrier,
                notes = farrierVisit.notes,
                isActive = farrierVisit.isActive,
                createdAt = farrierVisit.createdAt,
                updatedAt = farrierVisit.updatedAt,
            ).value

    override fun update(farrierVisit: FarrierVisit): Long =
        farrierVisitQueries
            .update(
                id = farrierVisit.id,
                patientId = farrierVisit.patientId,
                date = farrierVisit.date,
                trimOrShoe = farrierVisit.trimOrShoe,
                shoeType = farrierVisit.shoeType,
                findings = farrierVisit.findings,
                nextDueDate = farrierVisit.nextDueDate,
                farrier = farrierVisit.farrier,
                notes = farrierVisit.notes,
                isActive = farrierVisit.isActive,
                updatedAt = farrierVisit.updatedAt,
            ).value

    override fun setInactive(
        id: Long,
        updatedAt: Instant,
    ): Long =
        farrierVisitQueries
            .setInactive(
                id = id,
                updatedAt = updatedAt,
            ).value
}
