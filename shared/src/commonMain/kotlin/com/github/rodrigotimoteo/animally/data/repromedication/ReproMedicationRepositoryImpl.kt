package com.github.rodrigotimoteo.animally.data.repromedication

import app.cash.sqldelight.Query
import app.cash.sqldelight.db.QueryResult
import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.data.common.BasePatientRepository
import com.github.rodrigotimoteo.animally.data.common.DomainMapper
import com.github.rodrigotimoteo.animally.data.repromedication.mapper.toDomain
import com.github.rodrigotimoteo.animally.domain.repromedication.IReproMedicationRepository
import com.github.rodrigotimoteo.animally.domain.repromedication.model.ReproMedication
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single
import kotlin.time.Instant
import com.github.rodrigotimoteo.animally.data.migrations.ReproMedication as DbReproMedication

/**
 * Repository implementation for managing [ReproMedication] records.
 *
 * Extends [BasePatientRepository] for shared getByPatient/getById/insert/update/setInactive wiring.
 * The explicit trampoline overrides below look redundant (they just delegate to `super`) but are required
 * to satisfy [IReproMedicationRepository] with its domain-named parameters (`reproMedication` vs base `domain`).
 */
@Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE") // base uses `domain: ReproMedication`, interface uses `reproMedication: ReproMedication`
@Single(binds = [IReproMedicationRepository::class])
class ReproMedicationRepositoryImpl(
    @Provided database: AnimallyDatabase,
) : BasePatientRepository<DbReproMedication, ReproMedication>(
        database = database,
        mapper = DomainMapper { toDomain() },
    ),
    IReproMedicationRepository {
    private val reproMedQueries: ReproMedicationQueries = database.reproMedicationQueries

    override fun selectByPatient(patientId: Long): Query<DbReproMedication> = reproMedQueries.selectByPatient(patientId)

    override fun selectById(id: Long): Query<DbReproMedication> = reproMedQueries.selectById(id)

    override fun doInsert(domain: ReproMedication): QueryResult<Long> =
        reproMedQueries.insert(
            patientId = domain.patientId,
            medication = domain.medication,
            dateAdministered = domain.dateAdministered,
            dosage = domain.dosage,
            purpose = domain.purpose,
            vetName = domain.vetName,
            notes = domain.notes,
            isActive = domain.isActive,
            createdAt = domain.createdAt,
            updatedAt = domain.updatedAt,
        )

    override fun doUpdate(domain: ReproMedication): QueryResult<Long> =
        reproMedQueries.update(
            patientId = domain.patientId,
            medication = domain.medication,
            dateAdministered = domain.dateAdministered,
            dosage = domain.dosage,
            purpose = domain.purpose,
            vetName = domain.vetName,
            notes = domain.notes,
            isActive = domain.isActive,
            updatedAt = domain.updatedAt,
            id = domain.id,
        )

    override fun doSetInactive(
        id: Long,
        updatedAt: Instant,
    ): QueryResult<Long> = reproMedQueries.setInactive(updatedAt = updatedAt, id = id)

    // --- Trampolines to BasePatientRepository ---
    // See VaccinationRepositoryImpl for rationale: required for PARAMETER_NAME_CHANGED alignment;
    // no behavior change, each delegates to super. Sorting preserved via super+sortedByDescending.

    override fun getByPatient(patientId: Long): List<ReproMedication> =
        super
            .getByPatient(patientId)
            .sortedByDescending { it.dateAdministered }

    override fun getById(id: Long): ReproMedication? = super.getById(id)

    override fun insert(reproMedication: ReproMedication): Long = super.insert(reproMedication)

    override fun update(reproMedication: ReproMedication): Long = super.update(reproMedication)

    override fun setInactive(
        id: Long,
        updatedAt: Instant,
    ): Long = super.setInactive(id, updatedAt)
}
