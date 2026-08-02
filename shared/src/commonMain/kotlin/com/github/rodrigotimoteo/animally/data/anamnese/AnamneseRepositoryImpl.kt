package com.github.rodrigotimoteo.animally.data.anamnese

import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.data.anamnese.mapper.toDomain
import com.github.rodrigotimoteo.animally.domain.anamnese.IAnamneseRepository
import com.github.rodrigotimoteo.animally.domain.anamnese.model.Anamnese
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single

/**
 * Repository implementation for managing [Anamnese] records.
 *
 * Anamnese is 1:1 with a patient, so [save] upserts: it looks up the existing row
 * for the patient and updates it, or inserts a new row when none exists.
 */
@Single(binds = [IAnamneseRepository::class])
class AnamneseRepositoryImpl(
    @Provided private val database: AnimallyDatabase,
) : IAnamneseRepository {
    private val anamneseQueries: AnamneseQueries = database.anamneseQueries

    override fun getByPatient(patientId: Long): Anamnese? {
        val anamnese = anamneseQueries.selectByPatient(patientId).executeAsOneOrNull()
        return anamnese?.toDomain()
    }

    override fun save(anamnese: Anamnese): Long {
        val existing = anamneseQueries.selectByPatient(anamnese.patientId).executeAsOneOrNull()
        if (existing != null) {
            anamneseQueries.update(
                id = existing.id,
                generalHistory = anamnese.generalHistory,
                chronicConditions = anamnese.chronicConditions,
                allergies = anamnese.allergies,
                updatedAt = anamnese.updatedAt,
            )
            return existing.id
        }
        anamneseQueries.insert(
            patientId = anamnese.patientId,
            generalHistory = anamnese.generalHistory,
            chronicConditions = anamnese.chronicConditions,
            allergies = anamnese.allergies,
            createdAt = anamnese.createdAt,
            updatedAt = anamnese.updatedAt,
        )
        return anamneseQueries.selectByPatient(anamnese.patientId).executeAsOne().id
    }
}
