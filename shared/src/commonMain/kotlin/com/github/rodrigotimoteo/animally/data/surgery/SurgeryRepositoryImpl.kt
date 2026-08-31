package com.github.rodrigotimoteo.animally.data.surgery

import app.cash.sqldelight.Query
import app.cash.sqldelight.db.QueryResult
import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.data.common.BasePatientRepository
import com.github.rodrigotimoteo.animally.data.common.DomainMapper
import com.github.rodrigotimoteo.animally.data.surgery.mapper.toDomain
import com.github.rodrigotimoteo.animally.domain.surgery.ISurgeryRepository
import com.github.rodrigotimoteo.animally.domain.surgery.model.Surgery
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single
import kotlin.time.Instant
import com.github.rodrigotimoteo.animally.data.migrations.Surgery as DbSurgery

/**
 * Repository implementation for managing [Surgery] records.
 */
@Single(binds = [ISurgeryRepository::class])
class SurgeryRepositoryImpl(
    @Provided database: AnimallyDatabase,
) : BasePatientRepository<DbSurgery, Surgery>(
        database = database,
        mapper = DomainMapper { toDomain() },
    ),
    ISurgeryRepository {
    private val surgeryQueries: SurgeryQueries = database.surgeryQueries

    override fun selectByPatient(patientId: Long): Query<DbSurgery> = surgeryQueries.selectByPatient(patientId)

    override fun selectById(id: Long): Query<DbSurgery> = surgeryQueries.selectById(id)

    override fun doInsert(domain: Surgery): QueryResult<Long> =
        surgeryQueries.insert(
            patientId = domain.patientId,
            date = domain.date,
            type = domain.type,
            description = domain.description,
            outcome = domain.outcome,
            surgeon = domain.surgeon,
            anesthesia = domain.anesthesia,
            analgesia = domain.analgesia,
            complications = domain.complications,
            recoveryNotes = domain.recoveryNotes,
            isActive = domain.isActive,
            createdAt = domain.createdAt,
            updatedAt = domain.updatedAt,
        )

    override fun doUpdate(domain: Surgery): QueryResult<Long> =
        surgeryQueries.update(
            patientId = domain.patientId,
            date = domain.date,
            type = domain.type,
            description = domain.description,
            outcome = domain.outcome,
            surgeon = domain.surgeon,
            anesthesia = domain.anesthesia,
            analgesia = domain.analgesia,
            complications = domain.complications,
            recoveryNotes = domain.recoveryNotes,
            isActive = domain.isActive,
            updatedAt = domain.updatedAt,
            id = domain.id,
        )

    override fun doSetInactive(
        id: Long,
        updatedAt: Instant,
    ): QueryResult<Long> = surgeryQueries.setInactive(updatedAt = updatedAt, id = id)

    override fun insert(surgery: Surgery): Long = super.insertDomain(surgery)

    override fun update(surgery: Surgery): Long = super.updateDomain(surgery)
}
