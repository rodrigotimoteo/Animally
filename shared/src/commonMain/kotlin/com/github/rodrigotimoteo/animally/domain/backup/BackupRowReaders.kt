package com.github.rodrigotimoteo.animally.domain.backup

import com.github.rodrigotimoteo.animally.data.AnimallyDatabase

internal fun AnimallyDatabase.patientRows(): List<PatientDto> =
    patientQueries
        .selectAllRows()
        .executeAsList()
        .map { it.toDto() }

internal fun AnimallyDatabase.ownerRows(): List<OwnerDto> =
    ownerQueries
        .selectAllRows()
        .executeAsList()
        .map { it.toDto() }

internal fun AnimallyDatabase.anamneseRows(): List<AnamneseDto> =
    anamneseQueries
        .selectAllRows()
        .executeAsList()
        .map { it.toDto() }

internal fun AnimallyDatabase.consultationRows(): List<ConsultationDto> =
    consultationQueries
        .selectAllRows()
        .executeAsList()
        .map { it.toDto() }

internal fun AnimallyDatabase.vaccinationRows(): List<VaccinationDto> =
    vaccinationQueries
        .selectAllRows()
        .executeAsList()
        .map { it.toDto() }

internal fun AnimallyDatabase.weightRows(): List<WeightDto> =
    weightQueries
        .selectAllRows()
        .executeAsList()
        .map { it.toDto() }

internal fun AnimallyDatabase.dewormingRows(): List<DewormingDto> =
    dewormingQueries
        .selectAllRows()
        .executeAsList()
        .map { it.toDto() }

internal fun AnimallyDatabase.dentistryRows(): List<DentistryDto> =
    dentistryQueries
        .selectAllRows()
        .executeAsList()
        .map { it.toDto() }

internal fun AnimallyDatabase.lamenessRows(): List<LamenessDto> =
    lamenessQueries
        .selectAllRows()
        .executeAsList()
        .map { it.toDto() }

internal fun AnimallyDatabase.surgeryRows(): List<SurgeryDto> =
    surgeryQueries
        .selectAllRows()
        .executeAsList()
        .map { it.toDto() }
