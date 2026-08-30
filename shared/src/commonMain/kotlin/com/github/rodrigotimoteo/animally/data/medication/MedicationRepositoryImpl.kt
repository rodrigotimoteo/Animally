package com.github.rodrigotimoteo.animally.data.medication

import app.cash.sqldelight.Query
import app.cash.sqldelight.db.QueryResult
import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.data.common.BasePatientRepository
import com.github.rodrigotimoteo.animally.data.common.DomainMapper
import com.github.rodrigotimoteo.animally.data.medication.mapper.toDomain
import com.github.rodrigotimoteo.animally.domain.medication.IMedicationRepository
import com.github.rodrigotimoteo.animally.domain.medication.model.Medication
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single
import kotlin.time.Instant
import com.github.rodrigotimoteo.animally.data.migrations.Medication as DbMedication

/**
 * Repository implementation for managing [Medication] records.
 *
 * Extends [BasePatientRepository] for shared getByPatient/getById/insert/update/setInactive wiring.
 * The explicit trampoline overrides below look redundant (they just delegate to `super`) but are required
 * to satisfy [IMedicationRepository] with its domain-named parameters (`medication` vs base `domain`).
 */
@Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE") // base uses `domain: Medication`, interface uses `medication: Medication`
@Single(binds = [IMedicationRepository::class])
class MedicationRepositoryImpl(
    @Provided database: AnimallyDatabase,
) : BasePatientRepository<DbMedication, Medication>(
        database = database,
        mapper = DomainMapper { toDomain() },
    ),
    IMedicationRepository {
    private val medicationQueries: MedicationQueries = database.medicationQueries

    override fun selectByPatient(patientId: Long): Query<DbMedication> = medicationQueries.selectByPatient(patientId)

    override fun selectById(id: Long): Query<DbMedication> = medicationQueries.selectById(id)

    override fun doInsert(domain: Medication): QueryResult<Long> =
        medicationQueries.insert(
            patientId = domain.patientId,
            name = domain.name,
            dosage = domain.dosage,
            route = domain.route,
            frequency = domain.frequency,
            startDate = domain.startDate,
            endDate = domain.endDate,
            prescribedBy = domain.prescribedBy,
            notes = domain.notes,
            isActive = domain.isActive,
            createdAt = domain.createdAt,
            updatedAt = domain.updatedAt,
        )

    override fun doUpdate(domain: Medication): QueryResult<Long> =
        medicationQueries.update(
            patientId = domain.patientId,
            name = domain.name,
            dosage = domain.dosage,
            route = domain.route,
            frequency = domain.frequency,
            startDate = domain.startDate,
            endDate = domain.endDate,
            prescribedBy = domain.prescribedBy,
            notes = domain.notes,
            isActive = domain.isActive,
            updatedAt = domain.updatedAt,
            id = domain.id,
        )

    override fun doSetInactive(
        id: Long,
        updatedAt: Instant,
    ): QueryResult<Long> = medicationQueries.setInactive(updatedAt = updatedAt, id = id)

    // --- Trampolines to BasePatientRepository ---
    // See VaccinationRepositoryImpl for rationale: required for PARAMETER_NAME_CHANGED alignment;
    // no behavior change, each delegates to super. Sorting preserved via super+sortedByDescending.

    override fun getByPatient(patientId: Long): List<Medication> = super.getByPatient(patientId).sortedByDescending { it.startDate }

    override fun getById(id: Long): Medication? = super.getById(id)

    override fun insert(medication: Medication): Long = super.insert(medication)

    override fun update(medication: Medication): Long = super.update(medication)

    override fun setInactive(
        id: Long,
        updatedAt: Instant,
    ): Long = super.setInactive(id, updatedAt)
}
