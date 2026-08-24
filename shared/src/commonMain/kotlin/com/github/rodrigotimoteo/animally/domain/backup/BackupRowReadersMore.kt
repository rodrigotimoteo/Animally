package com.github.rodrigotimoteo.animally.domain.backup

import com.github.rodrigotimoteo.animally.data.AnimallyDatabase

internal fun AnimallyDatabase.medicationRows(): List<MedicationDto> =
    medicationQueries
        .selectAllRows()
        .executeAsList()
        .map { it.toDto() }

internal fun AnimallyDatabase.labResultRows(): List<LabResultDto> =
    labResultQueries
        .selectAllRows()
        .executeAsList()
        .map { it.toDto() }

internal fun AnimallyDatabase.imagingRows(): List<ImagingDto> =
    imagingQueries
        .selectAllRows()
        .executeAsList()
        .map { it.toDto() }

internal fun AnimallyDatabase.farrierVisitRows(): List<FarrierVisitDto> =
    farrierVisitQueries
        .selectAllRows()
        .executeAsList()
        .map { it.toDto() }

internal fun AnimallyDatabase.reproductionRows(): List<ReproductionEventDto> =
    reproductionQueries
        .selectAllRows()
        .executeAsList()
        .map { it.toDto() }

internal fun AnimallyDatabase.ultrasoundRows(): List<UltrasoundDto> =
    ultrasoundQueries
        .selectAllRows()
        .executeAsList()
        .map { it.toDto() }

internal fun AnimallyDatabase.gestationRows(): List<GestationDto> =
    gestationQueries
        .selectAllRows()
        .executeAsList()
        .map { it.toDto() }

internal fun AnimallyDatabase.reproMedicationRows(): List<ReproMedicationDto> =
    reproMedicationQueries
        .selectAllRows()
        .executeAsList()
        .map { it.toDto() }

internal fun AnimallyDatabase.substanceRows(): List<ControlledSubstanceDto> =
    substanceQueries
        .selectAllRows()
        .executeAsList()
        .map { it.toDto() }

internal fun AnimallyDatabase.follicleRows(): List<FollicleDto> =
    follicleQueries
        .selectAllRows()
        .executeAsList()
        .map { it.toDto() }

internal fun AnimallyDatabase.embryoTransferRows(): List<EmbryoTransferDto> =
    embryoTransferQueries
        .selectAllRows()
        .executeAsList()
        .map { it.toDto() }

internal fun AnimallyDatabase.icsiRows(): List<IcsiDto> =
    icsiQueries
        .selectAllRows()
        .executeAsList()
        .map { it.toDto() }
