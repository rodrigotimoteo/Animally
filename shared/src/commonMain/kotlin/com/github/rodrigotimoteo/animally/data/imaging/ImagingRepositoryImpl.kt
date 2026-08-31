package com.github.rodrigotimoteo.animally.data.imaging

import app.cash.sqldelight.Query
import app.cash.sqldelight.db.QueryResult
import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.data.common.BasePatientRepository
import com.github.rodrigotimoteo.animally.data.common.DomainMapper
import com.github.rodrigotimoteo.animally.data.imaging.mapper.toDomain
import com.github.rodrigotimoteo.animally.domain.imaging.IImagingRepository
import com.github.rodrigotimoteo.animally.domain.imaging.model.Imaging
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single
import kotlin.time.Instant
import com.github.rodrigotimoteo.animally.data.migrations.Imaging as DbImaging

/**
 * Repository implementation for managing [Imaging] records.
 */
@Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE") // base uses `value: Imaging`, interface uses `imaging: Imaging`
@Single(binds = [IImagingRepository::class])
class ImagingRepositoryImpl(
    @Provided database: AnimallyDatabase,
) : BasePatientRepository<DbImaging, Imaging>(
        database = database,
        mapper = DomainMapper { toDomain() },
    ),
    IImagingRepository {
    private val imagingQueries: ImagingQueries = database.imagingQueries

    override fun selectByPatient(patientId: Long): Query<DbImaging> = imagingQueries.selectByPatient(patientId)

    override fun selectById(id: Long): Query<DbImaging> = imagingQueries.selectById(id)

    override fun doInsert(domain: Imaging): QueryResult<Long> =
        imagingQueries.insert(
            patientId = domain.patientId,
            type = domain.type,
            date = domain.date,
            findings = domain.findings,
            imageUris = domain.imageUris,
            vetName = domain.vetName,
            notes = domain.notes,
            isActive = domain.isActive,
            createdAt = domain.createdAt,
            updatedAt = domain.updatedAt,
        )

    override fun doUpdate(domain: Imaging): QueryResult<Long> =
        imagingQueries.update(
            patientId = domain.patientId,
            type = domain.type,
            date = domain.date,
            findings = domain.findings,
            imageUris = domain.imageUris,
            vetName = domain.vetName,
            notes = domain.notes,
            isActive = domain.isActive,
            updatedAt = domain.updatedAt,
            id = domain.id,
        )

    override fun doSetInactive(
        id: Long,
        updatedAt: Instant,
    ): QueryResult<Long> = imagingQueries.setInactive(updatedAt = updatedAt, id = id)
}
