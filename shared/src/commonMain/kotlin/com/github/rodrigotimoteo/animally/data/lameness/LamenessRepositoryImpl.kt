package com.github.rodrigotimoteo.animally.data.lameness

import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.data.lameness.mapper.toDomain
import com.github.rodrigotimoteo.animally.domain.lameness.ILamenessRepository
import com.github.rodrigotimoteo.animally.domain.lameness.model.Lameness
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single
import kotlin.time.Instant

/**
 * Repository implementation for managing [Lameness] records.
 */
@Single(binds = [ILamenessRepository::class])
class LamenessRepositoryImpl(
    @Provided private val database: AnimallyDatabase,
) : ILamenessRepository {
    private val lamenessQueries: LamenessQueries = database.lamenessQueries

    override fun getByPatient(patientId: Long): List<Lameness> =
        lamenessQueries
            .selectByPatient(patientId)
            .executeAsList()
            .map { it.toDomain() }
            .sortedByDescending { it.date }

    override fun getById(id: Long): Lameness? = lamenessQueries.selectById(id).executeAsOneOrNull()?.toDomain()

    override fun insert(lameness: Lameness): Long =
        lamenessQueries
            .insert(
                patientId = lameness.patientId,
                date = lameness.date,
                gradeAAEP = lameness.gradeAAEP.toLong(),
                limbLocation = lameness.limbLocation,
                flexionTest = lameness.flexionTest,
                diagnosis = lameness.diagnosis,
                treatment = lameness.treatment,
                vetName = lameness.vetName,
                notes = lameness.notes,
                isActive = lameness.isActive,
                createdAt = lameness.createdAt,
                updatedAt = lameness.updatedAt,
            ).value

    override fun update(lameness: Lameness): Long =
        lamenessQueries
            .update(
                id = lameness.id,
                patientId = lameness.patientId,
                date = lameness.date,
                gradeAAEP = lameness.gradeAAEP.toLong(),
                limbLocation = lameness.limbLocation,
                flexionTest = lameness.flexionTest,
                diagnosis = lameness.diagnosis,
                treatment = lameness.treatment,
                vetName = lameness.vetName,
                notes = lameness.notes,
                isActive = lameness.isActive,
                updatedAt = lameness.updatedAt,
            ).value

    override fun setInactive(
        id: Long,
        updatedAt: Instant,
    ): Long =
        lamenessQueries
            .setInactive(
                id = id,
                updatedAt = updatedAt,
            ).value
}
