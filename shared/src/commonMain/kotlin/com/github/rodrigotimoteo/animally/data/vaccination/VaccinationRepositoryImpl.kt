package com.github.rodrigotimoteo.animally.data.vaccination

import app.cash.sqldelight.Query
import app.cash.sqldelight.db.QueryResult
import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.data.common.BasePatientRepository
import com.github.rodrigotimoteo.animally.data.common.DomainMapper
import com.github.rodrigotimoteo.animally.data.vaccination.mapper.toDomain
import com.github.rodrigotimoteo.animally.domain.vaccination.IVaccinationRepository
import com.github.rodrigotimoteo.animally.domain.vaccination.model.Vaccination
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single
import kotlin.time.Instant
import com.github.rodrigotimoteo.animally.data.migrations.Vaccination as DbVaccination

/**
 * Repository implementation for managing [Vaccination] records.
 */
@Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
// base uses `value: Vaccination`, interface uses `vaccination: Vaccination` —
// names must align per interface
@Single(binds = [IVaccinationRepository::class])
class VaccinationRepositoryImpl(
    @Provided database: AnimallyDatabase,
) : BasePatientRepository<DbVaccination, Vaccination>(
        database = database,
        mapper = DomainMapper { toDomain() },
    ),
    IVaccinationRepository {
    private val vaccinationQueries: VaccinationQueries = database.vaccinationQueries

    override fun selectByPatient(patientId: Long): Query<DbVaccination> = vaccinationQueries.selectByPatient(patientId)

    override fun selectById(id: Long): Query<DbVaccination> = vaccinationQueries.selectById(id)

    override fun doInsert(domain: Vaccination): QueryResult<Long> =
        vaccinationQueries.insert(
            patientId = domain.patientId,
            vaccineName = domain.vaccineName,
            dateAdministered = domain.dateAdministered,
            nextDueDate = domain.nextDueDate,
            vetName = domain.vetName,
            batchNumber = domain.batchNumber,
            site = domain.site,
            notes = domain.notes,
            isActive = domain.isActive,
            createdAt = domain.createdAt,
            updatedAt = domain.updatedAt,
        )

    override fun doUpdate(domain: Vaccination): QueryResult<Long> =
        vaccinationQueries.update(
            patientId = domain.patientId,
            vaccineName = domain.vaccineName,
            dateAdministered = domain.dateAdministered,
            nextDueDate = domain.nextDueDate,
            vetName = domain.vetName,
            batchNumber = domain.batchNumber,
            site = domain.site,
            notes = domain.notes,
            isActive = domain.isActive,
            updatedAt = domain.updatedAt,
            id = domain.id,
        )

    override fun doSetInactive(
        id: Long,
        updatedAt: Instant,
    ): QueryResult<Long> = vaccinationQueries.setInactive(updatedAt = updatedAt, id = id)
}
