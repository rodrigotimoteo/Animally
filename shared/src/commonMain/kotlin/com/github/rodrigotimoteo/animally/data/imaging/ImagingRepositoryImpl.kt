package com.github.rodrigotimoteo.animally.data.imaging

import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.data.imaging.mapper.toDomain
import com.github.rodrigotimoteo.animally.domain.imaging.IImagingRepository
import com.github.rodrigotimoteo.animally.domain.imaging.model.Imaging
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single
import kotlin.time.Instant

/**
 * Repository implementation for managing [Imaging] records.
 */
@Single(binds = [IImagingRepository::class])
class ImagingRepositoryImpl(
    @Provided private val database: AnimallyDatabase,
) : IImagingRepository {
    private val imagingQueries: ImagingQueries = database.imagingQueries

    override fun getByPatient(patientId: Long): List<Imaging> =
        imagingQueries
            .selectByPatient(patientId)
            .executeAsList()
            .map { it.toDomain() }
            .sortedByDescending { it.date }

    override fun getById(id: Long): Imaging? = imagingQueries.selectById(id).executeAsOneOrNull()?.toDomain()

    override fun insert(imaging: Imaging): Long =
        database.transactionWithResult {
            imagingQueries.insert(
                patientId = imaging.patientId,
                type = imaging.type,
                date = imaging.date,
                findings = imaging.findings,
                imageUris = imaging.imageUris,
                vetName = imaging.vetName,
                notes = imaging.notes,
                isActive = imaging.isActive,
                createdAt = imaging.createdAt,
                updatedAt = imaging.updatedAt,
            )
            database.commonQueries.selectLastRowId().executeAsOne()
        }

    override fun update(imaging: Imaging): Long =
        imagingQueries
            .update(
                id = imaging.id,
                patientId = imaging.patientId,
                type = imaging.type,
                date = imaging.date,
                findings = imaging.findings,
                imageUris = imaging.imageUris,
                vetName = imaging.vetName,
                notes = imaging.notes,
                isActive = imaging.isActive,
                updatedAt = imaging.updatedAt,
            ).value

    override fun setInactive(
        id: Long,
        updatedAt: Instant,
    ): Long =
        imagingQueries
            .setInactive(
                id = id,
                updatedAt = updatedAt,
            ).value
}
