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
 *
 * Extends [BasePatientRepository] for shared getByPatient/getById/insert/update/setInactive wiring.
 * The explicit trampoline overrides below look redundant (they just delegate to `super`) but are required
 * to satisfy [ISurgeryRepository] with its domain-named parameters (`surgery` vs base `domain`).
 */
@Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE") // base uses `domain: Surgery`, interface uses `surgery: Surgery`
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

    // --- Trampolines to BasePatientRepository ---
    // See VaccinationRepositoryImpl for rationale: required for PARAMETER_NAME_CHANGED alignment;
    // no behavior change, each delegates to super. Sorting preserved via super+sortedByDescending.

    override fun getByPatient(patientId: Long): List<Surgery> = super.getByPatient(patientId).sortedByDescending { it.date }

    override fun getById(id: Long): Surgery? = super.getById(id)

    override fun insert(surgery: Surgery): Long = super.insert(surgery)

    override fun update(surgery: Surgery): Long = super.update(surgery)

    override fun setInactive(
        id: Long,
        updatedAt: Instant,
    ): Long = super.setInactive(id, updatedAt)
}
