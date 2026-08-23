package com.github.rodrigotimoteo.animally.data.surgery

import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.data.surgery.mapper.toDomain
import com.github.rodrigotimoteo.animally.domain.surgery.ISurgeryRepository
import com.github.rodrigotimoteo.animally.domain.surgery.model.Surgery
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single
import kotlin.time.Instant

/**
 * Repository implementation for managing [Surgery] records.
 */
@Single(binds = [ISurgeryRepository::class])
class SurgeryRepositoryImpl(
    @Provided private val database: AnimallyDatabase,
) : ISurgeryRepository {
    private val surgeryQueries: SurgeryQueries = database.surgeryQueries

    override fun getByPatient(patientId: Long): List<Surgery> =
        surgeryQueries
            .selectByPatient(patientId)
            .executeAsList()
            .map { it.toDomain() }
            .sortedByDescending { it.date }

    override fun getById(id: Long): Surgery? = surgeryQueries.selectById(id).executeAsOneOrNull()?.toDomain()

    override fun insert(surgery: Surgery): Long =
        database.transactionWithResult {
            surgeryQueries.insert(
                patientId = surgery.patientId,
                date = surgery.date,
                type = surgery.type,
                description = surgery.description,
                outcome = surgery.outcome,
                surgeon = surgery.surgeon,
                anesthesia = surgery.anesthesia,
                analgesia = surgery.analgesia,
                complications = surgery.complications,
                recoveryNotes = surgery.recoveryNotes,
                isActive = surgery.isActive,
                createdAt = surgery.createdAt,
                updatedAt = surgery.updatedAt,
            )
            database.commonQueries.selectLastRowId().executeAsOne()
        }

    override fun update(surgery: Surgery): Long =
        surgeryQueries
            .update(
                id = surgery.id,
                patientId = surgery.patientId,
                date = surgery.date,
                type = surgery.type,
                description = surgery.description,
                outcome = surgery.outcome,
                surgeon = surgery.surgeon,
                anesthesia = surgery.anesthesia,
                analgesia = surgery.analgesia,
                complications = surgery.complications,
                recoveryNotes = surgery.recoveryNotes,
                isActive = surgery.isActive,
                updatedAt = surgery.updatedAt,
            ).value

    override fun setInactive(
        id: Long,
        updatedAt: Instant,
    ): Long =
        surgeryQueries
            .setInactive(
                id = id,
                updatedAt = updatedAt,
            ).value
}
