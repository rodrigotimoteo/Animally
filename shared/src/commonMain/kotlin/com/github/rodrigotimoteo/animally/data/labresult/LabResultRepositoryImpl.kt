package com.github.rodrigotimoteo.animally.data.labresult

import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.data.labresult.mapper.toDomain
import com.github.rodrigotimoteo.animally.domain.labresult.ILabResultRepository
import com.github.rodrigotimoteo.animally.domain.labresult.model.LabResult
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single
import kotlin.time.Instant

/**
 * Repository implementation for managing [LabResult] records.
 */
@Single(binds = [ILabResultRepository::class])
class LabResultRepositoryImpl(
    @Provided private val database: AnimallyDatabase,
) : ILabResultRepository {
    private val labResultQueries: LabResultQueries = database.labResultQueries

    override fun getByPatient(patientId: Long): List<LabResult> =
        labResultQueries
            .selectByPatient(patientId)
            .executeAsList()
            .map { it.toDomain() }
            .sortedByDescending { it.date }

    override fun getById(id: Long): LabResult? = labResultQueries.selectById(id).executeAsOneOrNull()?.toDomain()

    override fun insert(labResult: LabResult): Long =
        labResultQueries
            .insert(
                patientId = labResult.patientId,
                testType = labResult.testType,
                date = labResult.date,
                results = labResult.results,
                normalRange = labResult.normalRange,
                vetName = labResult.vetName,
                notes = labResult.notes,
                isActive = labResult.isActive,
                createdAt = labResult.createdAt,
                updatedAt = labResult.updatedAt,
            ).value

    override fun update(labResult: LabResult): Long =
        labResultQueries
            .update(
                id = labResult.id,
                patientId = labResult.patientId,
                testType = labResult.testType,
                date = labResult.date,
                results = labResult.results,
                normalRange = labResult.normalRange,
                vetName = labResult.vetName,
                notes = labResult.notes,
                isActive = labResult.isActive,
                updatedAt = labResult.updatedAt,
            ).value

    override fun setInactive(
        id: Long,
        updatedAt: Instant,
    ): Long =
        labResultQueries
            .setInactive(
                id = id,
                updatedAt = updatedAt,
            ).value
}
