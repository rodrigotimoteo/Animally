package com.github.rodrigotimoteo.animally.data.embryotransfer

import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.data.embryotransfer.mapper.toDomain
import com.github.rodrigotimoteo.animally.domain.embryotransfer.IEmbryoTransferRepository
import com.github.rodrigotimoteo.animally.domain.embryotransfer.model.EmbryoTransfer
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single
import kotlin.time.Instant

/**
 * Repository implementation for managing [EmbryoTransfer] records.
 */
@Single(binds = [IEmbryoTransferRepository::class])
class EmbryoTransferRepositoryImpl(
    @Provided private val database: AnimallyDatabase,
) : IEmbryoTransferRepository {
    private val queries = database.embryoTransferQueries

    override fun getByPatient(patientId: Long): List<EmbryoTransfer> =
        queries
            .selectByPatient(patientId)
            .executeAsList()
            .map { it.toDomain() }
            .sortedByDescending { it.date }

    override fun getById(id: Long): EmbryoTransfer? = queries.selectById(id).executeAsOneOrNull()?.toDomain()

    override fun insert(record: EmbryoTransfer): Long =
        queries
            .insert(
                patientId = record.patientId,
                date = record.date,
                embryoCount = record.embryoCount.toLong(),
                recipientMares = record.recipientMares,
                vetName = record.vetName,
                notes = record.notes,
                isActive = record.isActive,
                createdAt = record.createdAt,
                updatedAt = record.updatedAt,
            ).value

    override fun update(record: EmbryoTransfer): Long =
        queries
            .update(
                patientId = record.patientId,
                date = record.date,
                embryoCount = record.embryoCount.toLong(),
                recipientMares = record.recipientMares,
                vetName = record.vetName,
                notes = record.notes,
                isActive = record.isActive,
                updatedAt = record.updatedAt,
                id = record.id,
            ).value

    override fun setInactive(
        id: Long,
        updatedAt: Instant,
    ): Long =
        queries
            .setInactive(
                id = id,
                updatedAt = updatedAt,
            ).value
}
