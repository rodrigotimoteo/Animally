package com.github.rodrigotimoteo.animally.data.sync

import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.domain.sync.ChangedRecord
import com.github.rodrigotimoteo.animally.domain.sync.SyncChangeTracker
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single
import kotlin.time.Instant

/**
 * Collects rows changed since a given instant across all synced entity tables.
 */
@Single(binds = [SyncChangeTracker::class])
class SyncChangeTrackerImpl(
    @Provided private val database: AnimallyDatabase,
) : SyncChangeTracker {
    override suspend fun recordsChangedSince(instant: Instant): List<ChangedRecord> =
        buildList {
            addAll(collectOwnerPatientRecords(instant))
            addAll(collectConsultationRecords(instant))
            addAll(collectFarrierLamenessRecords(instant))
            addAll(collectReproMedicationRecords(instant))
            addAll(collectReproductionRecords(instant))
            addAll(collectImagingLabRecords(instant))
            addAll(collectVaccinationMiscRecords(instant))
        }

    private fun collectOwnerPatientRecords(instant: Instant): List<ChangedRecord> =
        buildList {
            addAll(
                database.ownerQueries.selectChangedSince(instant).executeAsList().map { row ->
                    ChangedRecord(
                        entityType = "Owner",
                        id = row.id,
                        updatedAt = row.updatedAt,
                        serverId = row.serverId,
                        isActive = row.isActive,
                    )
                },
            )
            addAll(
                database.patientQueries.selectChangedSince(instant).executeAsList().map { row ->
                    ChangedRecord(
                        entityType = "Patient",
                        id = row.id,
                        updatedAt = row.updatedAt,
                        serverId = row.serverId,
                        isActive = row.isActive,
                    )
                },
            )
        }

    private fun collectConsultationRecords(instant: Instant): List<ChangedRecord> =
        buildList {
            addAll(
                database.consultationQueries.selectChangedSince(instant).executeAsList().map { row ->
                    ChangedRecord(
                        entityType = "Consultation",
                        id = row.id,
                        updatedAt = row.updatedAt,
                        serverId = row.serverId,
                        isActive = row.isActive,
                    )
                },
            )
            addAll(
                database.dentistryQueries.selectChangedSince(instant).executeAsList().map { row ->
                    ChangedRecord(
                        entityType = "Dentistry",
                        id = row.id,
                        updatedAt = row.updatedAt,
                        serverId = row.serverId,
                        isActive = row.isActive,
                    )
                },
            )
            addAll(
                database.dewormingQueries.selectChangedSince(instant).executeAsList().map { row ->
                    ChangedRecord(
                        entityType = "Deworming",
                        id = row.id,
                        updatedAt = row.updatedAt,
                        serverId = row.serverId,
                        isActive = row.isActive,
                    )
                },
            )
        }

    private fun collectFarrierLamenessRecords(instant: Instant): List<ChangedRecord> =
        buildList {
            addAll(
                database.farrierVisitQueries.selectChangedSince(instant).executeAsList().map { row ->
                    ChangedRecord(
                        entityType = "FarrierVisit",
                        id = row.id,
                        updatedAt = row.updatedAt,
                        serverId = row.serverId,
                        isActive = row.isActive,
                    )
                },
            )
            addAll(
                database.lamenessQueries.selectChangedSince(instant).executeAsList().map { row ->
                    ChangedRecord(
                        entityType = "Lameness",
                        id = row.id,
                        updatedAt = row.updatedAt,
                        serverId = row.serverId,
                        isActive = row.isActive,
                    )
                },
            )
            addAll(
                database.medicationQueries.selectChangedSince(instant).executeAsList().map { row ->
                    ChangedRecord(
                        entityType = "Medication",
                        id = row.id,
                        updatedAt = row.updatedAt,
                        serverId = row.serverId,
                        isActive = row.isActive,
                    )
                },
            )
        }

    private fun collectReproMedicationRecords(instant: Instant): List<ChangedRecord> =
        buildList {
            addAll(
                database.reproMedicationQueries.selectChangedSince(instant).executeAsList().map { row ->
                    ChangedRecord(
                        entityType = "ReproMedication",
                        id = row.id,
                        updatedAt = row.updatedAt,
                        serverId = row.serverId,
                        isActive = row.isActive,
                    )
                },
            )
            addAll(
                database.substanceQueries.selectChangedSince(instant).executeAsList().map { row ->
                    ChangedRecord(
                        entityType = "Substance",
                        id = row.id,
                        updatedAt = row.updatedAt,
                        serverId = row.serverId,
                        isActive = row.isActive,
                    )
                },
            )
            addAll(
                database.customReminderQueries.selectChangedSince(instant).executeAsList().map { row ->
                    ChangedRecord(
                        entityType = "CustomReminder",
                        id = row.id,
                        updatedAt = row.updatedAt,
                        serverId = row.serverId,
                        isActive = row.isActive,
                    )
                },
            )
        }

    private fun collectReproductionRecords(instant: Instant): List<ChangedRecord> =
        buildList {
            addAll(
                database.reproductionQueries.selectChangedSince(instant).executeAsList().map { row ->
                    ChangedRecord(
                        entityType = "Reproduction",
                        id = row.id,
                        updatedAt = row.updatedAt,
                        serverId = row.serverId,
                        isActive = row.isActive,
                    )
                },
            )
            addAll(
                database.gestationQueries.selectChangedSince(instant).executeAsList().map { row ->
                    ChangedRecord(
                        entityType = "Gestation",
                        id = row.id,
                        updatedAt = row.updatedAt,
                        serverId = row.serverId,
                        isActive = row.isActive,
                    )
                },
            )
            addAll(
                database.ultrasoundQueries.selectChangedSince(instant).executeAsList().map { row ->
                    ChangedRecord(
                        entityType = "Ultrasound",
                        id = row.id,
                        updatedAt = row.updatedAt,
                        serverId = row.serverId,
                        isActive = row.isActive,
                    )
                },
            )
        }

    private fun collectImagingLabRecords(instant: Instant): List<ChangedRecord> =
        buildList {
            addAll(
                database.imagingQueries.selectChangedSince(instant).executeAsList().map { row ->
                    ChangedRecord(
                        entityType = "Imaging",
                        id = row.id,
                        updatedAt = row.updatedAt,
                        serverId = row.serverId,
                        isActive = row.isActive,
                    )
                },
            )
            addAll(
                database.labResultQueries.selectChangedSince(instant).executeAsList().map { row ->
                    ChangedRecord(
                        entityType = "LabResult",
                        id = row.id,
                        updatedAt = row.updatedAt,
                        serverId = row.serverId,
                        isActive = row.isActive,
                    )
                },
            )
            addAll(
                database.surgeryQueries.selectChangedSince(instant).executeAsList().map { row ->
                    ChangedRecord(
                        entityType = "Surgery",
                        id = row.id,
                        updatedAt = row.updatedAt,
                        serverId = row.serverId,
                        isActive = row.isActive,
                    )
                },
            )
        }

    private fun collectVaccinationMiscRecords(instant: Instant): List<ChangedRecord> =
        buildList {
            addAll(
                database.vaccinationQueries.selectChangedSince(instant).executeAsList().map { row ->
                    ChangedRecord(
                        entityType = "Vaccination",
                        id = row.id,
                        updatedAt = row.updatedAt,
                        serverId = row.serverId,
                        isActive = row.isActive,
                    )
                },
            )
            addAll(
                database.anamneseQueries.selectChangedSince(instant).executeAsList().map { row ->
                    ChangedRecord(
                        entityType = "Anamnese",
                        id = row.id,
                        updatedAt = row.updatedAt,
                        serverId = row.serverId,
                        isActive = true,
                    )
                },
            )
            addAll(
                database.weightQueries.selectChangedSince(instant).executeAsList().map { row ->
                    ChangedRecord(
                        entityType = "Weight",
                        id = row.id,
                        updatedAt = row.updatedAt,
                        serverId = row.serverId,
                        isActive = row.isActive,
                    )
                },
            )
        }
}
