package com.github.rodrigotimoteo.animally.data.deworming

import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.data.deworming.mapper.toDomain
import com.github.rodrigotimoteo.animally.domain.deworming.IDewormingRepository
import com.github.rodrigotimoteo.animally.domain.deworming.model.Deworming
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single
import kotlin.time.Instant

/**
 * Repository implementation for managing [Deworming] records.
 */
@Single(binds = [IDewormingRepository::class])
class DewormingRepositoryImpl(
    @Provided private val database: AnimallyDatabase,
) : IDewormingRepository {
    private val dewormingQueries: DewormingQueries = database.dewormingQueries

    override fun getByPatient(patientId: Long): List<Deworming> =
        dewormingQueries
            .selectByPatient(patientId)
            .executeAsList()
            .map { it.toDomain() }

    override fun getById(id: Long): Deworming? = dewormingQueries.selectById(id).executeAsOneOrNull()?.toDomain()

    override fun insert(deworming: Deworming): Long =
        dewormingQueries
            .insert(
                patientId = deworming.patientId,
                product = deworming.product,
                dateAdministered = deworming.dateAdministered,
                nextDueDate = deworming.nextDueDate,
                dose = deworming.dose,
                vetName = deworming.vetName,
                notes = deworming.notes,
                isActive = deworming.isActive,
                createdAt = deworming.createdAt,
                updatedAt = deworming.updatedAt,
            ).value

    override fun update(deworming: Deworming): Long =
        dewormingQueries
            .update(
                id = deworming.id,
                patientId = deworming.patientId,
                product = deworming.product,
                dateAdministered = deworming.dateAdministered,
                nextDueDate = deworming.nextDueDate,
                dose = deworming.dose,
                vetName = deworming.vetName,
                notes = deworming.notes,
                isActive = deworming.isActive,
                updatedAt = deworming.updatedAt,
            ).value

    override fun setInactive(
        id: Long,
        updatedAt: Instant,
    ): Long =
        dewormingQueries
            .setInactive(
                id = id,
                updatedAt = updatedAt,
            ).value
}
