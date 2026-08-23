package com.github.rodrigotimoteo.animally.data.vaccination

import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.data.vaccination.mapper.toDomain
import com.github.rodrigotimoteo.animally.domain.vaccination.IVaccinationRepository
import com.github.rodrigotimoteo.animally.domain.vaccination.model.Vaccination
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single
import kotlin.time.Instant

/**
 * Repository implementation for managing [Vaccination] records.
 */
@Single(binds = [IVaccinationRepository::class])
class VaccinationRepositoryImpl(
    @Provided private val database: AnimallyDatabase,
) : IVaccinationRepository {
    private val vaccinationQueries: VaccinationQueries = database.vaccinationQueries

    override fun getByPatient(patientId: Long): List<Vaccination> =
        vaccinationQueries
            .selectByPatient(patientId)
            .executeAsList()
            .map { it.toDomain() }

    override fun getById(id: Long): Vaccination? = vaccinationQueries.selectById(id).executeAsOneOrNull()?.toDomain()

    override fun insert(vaccination: Vaccination): Long =
        database.transactionWithResult {
            vaccinationQueries.insert(
                patientId = vaccination.patientId,
                vaccineName = vaccination.vaccineName,
                dateAdministered = vaccination.dateAdministered,
                nextDueDate = vaccination.nextDueDate,
                vetName = vaccination.vetName,
                batchNumber = vaccination.batchNumber,
                site = vaccination.site,
                notes = vaccination.notes,
                isActive = vaccination.isActive,
                createdAt = vaccination.createdAt,
                updatedAt = vaccination.updatedAt,
            )
            database.commonQueries.selectLastRowId().executeAsOne()
        }

    override fun update(vaccination: Vaccination): Long =
        vaccinationQueries
            .update(
                id = vaccination.id,
                patientId = vaccination.patientId,
                vaccineName = vaccination.vaccineName,
                dateAdministered = vaccination.dateAdministered,
                nextDueDate = vaccination.nextDueDate,
                vetName = vaccination.vetName,
                batchNumber = vaccination.batchNumber,
                site = vaccination.site,
                notes = vaccination.notes,
                isActive = vaccination.isActive,
                updatedAt = vaccination.updatedAt,
            ).value

    override fun setInactive(
        id: Long,
        updatedAt: Instant,
    ): Long =
        vaccinationQueries
            .setInactive(
                id = id,
                updatedAt = updatedAt,
            ).value
}
