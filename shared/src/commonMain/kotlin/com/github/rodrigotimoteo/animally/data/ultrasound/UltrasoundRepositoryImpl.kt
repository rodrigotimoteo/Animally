package com.github.rodrigotimoteo.animally.data.ultrasound

import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.data.ultrasound.mapper.toDomain
import com.github.rodrigotimoteo.animally.domain.ultrasound.IUltrasoundRepository
import com.github.rodrigotimoteo.animally.domain.ultrasound.model.Ultrasound
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single
import kotlin.time.Instant

/**
 * Repository implementation for managing [Ultrasound] records.
 */
@Single(binds = [IUltrasoundRepository::class])
class UltrasoundRepositoryImpl(
    @Provided private val database: AnimallyDatabase,
) : IUltrasoundRepository {
    private val ultrasoundQueries: UltrasoundQueries = database.ultrasoundQueries

    override fun getByPatient(patientId: Long): List<Ultrasound> =
        ultrasoundQueries
            .selectByPatient(patientId)
            .executeAsList()
            .map { it.toDomain() }
            .sortedByDescending { it.date }

    override fun getById(id: Long): Ultrasound? = ultrasoundQueries.selectById(id).executeAsOneOrNull()?.toDomain()

    override fun insert(ultrasound: Ultrasound): Long =
        ultrasoundQueries
            .insert(
                patientId = ultrasound.patientId,
                date = ultrasound.date,
                ovaryStatus = ultrasound.ovaryStatus,
                uterineStatus = ultrasound.uterineStatus,
                follicleSizeMm = ultrasound.follicleSizeMm,
                findings = ultrasound.findings,
                imageUris = ultrasound.imageUris,
                vetName = ultrasound.vetName,
                notes = ultrasound.notes,
                isActive = ultrasound.isActive,
                createdAt = ultrasound.createdAt,
                updatedAt = ultrasound.updatedAt,
            ).value

    override fun update(ultrasound: Ultrasound): Long =
        ultrasoundQueries
            .update(
                id = ultrasound.id,
                patientId = ultrasound.patientId,
                date = ultrasound.date,
                ovaryStatus = ultrasound.ovaryStatus,
                uterineStatus = ultrasound.uterineStatus,
                follicleSizeMm = ultrasound.follicleSizeMm,
                findings = ultrasound.findings,
                imageUris = ultrasound.imageUris,
                vetName = ultrasound.vetName,
                notes = ultrasound.notes,
                isActive = ultrasound.isActive,
                updatedAt = ultrasound.updatedAt,
            ).value

    override fun setInactive(
        id: Long,
        updatedAt: Instant,
    ): Long =
        ultrasoundQueries
            .setInactive(
                id = id,
                updatedAt = updatedAt,
            ).value
}
