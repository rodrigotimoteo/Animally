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
 */
@Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE") // base uses `value: Consultation`, interface uses `consultation: Consultation`
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
}
