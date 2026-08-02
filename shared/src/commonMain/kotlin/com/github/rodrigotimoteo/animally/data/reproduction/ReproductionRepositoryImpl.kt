package com.github.rodrigotimoteo.animally.data.reproduction

import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.data.reproduction.mapper.toDomain
import com.github.rodrigotimoteo.animally.domain.reproduction.IReproductionRepository
import com.github.rodrigotimoteo.animally.domain.reproduction.model.ReproductionEvent
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single
import kotlin.time.Instant

/**
 * Repository implementation for managing [ReproductionEvent] records.
 */
@Single(binds = [IReproductionRepository::class])
class ReproductionRepositoryImpl(
    @Provided private val database: AnimallyDatabase,
) : IReproductionRepository {
    private val reproductionQueries: ReproductionQueries = database.reproductionQueries

    override fun getByPatient(patientId: Long): List<ReproductionEvent> =
        reproductionQueries
            .selectByPatient(patientId)
            .executeAsList()
            .map { it.toDomain() }
            .sortedByDescending { it.date }

    @Suppress("MaxLineLength")
    override fun getById(id: Long): ReproductionEvent? = reproductionQueries.selectById(id).executeAsOneOrNull()?.toDomain()

    override fun insert(reproductionEvent: ReproductionEvent): Long =
        reproductionQueries
            .insert(
                patientId = reproductionEvent.patientId,
                eventType = reproductionEvent.eventType,
                date = reproductionEvent.date,
                details = reproductionEvent.details,
                vetName = reproductionEvent.vetName,
                notes = reproductionEvent.notes,
                isActive = reproductionEvent.isActive,
                createdAt = reproductionEvent.createdAt,
                updatedAt = reproductionEvent.updatedAt,
            ).value

    override fun update(reproductionEvent: ReproductionEvent): Long =
        reproductionQueries
            .update(
                id = reproductionEvent.id,
                patientId = reproductionEvent.patientId,
                eventType = reproductionEvent.eventType,
                date = reproductionEvent.date,
                details = reproductionEvent.details,
                vetName = reproductionEvent.vetName,
                notes = reproductionEvent.notes,
                isActive = reproductionEvent.isActive,
                updatedAt = reproductionEvent.updatedAt,
            ).value

    override fun setInactive(
        id: Long,
        updatedAt: Instant,
    ): Long =
        reproductionQueries
            .setInactive(
                id = id,
                updatedAt = updatedAt,
            ).value
}
