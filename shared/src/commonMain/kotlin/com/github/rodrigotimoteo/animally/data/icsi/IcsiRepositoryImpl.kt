package com.github.rodrigotimoteo.animally.data.icsi

import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.data.icsi.mapper.toDomain
import com.github.rodrigotimoteo.animally.domain.icsi.IIcsiRepository
import com.github.rodrigotimoteo.animally.domain.icsi.model.Icsi
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single
import kotlin.time.Instant

/**
 * Repository implementation for managing [Icsi] records.
 */
@Single(binds = [IIcsiRepository::class])
class IcsiRepositoryImpl(
    @Provided private val database: AnimallyDatabase,
) : IIcsiRepository {
    private val queries = database.icsiQueries

    override fun getByPatient(patientId: Long): List<Icsi> =
        queries
            .selectByPatient(patientId)
            .executeAsList()
            .map { it.toDomain() }
            .sortedByDescending { it.date }

    override fun getById(id: Long): Icsi? = queries.selectById(id).executeAsOneOrNull()?.toDomain()

    override fun insert(record: Icsi): Long =
        queries
            .insert(
                patientId = record.patientId,
                date = record.date,
                folliclesRecovered = record.folliclesRecovered.toLong(),
                vetName = record.vetName,
                notes = record.notes,
                isActive = record.isActive,
                createdAt = record.createdAt,
                updatedAt = record.updatedAt,
            ).value

    override fun update(record: Icsi): Long =
        queries
            .update(
                patientId = record.patientId,
                date = record.date,
                folliclesRecovered = record.folliclesRecovered.toLong(),
                vetName = record.vetName,
                notes = record.notes,
                isActive = record.isActive,
                updatedAt = record.updatedAt,
                id = record.id,
            ).value

    override fun setInactive(
        id: Long,
        updatedAt: Instant,
    ): Long =
        queries
            .setInactive(
                id = id,
                updatedAt = updatedAt,
            ).value
}
