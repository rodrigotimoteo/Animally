package com.github.rodrigotimoteo.animally.data.search

import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.data.anamnese.mapper.toDomain
import com.github.rodrigotimoteo.animally.data.consultation.mapper.toDomain
import com.github.rodrigotimoteo.animally.data.customreminder.mapper.toDomain
import com.github.rodrigotimoteo.animally.data.dentistry.mapper.toDomain
import com.github.rodrigotimoteo.animally.data.deworming.mapper.toDomain
import com.github.rodrigotimoteo.animally.data.embryotransfer.mapper.toDomain
import com.github.rodrigotimoteo.animally.data.farrier.mapper.toDomain
import com.github.rodrigotimoteo.animally.data.gestation.mapper.toDomain
import com.github.rodrigotimoteo.animally.data.icsi.mapper.toDomain
import com.github.rodrigotimoteo.animally.data.imaging.mapper.toDomain
import com.github.rodrigotimoteo.animally.data.labresult.mapper.toDomain
import com.github.rodrigotimoteo.animally.data.lameness.mapper.toDomain
import com.github.rodrigotimoteo.animally.data.medication.mapper.toDomain
import com.github.rodrigotimoteo.animally.data.owner.OwnerQueries
import com.github.rodrigotimoteo.animally.data.owner.mapper.toDomain
import com.github.rodrigotimoteo.animally.data.patient.mapper.toDomain
import com.github.rodrigotimoteo.animally.data.reproduction.mapper.toDomain
import com.github.rodrigotimoteo.animally.data.repromedication.mapper.toDomain
import com.github.rodrigotimoteo.animally.data.substance.mapper.toDomain
import com.github.rodrigotimoteo.animally.data.surgery.mapper.toDomain
import com.github.rodrigotimoteo.animally.data.ultrasound.mapper.toDomain
import com.github.rodrigotimoteo.animally.data.vaccination.mapper.toDomain
import com.github.rodrigotimoteo.animally.data.weight.mapper.toDomain
import com.github.rodrigotimoteo.animally.domain.common.RecordType
import com.github.rodrigotimoteo.animally.domain.search.ISearchRepository
import com.github.rodrigotimoteo.animally.domain.search.SearchableText

/**
 * Registry of per-entity [SearchIndexer]s. Delegates bulk reindexing to a
 * loop over the indexers so [SearchRepositoryImpl] stays a thin facade.
 */
internal class SearchIndexerRegistry(
    private val database: AnimallyDatabase,
    private val ownerQueries: OwnerQueries,
) {
    private val recordIndexers: List<SearchIndexer> =
        listOf(
            AnamneseIndexer(database),
            VaccinationIndexer(database),
            ConsultationIndexer(database),
            MedicationIndexer(database),
            DewormingIndexer(database),
            DentistryIndexer(database),
            FarrierVisitIndexer(database),
            LamenessIndexer(database),
            SurgeryIndexer(database),
            ControlledSubstanceIndexer(database),
            WeightIndexer(database),
            ReproductionEventIndexer(database),
            UltrasoundIndexer(database),
            GestationIndexer(database),
            ReproMedicationIndexer(database),
            LabResultIndexer(database),
            ImagingIndexer(database),
            EmbryoTransferIndexer(database),
            IcsiIndexer(database),
            CustomReminderIndexer(database),
        )

    fun reindexOwners(writer: SearchIndexWriter) {
        OwnerIndexer(ownerQueries).reindex(writer)
    }

    fun reindexPatients(writer: SearchIndexWriter) {
        PatientIndexer(database).reindex(writer)
    }

    fun reindexRecords(writer: SearchIndexWriter) {
        recordIndexers.forEach { it.reindex(writer) }
    }

    private class OwnerIndexer(
        private val ownerQueries: OwnerQueries,
    ) : SearchIndexer {
        override fun reindex(writer: SearchIndexWriter) {
            ownerQueries
                .selectAll()
                .executeAsList()
                .forEach { owner ->
                    val searchableText = SearchableText.owner(owner.toDomain())
                    writer(
                        ISearchRepository.TYPE_OWNER,
                        0L,
                        owner.id,
                        null,
                        searchableText,
                    )
                }
        }
    }

    private class PatientIndexer(
        private val database: AnimallyDatabase,
    ) : SearchIndexer {
        override fun reindex(writer: SearchIndexWriter) {
            database.patientQueries
                .selectAll()
                .executeAsList()
                .forEach { patient ->
                    val searchableText = SearchableText.patient(patient.toDomain())
                    writer(
                        ISearchRepository.TYPE_PATIENT,
                        patient.id,
                        patient.id,
                        null,
                        searchableText,
                    )
                }
        }
    }

    private class AnamneseIndexer(
        private val database: AnimallyDatabase,
    ) : SearchIndexer {
        override fun reindex(writer: SearchIndexWriter) {
            database.anamneseQueries.selectAllRows().executeAsList().forEach {
                val anamnese = it.toDomain()
                writer(
                    RecordType.Anamnese.wireName,
                    anamnese.patientId,
                    anamnese.id,
                    null,
                    SearchableText.anamnese(anamnese),
                )
            }
        }
    }

    private class VaccinationIndexer(
        private val database: AnimallyDatabase,
    ) : SearchIndexer {
        override fun reindex(writer: SearchIndexWriter) {
            database.vaccinationQueries.selectAll().executeAsList().forEach {
                val searchableText = SearchableText.vaccination(it.toDomain())
                writer(
                    RecordType.Vaccination.wireName,
                    it.patientId,
                    it.id,
                    it.dateAdministered,
                    searchableText,
                )
            }
        }
    }

    private class ConsultationIndexer(
        private val database: AnimallyDatabase,
    ) : SearchIndexer {
        override fun reindex(writer: SearchIndexWriter) {
            database.consultationQueries.selectAll().executeAsList().forEach {
                val searchableText = SearchableText.consultation(it.toDomain())
                writer(
                    RecordType.Consultation.wireName,
                    it.patientId,
                    it.id,
                    it.date,
                    searchableText,
                )
            }
        }
    }

    private class MedicationIndexer(
        private val database: AnimallyDatabase,
    ) : SearchIndexer {
        override fun reindex(writer: SearchIndexWriter) {
            database.medicationQueries.selectAll().executeAsList().forEach {
                val medication = it.toDomain()
                writer(
                    RecordType.Medication.wireName,
                    medication.patientId,
                    medication.id,
                    null,
                    SearchableText.medication(medication),
                )
            }
        }
    }

    private class DewormingIndexer(
        private val database: AnimallyDatabase,
    ) : SearchIndexer {
        override fun reindex(writer: SearchIndexWriter) {
            database.dewormingQueries.selectAll().executeAsList().forEach {
                val searchableText = SearchableText.deworming(it.toDomain())
                writer(
                    RecordType.Deworming.wireName,
                    it.patientId,
                    it.id,
                    it.dateAdministered,
                    searchableText,
                )
            }
        }
    }

    private class DentistryIndexer(
        private val database: AnimallyDatabase,
    ) : SearchIndexer {
        override fun reindex(writer: SearchIndexWriter) {
            database.dentistryQueries.selectAll().executeAsList().forEach {
                val searchableText = SearchableText.dentistry(it.toDomain())
                writer(
                    RecordType.Dentistry.wireName,
                    it.patientId,
                    it.id,
                    it.date,
                    searchableText,
                )
            }
        }
    }

    private class FarrierVisitIndexer(
        private val database: AnimallyDatabase,
    ) : SearchIndexer {
        override fun reindex(writer: SearchIndexWriter) {
            database.farrierVisitQueries.selectAll().executeAsList().forEach {
                val searchableText = SearchableText.farrierVisit(it.toDomain())
                writer(
                    RecordType.FarrierVisit.wireName,
                    it.patientId,
                    it.id,
                    it.date,
                    searchableText,
                )
            }
        }
    }

    private class LamenessIndexer(
        private val database: AnimallyDatabase,
    ) : SearchIndexer {
        override fun reindex(writer: SearchIndexWriter) {
            database.lamenessQueries.selectAll().executeAsList().forEach {
                val searchableText = SearchableText.lameness(it.toDomain())
                writer(
                    RecordType.Lameness.wireName,
                    it.patientId,
                    it.id,
                    it.date,
                    searchableText,
                )
            }
        }
    }

    private class SurgeryIndexer(
        private val database: AnimallyDatabase,
    ) : SearchIndexer {
        override fun reindex(writer: SearchIndexWriter) {
            database.surgeryQueries.selectAll().executeAsList().forEach {
                val searchableText = SearchableText.surgery(it.toDomain())
                writer(
                    RecordType.Surgery.wireName,
                    it.patientId,
                    it.id,
                    it.date,
                    searchableText,
                )
            }
        }
    }

    private class ControlledSubstanceIndexer(
        private val database: AnimallyDatabase,
    ) : SearchIndexer {
        override fun reindex(writer: SearchIndexWriter) {
            database.substanceQueries.selectAll().executeAsList().forEach {
                val searchableText = SearchableText.controlledSubstance(it.toDomain())
                writer(
                    RecordType.ControlledSubstance.wireName,
                    it.patientId,
                    it.id,
                    it.date,
                    searchableText,
                )
            }
        }
    }

    private class WeightIndexer(
        private val database: AnimallyDatabase,
    ) : SearchIndexer {
        override fun reindex(writer: SearchIndexWriter) {
            database.weightQueries.selectAll().executeAsList().forEach {
                val searchableText = SearchableText.weight(it.toDomain())
                writer(
                    RecordType.Weight.wireName,
                    it.patientId,
                    it.id,
                    it.date,
                    searchableText,
                )
            }
        }
    }

    private class ReproductionEventIndexer(
        private val database: AnimallyDatabase,
    ) : SearchIndexer {
        override fun reindex(writer: SearchIndexWriter) {
            database.reproductionQueries.selectAll().executeAsList().forEach {
                val searchableText = SearchableText.reproductionEvent(it.toDomain())
                writer(
                    RecordType.ReproductionEvent.wireName,
                    it.patientId,
                    it.id,
                    it.date,
                    searchableText,
                )
            }
        }
    }

    private class UltrasoundIndexer(
        private val database: AnimallyDatabase,
    ) : SearchIndexer {
        override fun reindex(writer: SearchIndexWriter) {
            database.ultrasoundQueries.selectAll().executeAsList().forEach {
                val searchableText = SearchableText.ultrasound(it.toDomain())
                writer(
                    RecordType.Ultrasound.wireName,
                    it.patientId,
                    it.id,
                    it.date,
                    searchableText,
                )
            }
        }
    }

    private class GestationIndexer(
        private val database: AnimallyDatabase,
    ) : SearchIndexer {
        override fun reindex(writer: SearchIndexWriter) {
            database.gestationQueries.selectAll().executeAsList().forEach {
                val searchableText = SearchableText.gestation(it.toDomain())
                writer(
                    RecordType.Gestation.wireName,
                    it.patientId,
                    it.id,
                    it.breedingDate,
                    searchableText,
                )
            }
        }
    }

    private class ReproMedicationIndexer(
        private val database: AnimallyDatabase,
    ) : SearchIndexer {
        override fun reindex(writer: SearchIndexWriter) {
            database.reproMedicationQueries.selectAll().executeAsList().forEach {
                val searchableText = SearchableText.reproMedication(it.toDomain())
                writer(
                    RecordType.ReproMedication.wireName,
                    it.patientId,
                    it.id,
                    it.dateAdministered,
                    searchableText,
                )
            }
        }
    }

    private class LabResultIndexer(
        private val database: AnimallyDatabase,
    ) : SearchIndexer {
        override fun reindex(writer: SearchIndexWriter) {
            database.labResultQueries.selectAll().executeAsList().forEach {
                val searchableText = SearchableText.labResult(it.toDomain())
                writer(
                    RecordType.LabResult.wireName,
                    it.patientId,
                    it.id,
                    it.date,
                    searchableText,
                )
            }
        }
    }

    private class ImagingIndexer(
        private val database: AnimallyDatabase,
    ) : SearchIndexer {
        override fun reindex(writer: SearchIndexWriter) {
            database.imagingQueries.selectAll().executeAsList().forEach {
                val searchableText = SearchableText.imaging(it.toDomain())
                writer(
                    RecordType.Imaging.wireName,
                    it.patientId,
                    it.id,
                    it.date,
                    searchableText,
                )
            }
        }
    }

    private class EmbryoTransferIndexer(
        private val database: AnimallyDatabase,
    ) : SearchIndexer {
        override fun reindex(writer: SearchIndexWriter) {
            database.embryoTransferQueries.selectAll().executeAsList().forEach {
                val searchableText = SearchableText.embryoTransfer(it.toDomain())
                writer(
                    RecordType.EmbryoTransfer.wireName,
                    it.patientId,
                    it.id,
                    it.date,
                    searchableText,
                )
            }
        }
    }

    private class IcsiIndexer(
        private val database: AnimallyDatabase,
    ) : SearchIndexer {
        override fun reindex(writer: SearchIndexWriter) {
            database.icsiQueries.selectAll().executeAsList().forEach {
                val searchableText = SearchableText.icsi(it.toDomain())
                writer(
                    RecordType.Icsi.wireName,
                    it.patientId,
                    it.id,
                    it.date,
                    searchableText,
                )
            }
        }
    }

    private class CustomReminderIndexer(
        private val database: AnimallyDatabase,
    ) : SearchIndexer {
        override fun reindex(writer: SearchIndexWriter) {
            database.customReminderQueries.selectAllActive().executeAsList().forEach {
                val reminder = it.toDomain()
                writer(
                    RecordType.CustomReminder.wireName,
                    reminder.patientId,
                    reminder.id,
                    reminder.dueDate,
                    SearchableText.customReminder(reminder),
                )
            }
        }
    }
}
