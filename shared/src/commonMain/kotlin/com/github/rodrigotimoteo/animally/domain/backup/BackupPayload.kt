package com.github.rodrigotimoteo.animally.domain.backup

import kotlinx.datetime.LocalDate
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.time.Instant

/**
 * Current backup schema version. Version 1 remains readable, while a newer
 * unsupported version is rejected before restore can mutate local state.
 */
const val BACKUP_SCHEMA_VERSION: Int = 2

/** Oldest JSON schema with a supported reader/migrator. */
const val MIN_SUPPORTED_BACKUP_SCHEMA_VERSION: Int = 1

/** Maximum UTF-8 size accepted by the restore boundary. */
const val MAX_BACKUP_BYTES: Int = 16 * 1024 * 1024

/** Maximum UTF-16 input size accepted by restore editors before decoding. */
const val MAX_BACKUP_INPUT_CHARS: Int = MAX_BACKUP_BYTES / 4

/** Maximum number of rows accepted in any one backup collection. */
const val MAX_BACKUP_COLLECTION_ITEMS: Int = 10_000

/** Maximum number of rows accepted across all backup collections. */
const val MAX_BACKUP_TOTAL_COLLECTION_ITEMS: Int = 100_000

/**
 * Serializable [LocalDate] as an ISO-8601 string (e.g. `2026-08-02`).
 */
internal object LocalDateSerializer : KSerializer<LocalDate> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("BackupLocalDate", PrimitiveKind.STRING)

    override fun serialize(
        encoder: Encoder,
        value: LocalDate,
    ) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): LocalDate = LocalDate.parse(decoder.decodeString())
}

/**
 * Serializable [Instant] as epoch milliseconds (a `Long`).
 */
internal object InstantSerializer : KSerializer<Instant> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("BackupInstant", PrimitiveKind.LONG)

    override fun serialize(
        encoder: Encoder,
        value: Instant,
    ) {
        encoder.encodeLong(value.toEpochMilliseconds())
    }

    override fun deserialize(decoder: Decoder): Instant = Instant.fromEpochMilliseconds(decoder.decodeLong())
}

/**
 * Complete snapshot of the local database, dual-format backup payload.
 *
 * Mirrors every persisted table. DTOs keep their own serializable shapes so
 * the domain models never need to become [Serializable]. Restoring re-inserts
 * every row preserving ids, flags and timestamps.
 */
@Serializable
data class BackupPayload(
    val schemaVersion: Int,
    val exportedAt: String,
    val patients: List<PatientDto>,
    val owners: List<OwnerDto>,
    val anamnese: List<AnamneseDto>,
    val consultations: List<ConsultationDto>,
    val vaccinations: List<VaccinationDto>,
    val weights: List<WeightDto>,
    val dewormings: List<DewormingDto>,
    val dentistry: List<DentistryDto>,
    val lameness: List<LamenessDto>,
    val surgeries: List<SurgeryDto>,
    val medications: List<MedicationDto>,
    val labResults: List<LabResultDto>,
    val imaging: List<ImagingDto>,
    val farrierVisits: List<FarrierVisitDto>,
    val reproductionEvents: List<ReproductionEventDto>,
    val ultrasounds: List<UltrasoundDto>,
    val gestations: List<GestationDto>,
    val reproMedications: List<ReproMedicationDto>,
    val substances: List<ControlledSubstanceDto>,
    // Added after schema v1 shipped; default empty so legacy payloads still deserialize.
    val follicles: List<FollicleDto> = emptyList(),
    val embryoTransfers: List<EmbryoTransferDto> = emptyList(),
    val icsi: List<IcsiDto> = emptyList(),
    val customReminders: List<CustomReminderDto> = emptyList(),
    // Local assistant and dictation metadata. Raw audio remains an app-private
    // file and is not embedded in JSON backups.
    val assistantChatHistory: List<AssistantChatHistoryDto> = emptyList(),
    val dictationCaptures: List<DictationCaptureDto> = emptyList(),
)
