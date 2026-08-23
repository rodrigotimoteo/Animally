package com.github.rodrigotimoteo.animally.data.gestation

import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.data.gestation.mapper.toDomain
import com.github.rodrigotimoteo.animally.domain.gestation.IGestationRepository
import com.github.rodrigotimoteo.animally.domain.gestation.model.Gestation
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single
import kotlin.time.Instant

/**
 * Repository implementation for managing [Gestation] records.
 */
@Single(binds = [IGestationRepository::class])
class GestationRepositoryImpl(
    @Provided private val database: AnimallyDatabase,
) : IGestationRepository {
    private val gestationQueries: GestationQueries = database.gestationQueries

    override fun getByPatient(patientId: Long): List<Gestation> =
        gestationQueries
            .selectByPatient(patientId)
            .executeAsList()
            .map { it.toDomain() }
            .sortedByDescending { it.breedingDate }

    override fun getById(id: Long): Gestation? = gestationQueries.selectById(id).executeAsOneOrNull()?.toDomain()

    override fun insert(gestation: Gestation): Long =
        database.transactionWithResult {
            gestationQueries.insert(
                patientId = gestation.patientId,
                breedingDate = gestation.breedingDate,
                expectedDueDate = gestation.expectedDueDate,
                gestationDays = gestation.gestationDays.toLong(),
                status = gestation.status,
                fetalCount = gestation.fetalCount?.toLong(),
                lastCheckDate = gestation.lastCheckDate,
                notes = gestation.notes,
                isActive = gestation.isActive,
                createdAt = gestation.createdAt,
                updatedAt = gestation.updatedAt,
            )
            database.commonQueries.selectLastRowId().executeAsOne()
        }

    override fun update(gestation: Gestation): Long =
        gestationQueries
            .update(
                id = gestation.id,
                patientId = gestation.patientId,
                breedingDate = gestation.breedingDate,
                expectedDueDate = gestation.expectedDueDate,
                gestationDays = gestation.gestationDays.toLong(),
                status = gestation.status,
                fetalCount = gestation.fetalCount?.toLong(),
                lastCheckDate = gestation.lastCheckDate,
                notes = gestation.notes,
                isActive = gestation.isActive,
                updatedAt = gestation.updatedAt,
            ).value

    override fun setInactive(
        id: Long,
        updatedAt: Instant,
    ): Long =
        gestationQueries
            .setInactive(
                id = id,
                updatedAt = updatedAt,
            ).value
}
