package com.github.rodrigotimoteo.animally.data.patient

import com.github.rodrigotimoteo.animally.data.patient.mapper.toDomain
import com.github.rodrigotimoteo.animally.domain.patient.IPatientRepository
import com.github.rodrigotimoteo.animally.domain.patient.model.Patient
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single

/**
 * Repository implementation for reading active [Patient] records linked to an owner.
 */
@Single(binds = [IPatientRepository::class])
class PatientRepositoryImpl(
    @Provided private val patientQueries: PatientQueries,
) : IPatientRepository {
    override fun getPatientsByOwnerId(ownerId: Long): List<Patient> {
        val rows = patientQueries.selectActiveByOwnerId(ownerId).executeAsList()
        return rows.map { it.toDomain() }
    }

    override fun countPatientsByOwnerId(ownerId: Long): Long {
        val count = patientQueries.countActiveByOwnerId(ownerId).executeAsOne()
        return count
    }
}
