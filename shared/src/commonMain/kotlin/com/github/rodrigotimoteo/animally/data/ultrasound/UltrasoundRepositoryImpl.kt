package com.github.rodrigotimoteo.animally.data.ultrasound

import app.cash.sqldelight.Query
import app.cash.sqldelight.db.QueryResult
import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.data.common.BasePatientRepository
import com.github.rodrigotimoteo.animally.data.common.DomainMapper
import com.github.rodrigotimoteo.animally.data.ultrasound.mapper.toDomain
import com.github.rodrigotimoteo.animally.domain.ultrasound.IUltrasoundRepository
import com.github.rodrigotimoteo.animally.domain.ultrasound.model.Ultrasound
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single
import kotlin.time.Instant
import com.github.rodrigotimoteo.animally.data.migrations.Ultrasound as DbUltrasound

/**
 * Repository implementation for managing [Ultrasound] records.
 */
@Single(binds = [IUltrasoundRepository::class])
class UltrasoundRepositoryImpl(
    @Provided database: AnimallyDatabase,
) : BasePatientRepository<DbUltrasound, Ultrasound>(
        database = database,
        mapper = DomainMapper { toDomain() },
    ),
    IUltrasoundRepository {
    private val ultrasoundQueries: UltrasoundQueries = database.ultrasoundQueries

    override fun selectByPatient(patientId: Long): Query<DbUltrasound> = ultrasoundQueries.selectByPatient(patientId)

    override fun selectById(id: Long): Query<DbUltrasound> = ultrasoundQueries.selectById(id)

    override fun doInsert(domain: Ultrasound): QueryResult<Long> =
        ultrasoundQueries.insert(
            patientId = domain.patientId,
            date = domain.date,
            ovaryStatus = domain.ovaryStatus,
            uterineStatus = domain.uterineStatus,
            follicleSizeMm = domain.follicleSizeMm,
            leftOvaryStatus = domain.leftOvaryStatus,
            rightOvaryStatus = domain.rightOvaryStatus,
            leftFollicleSizeMm = domain.leftFollicleSizeMm,
            rightFollicleSizeMm = domain.rightFollicleSizeMm,
            uterineEdema = domain.uterineEdema,
            uterineLiquid = domain.uterineLiquid,
            uterineLiquidDescription = domain.uterineLiquidDescription,
            uterusDescription = domain.uterusDescription,
            findings = domain.findings,
            imageUris = domain.imageUris,
            vetName = domain.vetName,
            notes = domain.notes,
            isActive = domain.isActive,
            createdAt = domain.createdAt,
            updatedAt = domain.updatedAt,
        )

    override fun doUpdate(domain: Ultrasound): QueryResult<Long> =
        ultrasoundQueries.update(
            patientId = domain.patientId,
            date = domain.date,
            ovaryStatus = domain.ovaryStatus,
            uterineStatus = domain.uterineStatus,
            follicleSizeMm = domain.follicleSizeMm,
            leftOvaryStatus = domain.leftOvaryStatus,
            rightOvaryStatus = domain.rightOvaryStatus,
            leftFollicleSizeMm = domain.leftFollicleSizeMm,
            rightFollicleSizeMm = domain.rightFollicleSizeMm,
            uterineEdema = domain.uterineEdema,
            uterineLiquid = domain.uterineLiquid,
            uterineLiquidDescription = domain.uterineLiquidDescription,
            uterusDescription = domain.uterusDescription,
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
    ): QueryResult<Long> = ultrasoundQueries.setInactive(updatedAt = updatedAt, id = id)

    override fun insert(ultrasound: Ultrasound): Long = super.insertDomain(ultrasound)

    override fun update(ultrasound: Ultrasound): Long = super.updateDomain(ultrasound)
}
