package com.github.rodrigotimoteo.animally.data.consultation

import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.data.consultation.mapper.toDomain
import com.github.rodrigotimoteo.animally.domain.consultation.IConsultationRepository
import com.github.rodrigotimoteo.animally.domain.consultation.model.Consultation
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single
import kotlin.time.Instant

/**
 * Repository implementation for managing [Consultation] records.
 */
@Single(binds = [IConsultationRepository::class])
class ConsultationRepositoryImpl(
    @Provided private val database: AnimallyDatabase,
) : IConsultationRepository {
    private val consultationQueries: ConsultationQueries = database.consultationQueries

    override fun getByPatient(patientId: Long): List<Consultation> =
        consultationQueries
            .selectByPatient(patientId)
            .executeAsList()
            .map { it.toDomain() }
            .sortedByDescending { it.date }

    override fun getById(id: Long): Consultation? = consultationQueries.selectById(id).executeAsOneOrNull()?.toDomain()

    override fun insert(consultation: Consultation): Long =
        database.transactionWithResult {
            consultationQueries.insert(
                patientId = consultation.patientId,
                date = consultation.date,
                subjective = consultation.subjective,
                objective = consultation.objective,
                assessment = consultation.assessment,
                plan = consultation.plan,
                vetName = consultation.vetName,
                nextVisitDate = consultation.nextVisitDate,
                isActive = consultation.isActive,
                createdAt = consultation.createdAt,
                updatedAt = consultation.updatedAt,
            )
            database.commonQueries.selectLastRowId().executeAsOne()
        }

    override fun update(consultation: Consultation): Long =
        consultationQueries
            .update(
                id = consultation.id,
                patientId = consultation.patientId,
                date = consultation.date,
                subjective = consultation.subjective,
                objective = consultation.objective,
                assessment = consultation.assessment,
                plan = consultation.plan,
                vetName = consultation.vetName,
                nextVisitDate = consultation.nextVisitDate,
                isActive = consultation.isActive,
                updatedAt = consultation.updatedAt,
            ).value

    override fun setInactive(
        id: Long,
        updatedAt: Instant,
    ): Long =
        consultationQueries
            .setInactive(
                id = id,
                updatedAt = updatedAt,
            ).value
}
