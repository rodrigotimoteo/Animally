package com.github.rodrigotimoteo.animally.data.weight

import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.data.weight.mapper.toDomain
import com.github.rodrigotimoteo.animally.domain.weight.IWeightRepository
import com.github.rodrigotimoteo.animally.domain.weight.model.Weight
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single
import kotlin.time.Instant

/**
 * Repository implementation for managing [Weight] records.
 */
@Single(binds = [IWeightRepository::class])
class WeightRepositoryImpl(
    @Provided private val database: AnimallyDatabase,
) : IWeightRepository {
    private val weightQueries: WeightQueries = database.weightQueries

    override fun getByPatient(patientId: Long): List<Weight> =
        weightQueries
            .selectByPatient(patientId)
            .executeAsList()
            .map { it.toDomain() }
            .sortedByDescending { it.date }

    override fun getById(id: Long): Weight? = weightQueries.selectById(id).executeAsOneOrNull()?.toDomain()

    override fun insert(weight: Weight): Long =
        database.transactionWithResult {
            weightQueries.insert(
                patientId = weight.patientId,
                weightKg = weight.weightKg,
                date = weight.date,
                notes = weight.notes,
                isActive = weight.isActive,
                createdAt = weight.createdAt,
                updatedAt = weight.updatedAt,
            )
            database.commonQueries.selectLastRowId().executeAsOne()
        }

    override fun update(weight: Weight): Long =
        weightQueries
            .update(
                id = weight.id,
                patientId = weight.patientId,
                weightKg = weight.weightKg,
                date = weight.date,
                notes = weight.notes,
                isActive = weight.isActive,
                updatedAt = weight.updatedAt,
            ).value

    override fun setInactive(
        id: Long,
        updatedAt: Instant,
    ): Long =
        weightQueries
            .setInactive(
                id = id,
                updatedAt = updatedAt,
            ).value
}
