package com.github.rodrigotimoteo.animally.data.dentistry

import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.data.dentistry.mapper.toDomain
import com.github.rodrigotimoteo.animally.domain.dentistry.IDentistryRepository
import com.github.rodrigotimoteo.animally.domain.dentistry.model.Dentistry
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single
import kotlin.time.Instant

/**
 * Repository implementation for managing [Dentistry] records.
 */
@Single(binds = [IDentistryRepository::class])
class DentistryRepositoryImpl(
    @Provided private val database: AnimallyDatabase,
) : IDentistryRepository {
    private val dentistryQueries: DentistryQueries = database.dentistryQueries

    override fun getByPatient(patientId: Long): List<Dentistry> =
        dentistryQueries
            .selectByPatient(patientId)
            .executeAsList()
            .map { it.toDomain() }

    override fun getById(id: Long): Dentistry? = dentistryQueries.selectById(id).executeAsOneOrNull()?.toDomain()

    override fun insert(dentistry: Dentistry): Long =
        database.transactionWithResult {
            dentistryQueries.insert(
                patientId = dentistry.patientId,
                date = dentistry.date,
                findings = dentistry.findings,
                treatment = dentistry.treatment,
                nextDueDate = dentistry.nextDueDate,
                vetName = dentistry.vetName,
                notes = dentistry.notes,
                isActive = dentistry.isActive,
                createdAt = dentistry.createdAt,
                updatedAt = dentistry.updatedAt,
            )
            database.commonQueries.selectLastRowId().executeAsOne()
        }

    override fun update(dentistry: Dentistry): Long =
        dentistryQueries
            .update(
                id = dentistry.id,
                patientId = dentistry.patientId,
                date = dentistry.date,
                findings = dentistry.findings,
                treatment = dentistry.treatment,
                nextDueDate = dentistry.nextDueDate,
                vetName = dentistry.vetName,
                notes = dentistry.notes,
                isActive = dentistry.isActive,
                updatedAt = dentistry.updatedAt,
            ).value

    override fun setInactive(
        id: Long,
        updatedAt: Instant,
    ): Long =
        dentistryQueries
            .setInactive(
                id = id,
                updatedAt = updatedAt,
            ).value
}
