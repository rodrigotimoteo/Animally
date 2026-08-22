package com.github.rodrigotimoteo.animally.data.follicle

import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.data.follicle.mapper.toDomain
import com.github.rodrigotimoteo.animally.domain.follicle.IFollicleRepository
import com.github.rodrigotimoteo.animally.domain.follicle.model.Follicle
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single
import kotlin.time.Instant

/**
 * Repository implementation for managing [Follicle] records.
 */
@Single(binds = [IFollicleRepository::class])
class FollicleRepositoryImpl(
    @Provided private val database: AnimallyDatabase,
) : IFollicleRepository {
    private val queries = database.follicleQueries

    override fun getByUltrasound(ultrasoundId: Long): List<Follicle> =
        queries
            .selectByUltrasound(ultrasoundId)
            .executeAsList()
            .map { it.toDomain() }
            .sortedWith(compareBy({ it.side }, { it.sizeMm }))

    override fun getById(id: Long): Follicle? = queries.selectById(id).executeAsOneOrNull()?.toDomain()

    override fun insert(follicle: Follicle): Long =
        queries
            .insert(
                ultrasoundId = follicle.ultrasoundId,
                side = follicle.side,
                sizeMm = follicle.sizeMm,
                description = follicle.description,
                isActive = follicle.isActive,
                createdAt = follicle.createdAt,
                updatedAt = follicle.updatedAt,
            ).value

    override fun update(follicle: Follicle): Long =
        queries
            .update(
                ultrasoundId = follicle.ultrasoundId,
                side = follicle.side,
                sizeMm = follicle.sizeMm,
                description = follicle.description,
                isActive = follicle.isActive,
                updatedAt = follicle.updatedAt,
                id = follicle.id,
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

    override fun setInactiveForUltrasound(
        ultrasoundId: Long,
        updatedAt: Instant,
    ) {
        getByUltrasound(ultrasoundId).forEach { setInactive(it.id, updatedAt) }
    }
}
