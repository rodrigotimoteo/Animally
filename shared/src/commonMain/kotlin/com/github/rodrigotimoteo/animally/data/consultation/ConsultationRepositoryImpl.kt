package com.github.rodrigotimoteo.animally.data.consultation

import app.cash.sqldelight.Query
import app.cash.sqldelight.db.QueryResult
import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.data.common.BasePatientRepository
import com.github.rodrigotimoteo.animally.data.common.DomainMapper
import com.github.rodrigotimoteo.animally.data.consultation.mapper.toDomain
import com.github.rodrigotimoteo.animally.domain.consultation.IConsultationRepository
import com.github.rodrigotimoteo.animally.domain.consultation.model.Consultation
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single
import kotlin.time.Instant
import com.github.rodrigotimoteo.animally.data.migrations.Consultation as DbConsultation

/**
 * Repository implementation for managing [Consultation] records.
 *
 * Extends [BasePatientRepository] for shared getByPatient/getById/insert/update/setInactive wiring.
 * The explicit trampoline overrides below look redundant (they just delegate to `super`) but are required
 * to satisfy [IConsultationRepository] with its domain-named parameters (`consultation` vs base `domain`).
 */
@Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE") // base uses `domain: Consultation`, interface uses `consultation: Consultation`
@Single(binds = [IConsultationRepository::class])
class ConsultationRepositoryImpl(
    @Provided database: AnimallyDatabase,
) : BasePatientRepository<DbConsultation, Consultation>(
        database = database,
        mapper = DomainMapper { toDomain() },
    ),
    IConsultationRepository {
    private val consultationQueries: ConsultationQueries = database.consultationQueries

    override fun selectByPatient(patientId: Long): Query<DbConsultation> = consultationQueries.selectByPatient(patientId)

    override fun selectById(id: Long): Query<DbConsultation> = consultationQueries.selectById(id)

    override fun doInsert(domain: Consultation): QueryResult<Long> =
        consultationQueries.insert(
            patientId = domain.patientId,
            date = domain.date,
            subjective = domain.subjective,
            objective = domain.objective,
            assessment = domain.assessment,
            plan = domain.plan,
            vetName = domain.vetName,
            nextVisitDate = domain.nextVisitDate,
            isActive = domain.isActive,
            createdAt = domain.createdAt,
            updatedAt = domain.updatedAt,
        )

    override fun doUpdate(domain: Consultation): QueryResult<Long> =
        consultationQueries.update(
            patientId = domain.patientId,
            date = domain.date,
            subjective = domain.subjective,
            objective = domain.objective,
            assessment = domain.assessment,
            plan = domain.plan,
            vetName = domain.vetName,
            nextVisitDate = domain.nextVisitDate,
            isActive = domain.isActive,
            updatedAt = domain.updatedAt,
            id = domain.id,
        )

    override fun doSetInactive(
        id: Long,
        updatedAt: Instant,
    ): QueryResult<Long> = consultationQueries.setInactive(updatedAt = updatedAt, id = id)

    // --- Trampolines to BasePatientRepository ---
    // See VaccinationRepositoryImpl for rationale: required for PARAMETER_NAME_CHANGED alignment;
    // no behavior change, each delegates to super. Sorting preserved via super+sortedByDescending.

    override fun getByPatient(patientId: Long): List<Consultation> = super.getByPatient(patientId).sortedByDescending { it.date }

    override fun getById(id: Long): Consultation? = super.getById(id)

    override fun insert(consultation: Consultation): Long = super.insert(consultation)

    override fun update(consultation: Consultation): Long = super.update(consultation)

    override fun setInactive(
        id: Long,
        updatedAt: Instant,
    ): Long = super.setInactive(id, updatedAt)
}
