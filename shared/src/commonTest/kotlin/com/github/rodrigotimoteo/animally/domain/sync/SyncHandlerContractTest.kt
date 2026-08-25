package com.github.rodrigotimoteo.animally.domain.sync

import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.data.anamnese.AnamneseRepositoryImpl
import com.github.rodrigotimoteo.animally.data.consultation.ConsultationRepositoryImpl
import com.github.rodrigotimoteo.animally.data.consultation.mapper.toDomain
import com.github.rodrigotimoteo.animally.data.customreminder.CustomReminderRepositoryImpl
import com.github.rodrigotimoteo.animally.data.customreminder.mapper.toDomain
import com.github.rodrigotimoteo.animally.data.dentistry.DentistryRepositoryImpl
import com.github.rodrigotimoteo.animally.data.dentistry.mapper.toDomain
import com.github.rodrigotimoteo.animally.data.deworming.DewormingRepositoryImpl
import com.github.rodrigotimoteo.animally.data.deworming.mapper.toDomain
import com.github.rodrigotimoteo.animally.data.embryotransfer.EmbryoTransferRepositoryImpl
import com.github.rodrigotimoteo.animally.data.embryotransfer.mapper.toDomain
import com.github.rodrigotimoteo.animally.data.farrier.FarrierVisitRepositoryImpl
import com.github.rodrigotimoteo.animally.data.farrier.mapper.toDomain
import com.github.rodrigotimoteo.animally.data.gestation.GestationRepositoryImpl
import com.github.rodrigotimoteo.animally.data.gestation.mapper.toDomain
import com.github.rodrigotimoteo.animally.data.icsi.IcsiRepositoryImpl
import com.github.rodrigotimoteo.animally.data.icsi.mapper.toDomain
import com.github.rodrigotimoteo.animally.data.imaging.ImagingRepositoryImpl
import com.github.rodrigotimoteo.animally.data.imaging.mapper.toDomain
import com.github.rodrigotimoteo.animally.data.labresult.LabResultRepositoryImpl
import com.github.rodrigotimoteo.animally.data.labresult.mapper.toDomain
import com.github.rodrigotimoteo.animally.data.lameness.LamenessRepositoryImpl
import com.github.rodrigotimoteo.animally.data.lameness.mapper.toDomain
import com.github.rodrigotimoteo.animally.data.medication.MedicationRepositoryImpl
import com.github.rodrigotimoteo.animally.data.medication.mapper.toDomain
import com.github.rodrigotimoteo.animally.data.owner.OwnerRepositoryImpl
import com.github.rodrigotimoteo.animally.data.owner.mapper.toDomain
import com.github.rodrigotimoteo.animally.data.patient.PatientRepositoryImpl
import com.github.rodrigotimoteo.animally.data.patient.mapper.toDomain
import com.github.rodrigotimoteo.animally.data.reproduction.ReproductionRepositoryImpl
import com.github.rodrigotimoteo.animally.data.reproduction.mapper.toDomain
import com.github.rodrigotimoteo.animally.data.repromedication.ReproMedicationRepositoryImpl
import com.github.rodrigotimoteo.animally.data.repromedication.mapper.toDomain
import com.github.rodrigotimoteo.animally.data.substance.ControlledSubstanceRepositoryImpl
import com.github.rodrigotimoteo.animally.data.substance.mapper.toDomain
import com.github.rodrigotimoteo.animally.data.surgery.SurgeryRepositoryImpl
import com.github.rodrigotimoteo.animally.data.surgery.mapper.toDomain
import com.github.rodrigotimoteo.animally.data.ultrasound.UltrasoundRepositoryImpl
import com.github.rodrigotimoteo.animally.data.ultrasound.mapper.toDomain
import com.github.rodrigotimoteo.animally.data.vaccination.VaccinationRepositoryImpl
import com.github.rodrigotimoteo.animally.data.vaccination.mapper.toDomain
import com.github.rodrigotimoteo.animally.data.weight.WeightRepositoryImpl
import com.github.rodrigotimoteo.animally.data.weight.mapper.toDomain
import com.github.rodrigotimoteo.animally.di.database.createTestDatabase
import com.github.rodrigotimoteo.animally.domain.anamnese.model.Anamnese
import com.github.rodrigotimoteo.animally.domain.consultation.model.Consultation
import com.github.rodrigotimoteo.animally.domain.customreminder.model.CustomReminder
import com.github.rodrigotimoteo.animally.domain.dentistry.model.Dentistry
import com.github.rodrigotimoteo.animally.domain.deworming.model.Deworming
import com.github.rodrigotimoteo.animally.domain.embryotransfer.model.EmbryoTransfer
import com.github.rodrigotimoteo.animally.domain.farrier.model.FarrierVisit
import com.github.rodrigotimoteo.animally.domain.gestation.model.Gestation
import com.github.rodrigotimoteo.animally.domain.icsi.model.Icsi
import com.github.rodrigotimoteo.animally.domain.imaging.model.Imaging
import com.github.rodrigotimoteo.animally.domain.labresult.model.LabResult
import com.github.rodrigotimoteo.animally.domain.lameness.model.Lameness
import com.github.rodrigotimoteo.animally.domain.medication.model.Medication
import com.github.rodrigotimoteo.animally.domain.owner.model.Owner
import com.github.rodrigotimoteo.animally.domain.patient.model.Patient
import com.github.rodrigotimoteo.animally.domain.reproduction.model.ReproductionEvent
import com.github.rodrigotimoteo.animally.domain.repromedication.model.ReproMedication
import com.github.rodrigotimoteo.animally.domain.substance.model.ControlledSubstance
import com.github.rodrigotimoteo.animally.domain.surgery.model.Surgery
import com.github.rodrigotimoteo.animally.domain.sync.handlers.AnamnesePayload
import com.github.rodrigotimoteo.animally.domain.sync.handlers.AnamneseSyncHandler
import com.github.rodrigotimoteo.animally.domain.sync.handlers.ConsultationPayload
import com.github.rodrigotimoteo.animally.domain.sync.handlers.ConsultationSyncHandler
import com.github.rodrigotimoteo.animally.domain.sync.handlers.CustomReminderPayload
import com.github.rodrigotimoteo.animally.domain.sync.handlers.CustomReminderSyncHandler
import com.github.rodrigotimoteo.animally.domain.sync.handlers.DentistryPayload
import com.github.rodrigotimoteo.animally.domain.sync.handlers.DentistrySyncHandler
import com.github.rodrigotimoteo.animally.domain.sync.handlers.DewormingPayload
import com.github.rodrigotimoteo.animally.domain.sync.handlers.DewormingSyncHandler
import com.github.rodrigotimoteo.animally.domain.sync.handlers.EmbryoTransferPayload
import com.github.rodrigotimoteo.animally.domain.sync.handlers.EmbryoTransferSyncHandler
import com.github.rodrigotimoteo.animally.domain.sync.handlers.FarrierVisitPayload
import com.github.rodrigotimoteo.animally.domain.sync.handlers.FarrierVisitSyncHandler
import com.github.rodrigotimoteo.animally.domain.sync.handlers.GestationPayload
import com.github.rodrigotimoteo.animally.domain.sync.handlers.GestationSyncHandler
import com.github.rodrigotimoteo.animally.domain.sync.handlers.IcsiPayload
import com.github.rodrigotimoteo.animally.domain.sync.handlers.IcsiSyncHandler
import com.github.rodrigotimoteo.animally.domain.sync.handlers.ImagingPayload
import com.github.rodrigotimoteo.animally.domain.sync.handlers.ImagingSyncHandler
import com.github.rodrigotimoteo.animally.domain.sync.handlers.LabResultPayload
import com.github.rodrigotimoteo.animally.domain.sync.handlers.LabResultSyncHandler
import com.github.rodrigotimoteo.animally.domain.sync.handlers.LamenessPayload
import com.github.rodrigotimoteo.animally.domain.sync.handlers.LamenessSyncHandler
import com.github.rodrigotimoteo.animally.domain.sync.handlers.MedicationPayload
import com.github.rodrigotimoteo.animally.domain.sync.handlers.MedicationSyncHandler
import com.github.rodrigotimoteo.animally.domain.sync.handlers.OwnerPayload
import com.github.rodrigotimoteo.animally.domain.sync.handlers.OwnerSyncHandler
import com.github.rodrigotimoteo.animally.domain.sync.handlers.PatientPayload
import com.github.rodrigotimoteo.animally.domain.sync.handlers.PatientSyncHandler
import com.github.rodrigotimoteo.animally.domain.sync.handlers.ReproMedicationPayload
import com.github.rodrigotimoteo.animally.domain.sync.handlers.ReproMedicationSyncHandler
import com.github.rodrigotimoteo.animally.domain.sync.handlers.ReproductionPayload
import com.github.rodrigotimoteo.animally.domain.sync.handlers.ReproductionSyncHandler
import com.github.rodrigotimoteo.animally.domain.sync.handlers.SubstancePayload
import com.github.rodrigotimoteo.animally.domain.sync.handlers.SubstanceSyncHandler
import com.github.rodrigotimoteo.animally.domain.sync.handlers.SurgeryPayload
import com.github.rodrigotimoteo.animally.domain.sync.handlers.SurgerySyncHandler
import com.github.rodrigotimoteo.animally.domain.sync.handlers.SyncJson
import com.github.rodrigotimoteo.animally.domain.sync.handlers.UltrasoundPayload
import com.github.rodrigotimoteo.animally.domain.sync.handlers.UltrasoundSyncHandler
import com.github.rodrigotimoteo.animally.domain.sync.handlers.VaccinationPayload
import com.github.rodrigotimoteo.animally.domain.sync.handlers.VaccinationSyncHandler
import com.github.rodrigotimoteo.animally.domain.sync.handlers.WeightPayload
import com.github.rodrigotimoteo.animally.domain.sync.handlers.WeightSyncHandler
import com.github.rodrigotimoteo.animally.domain.ultrasound.model.Ultrasound
import com.github.rodrigotimoteo.animally.domain.vaccination.model.Vaccination
import com.github.rodrigotimoteo.animally.domain.weight.model.Weight
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.time.Instant

/**
 * Generic handler contract run against EVERY registered [SyncEntityHandler].
 *
 * Per entity the suite verifies:
 *  - pull-apply of a fresh remote record inserts a row with mapped DTO fields,
 *    resolved parent FK and stamped server id;
 *  - remote-newer `updatedAt` wins over a stale local row;
 *  - locally-newer `updatedAt` keeps the local row untouched;
 *  - equal timestamps resolve deterministically to REMOTE WINS (the shared
 *    EntitySyncHandler LWW rule compares with `>=`);
 *  - delete-tombstone replay: a locally deactivated row stays inactive when a
 *    stale ACTIVE record is replayed, and stays inactive after a newer
 *    tombstone is applied.
 *
 * Field mapping is spot-checked on 2-3 signature fields per entity; exhaustive
 * payload coverage lives in the backup lane.
 */
class SyncHandlerContractTest {
    private fun fixtures(): List<HandlerContract> =
        listOf(
            OwnerContract,
            PatientContract,
            AnamneseContract,
            ConsultationContract,
            DentistryContract,
            DewormingContract,
            FarrierVisitContract,
            GestationContract,
            ImagingContract,
            LabResultContract,
            LamenessContract,
            MedicationContract,
            ReproductionContract,
            ReproMedicationContract,
            SubstanceContract,
            SurgeryContract,
            UltrasoundContract,
            VaccinationContract,
            WeightContract,
            CustomReminderContract,
            EmbryoTransferContract,
            IcsiContract,
        )

    private fun record(
        fixture: HandlerContract,
        serverId: String,
        updatedAt: Instant,
        parents: SeededParents,
        isActive: Boolean,
    ) = SyncRecord(
        type = fixture.entityType.wireName,
        serverId = serverId,
        updatedAt = updatedAt,
        isActive = isActive,
        parentServerIds = parents.wire,
        payload = fixture.remotePayload(),
    )

    @Test
    fun `contract - pull apply inserts remote record with mapped fields and stamped serverId`() =
        runTest {
            fixtures().forEach { fixture ->
                val database = createTestDatabase()
                val sut = fixture.createHandler(database)
                val parents = fixture.seedParents(database)
                val serverId = "svc-${fixture.entityType.wireName}-fresh"

                val newId = sut.applyRecord(record(fixture, serverId, TS_REMOTE_NEW, parents, isActive = true))

                assertNotEquals(ENTITY_NOT_APPLIED, newId, "${fixture.entityType}: fresh pull must insert")
                assertEquals(serverId, sut.serverIdOf(newId), "${fixture.entityType}: serverId stamped")
                assertEquals(newId, sut.localIdFor(serverId), "${fixture.entityType}: localIdFor resolves")
                assertEquals(TS_REMOTE_NEW, fixture.rowUpdatedAt(database, newId), "${fixture.entityType}: envelope updatedAt applied")
                assertEquals(
                    fixture.expectedRemoteValues(parents),
                    fixture.actualRowValues(database, newId, parents),
                    "${fixture.entityType}: DTO fields mapped to row",
                )
            }
        }

    @Test
    fun `contract - remote newer updatedAt wins over stale local row`() =
        runTest {
            fixtures().forEach { fixture ->
                val database = createTestDatabase()
                val sut = fixture.createHandler(database)
                val parents = fixture.seedParents(database)
                val localId = fixture.seedLocalRow(database, parents, TS_LOCAL)
                val serverId = "svc-${fixture.entityType.wireName}-newer"
                fixture.attachServerId(database, localId, serverId, TS_LOCAL)

                val result = sut.applyRecord(record(fixture, serverId, TS_REMOTE_NEW, parents, isActive = true))

                assertEquals(localId, result, "${fixture.entityType}: remote-newer updates existing row")
                assertEquals(TS_REMOTE_NEW, fixture.rowUpdatedAt(database, localId), "${fixture.entityType}: updatedAt advanced")
                assertEquals(
                    fixture.expectedRemoteValues(parents),
                    fixture.actualRowValues(database, localId, parents),
                    "${fixture.entityType}: remote payload overwrote local row",
                )
            }
        }

    @Test
    fun `contract - locally newer updatedAt wins over stale remote replay`() =
        runTest {
            fixtures().forEach { fixture ->
                val database = createTestDatabase()
                val sut = fixture.createHandler(database)
                val parents = fixture.seedParents(database)
                val localId = fixture.seedLocalRow(database, parents, TS_LOCAL)
                val serverId = "svc-${fixture.entityType.wireName}-stale"
                fixture.attachServerId(database, localId, serverId, TS_LOCAL)

                val result = sut.applyRecord(record(fixture, serverId, TS_REMOTE_STALE, parents, isActive = true))

                assertEquals(localId, result, "${fixture.entityType}: stale remote keeps row id")
                assertEquals(TS_LOCAL, fixture.rowUpdatedAt(database, localId), "${fixture.entityType}: local updatedAt preserved")
                assertEquals(
                    fixture.expectedLocalValues(parents),
                    fixture.actualRowValues(database, localId, parents),
                    "${fixture.entityType}: stale remote did not overwrite local fields",
                )
            }
        }

    @Test
    fun `contract - equal updatedAt resolves deterministically to remote wins`() =
        runTest {
            // Documented tie-break: EntitySyncHandler.lwwDecision uses >= so the
            // remote record is applied when timestamps are equal.
            fixtures().forEach { fixture ->
                val database = createTestDatabase()
                val sut = fixture.createHandler(database)
                val parents = fixture.seedParents(database)
                val localId = fixture.seedLocalRow(database, parents, TS_LOCAL)
                val serverId = "svc-${fixture.entityType.wireName}-tie"
                fixture.attachServerId(database, localId, serverId, TS_LOCAL)

                val result = sut.applyRecord(record(fixture, serverId, TS_LOCAL, parents, isActive = true))

                assertEquals(localId, result, "${fixture.entityType}: tie keeps row id")
                assertEquals(
                    fixture.expectedRemoteValues(parents),
                    fixture.actualRowValues(database, localId, parents),
                    "${fixture.entityType}: tie applies remote payload",
                )
            }
        }

    @Test
    fun `contract - delete tombstone replay keeps locally deactivated row inactive`() =
        runTest {
            fixtures().filter { it.supportsTombstone }.forEach { fixture ->
                val database = createTestDatabase()
                val sut = fixture.createHandler(database)
                val parents = fixture.seedParents(database)
                val localId = fixture.seedLocalRow(database, parents, TS_LOCAL)
                val serverId = "svc-${fixture.entityType.wireName}-tomb"
                fixture.attachServerId(database, localId, serverId, TS_LOCAL)
                fixture.deactivateLocal(database, localId, TS_TOMBSTONE_LOCAL)
                assertFalse(fixture.rowIsActive(database, localId), "${fixture.entityType}: precondition - row deactivated")

                // Stale ACTIVE replay must not resurrect the deleted row.
                val keptId = sut.applyRecord(record(fixture, serverId, TS_TOMBSTONE_REPLAY, parents, isActive = true))
                assertEquals(localId, keptId, "${fixture.entityType}: stale active replay keeps row id")
                assertFalse(
                    fixture.rowIsActive(database, localId),
                    "${fixture.entityType}: stale active replay must not resurrect deleted row",
                )
                assertEquals(TS_TOMBSTONE_LOCAL, fixture.rowUpdatedAt(database, localId), "${fixture.entityType}: stale replay left updatedAt")

                // Newer tombstone replay applies and the row stays inactive.
                val appliedId = sut.applyRecord(record(fixture, serverId, TS_REMOTE_NEW, parents, isActive = false))
                assertEquals(localId, appliedId, "${fixture.entityType}: newer tombstone applies in place")
                assertFalse(fixture.rowIsActive(database, localId), "${fixture.entityType}: row stays inactive after tombstone")
            }
        }
}

/** Local baseline timestamp used for seeded rows. */
private val TS_LOCAL = Instant.fromEpochMilliseconds(1_000)

/** Remote timestamp strictly newer than [TS_LOCAL]. */
private val TS_REMOTE_NEW = Instant.fromEpochMilliseconds(3_000)

/** Remote timestamp strictly older than [TS_LOCAL]. */
private val TS_REMOTE_STALE = Instant.fromEpochMilliseconds(500)

/** Timestamp written when a row is deactivated locally (newer than any stale replay). */
private val TS_TOMBSTONE_LOCAL = Instant.fromEpochMilliseconds(2_000)

/** Stale replay timestamp that predates the local deactivation. */
private val TS_TOMBSTONE_REPLAY = Instant.fromEpochMilliseconds(1_500)

private val EPOCH = Instant.fromEpochMilliseconds(0)

private const val PARENT_OWNER_SERVER_ID = "svc-owner-parent"

private const val PARENT_PATIENT_SERVER_ID = "svc-patient-parent"

/** Parent ids + wire parent map produced by seeding one owner/patient chain. */
private data class SeededParents(
    val patientId: Long?,
    val wire: Map<String, String?>,
)

/**
 * Per-entity adapter the generic contract suite runs against. One instance per
 * registered [SyncEntityType]; all methods are stateless and receive the
 * scenario's database.
 */
private interface HandlerContract {
    val entityType: SyncEntityType

    /** False when the entity has no soft-delete column (Anamnese). */
    val supportsTombstone: Boolean get() = true

    fun createHandler(database: AnimallyDatabase): SyncEntityHandler

    /** Seeds the parent chain (owner [+ patient]) and stamps their server ids. */
    fun seedParents(database: AnimallyDatabase): SeededParents

    /** Inserts a local row linked to the seeded parents with baseline field values. */
    fun seedLocalRow(
        database: AnimallyDatabase,
        parents: SeededParents,
        updatedAt: Instant,
    ): Long

    fun attachServerId(
        database: AnimallyDatabase,
        entityId: Long,
        serverId: String,
        updatedAt: Instant,
    )

    fun deactivateLocal(
        database: AnimallyDatabase,
        entityId: Long,
        updatedAt: Instant,
    )

    /** Remote payload body carrying values distinct from the local baseline. */
    fun remotePayload(): JsonObject

    /** Signature field values (incl. parent FK) the remote payload must land in the row. */
    fun expectedRemoteValues(parents: SeededParents): Map<String, String?>

    /** Baseline row values right after [seedLocalRow], before any remote apply. */
    fun expectedLocalValues(parents: SeededParents): Map<String, String?>

    fun actualRowValues(
        database: AnimallyDatabase,
        entityId: Long,
        parents: SeededParents,
    ): Map<String, String?>

    fun rowUpdatedAt(
        database: AnimallyDatabase,
        entityId: Long,
    ): Instant

    fun rowIsActive(
        database: AnimallyDatabase,
        entityId: Long,
    ): Boolean
}

/*
 * Fixture row reads go through the generated `selectRowById` query instead of
 * repository getters: repos filter `isActive = 1` by design (UI never sees
 * soft-deleted rows), while this contract must observe deactivated rows after
 * a tombstone apply.
 */

/** Shared parent seeding for handlers whose entity hangs off a Patient. */

private abstract class PatientLinkedContract : HandlerContract {
    override fun seedParents(database: AnimallyDatabase): SeededParents {
        val ownerId =
            OwnerRepositoryImpl(database.ownerQueries, database).insertOwner(
                Owner(
                    id = 0L,
                    name = "Contract Owner",
                    email = null,
                    phone = null,
                    address = null,
                    createdAt = EPOCH,
                    updatedAt = EPOCH,
                ),
            )
        database.ownerQueries.setServerId(PARENT_OWNER_SERVER_ID, EPOCH, ownerId)
        val patientId =
            PatientRepositoryImpl(database).insertPatient(
                Patient(id = 0L, name = "Contract Horse", ownerId = ownerId, createdAt = EPOCH, updatedAt = EPOCH),
            )
        database.patientQueries.setServerId(PARENT_PATIENT_SERVER_ID, EPOCH, patientId)
        return SeededParents(patientId, mapOf("patientId" to PARENT_PATIENT_SERVER_ID))
    }

    protected fun patientIdValue(parents: SeededParents): String = requireNotNull(parents.patientId).toString()

    protected fun seededPatientId(database: AnimallyDatabase): Long =
        requireNotNull(
            database.patientQueries
                .selectByServerId(PARENT_PATIENT_SERVER_ID)
                .executeAsOneOrNull(),
        ) { "seeded patient missing" }.id
}

// ---------------------------------------------------------------------------
// Owner / Patient / Anamnese
// ---------------------------------------------------------------------------

private object OwnerContract : HandlerContract {
    override val entityType = SyncEntityType.OWNER

    private fun repo(database: AnimallyDatabase) = OwnerRepositoryImpl(database.ownerQueries, database)

    private fun row(
        database: AnimallyDatabase,
        entityId: Long,
    ): Owner =
        requireNotNull(
            database.ownerQueries
                .selectRowById(entityId)
                .executeAsOneOrNull()
                ?.toDomain(),
        ) { "Owner $entityId missing" }

    override fun createHandler(database: AnimallyDatabase) = OwnerSyncHandler(repo(database), database)

    override fun seedParents(database: AnimallyDatabase) = SeededParents(null, emptyMap())

    override fun seedLocalRow(
        database: AnimallyDatabase,
        parents: SeededParents,
        updatedAt: Instant,
    ) = repo(database).insertOwner(
        Owner(
            id = 0L,
            name = "Local Owner",
            email = "local@example.com",
            phone = null,
            address = "Local Address",
            createdAt = EPOCH,
            updatedAt = updatedAt,
        ),
    )

    override fun attachServerId(
        database: AnimallyDatabase,
        entityId: Long,
        serverId: String,
        updatedAt: Instant,
    ) {
        database.ownerQueries.setServerId(serverId, updatedAt, entityId)
    }

    override fun deactivateLocal(
        database: AnimallyDatabase,
        entityId: Long,
        updatedAt: Instant,
    ) {
        repo(database).setInactive(entityId, updatedAt)
    }

    override fun remotePayload() =
        SyncJson
            .encodeToJsonElement(
                OwnerPayload.serializer(),
                OwnerPayload(name = "Remote Owner", email = "remote@example.com", address = "Remote Address"),
            ).jsonObject

    override fun expectedRemoteValues(parents: SeededParents) =
        mapOf(
            "name" to "Remote Owner",
            "email" to "remote@example.com",
            "address" to "Remote Address",
        )

    override fun expectedLocalValues(parents: SeededParents) =
        mapOf(
            "name" to "Local Owner",
            "email" to "local@example.com",
            "address" to "Local Address",
        )

    override fun actualRowValues(
        database: AnimallyDatabase,
        entityId: Long,
        parents: SeededParents,
    ) = row(database, entityId).let { mapOf("name" to it.name, "email" to it.email, "address" to it.address) }

    override fun rowUpdatedAt(
        database: AnimallyDatabase,
        entityId: Long,
    ) = row(database, entityId).updatedAt

    override fun rowIsActive(
        database: AnimallyDatabase,
        entityId: Long,
    ) = row(database, entityId).isActive
}

private object PatientContract : HandlerContract {
    override val entityType = SyncEntityType.PATIENT

    private fun repo(database: AnimallyDatabase) = PatientRepositoryImpl(database)

    private fun row(
        database: AnimallyDatabase,
        entityId: Long,
    ): Patient =
        requireNotNull(
            database.patientQueries
                .selectRowById(entityId)
                .executeAsOneOrNull()
                ?.toDomain(),
        ) { "Patient $entityId missing" }

    override fun createHandler(database: AnimallyDatabase) = PatientSyncHandler(repo(database), database)

    override fun seedParents(database: AnimallyDatabase): SeededParents {
        val ownerId =
            OwnerRepositoryImpl(database.ownerQueries, database).insertOwner(
                Owner(
                    id = 0L,
                    name = "Contract Owner",
                    email = null,
                    phone = null,
                    address = null,
                    createdAt = EPOCH,
                    updatedAt = EPOCH,
                ),
            )
        database.ownerQueries.setServerId(PARENT_OWNER_SERVER_ID, EPOCH, ownerId)
        return SeededParents(null, mapOf("ownerId" to PARENT_OWNER_SERVER_ID))
    }

    override fun seedLocalRow(
        database: AnimallyDatabase,
        parents: SeededParents,
        updatedAt: Instant,
    ): Long {
        val ownerId =
            requireNotNull(
                database.ownerQueries
                    .selectByServerId(PARENT_OWNER_SERVER_ID)
                    .executeAsOneOrNull(),
            ) { "seeded owner missing" }.id
        return repo(database).insertPatient(
            Patient(
                id = 0L,
                name = "Local Horse",
                microchipId = "MC-LOCAL-1",
                ownerId = ownerId,
                createdAt = EPOCH,
                updatedAt = updatedAt,
            ),
        )
    }

    override fun attachServerId(
        database: AnimallyDatabase,
        entityId: Long,
        serverId: String,
        updatedAt: Instant,
    ) {
        database.patientQueries.setServerId(serverId, updatedAt, entityId)
    }

    override fun deactivateLocal(
        database: AnimallyDatabase,
        entityId: Long,
        updatedAt: Instant,
    ) {
        repo(database).setInactive(entityId, updatedAt)
    }

    override fun remotePayload() =
        SyncJson
            .encodeToJsonElement(
                PatientPayload.serializer(),
                PatientPayload(name = "Remote Horse", microchipId = "MC-REMOTE-1", ueln = "UELN-REMOTE-1"),
            ).jsonObject

    override fun expectedRemoteValues(parents: SeededParents) =
        mapOf(
            "name" to "Remote Horse",
            "microchipId" to "MC-REMOTE-1",
            "ueln" to "UELN-REMOTE-1",
        )

    override fun expectedLocalValues(parents: SeededParents) =
        mapOf(
            "name" to "Local Horse",
            "microchipId" to "MC-LOCAL-1",
            "ueln" to null,
        )

    override fun actualRowValues(
        database: AnimallyDatabase,
        entityId: Long,
        parents: SeededParents,
    ) = row(database, entityId).let { mapOf("name" to it.name, "microchipId" to it.microchipId, "ueln" to it.ueln) }

    override fun rowUpdatedAt(
        database: AnimallyDatabase,
        entityId: Long,
    ) = row(database, entityId).updatedAt

    override fun rowIsActive(
        database: AnimallyDatabase,
        entityId: Long,
    ) = row(database, entityId).isActive
}

private object AnamneseContract : PatientLinkedContract() {
    override val entityType = SyncEntityType.ANAMNESE

    // Anamnese has no soft-delete column; its handler hardcodes isActive=true.
    override val supportsTombstone = false

    private fun repo(database: AnimallyDatabase) = AnamneseRepositoryImpl(database)

    private fun patientIdFor(
        database: AnimallyDatabase,
        entityId: Long,
    ): Long =
        requireNotNull(
            database.anamneseQueries
                .selectAllRows()
                .executeAsList()
                .firstOrNull { it.id == entityId },
        ) { "Anamnese $entityId missing" }.patientId

    private fun row(
        database: AnimallyDatabase,
        entityId: Long,
    ): Anamnese = requireNotNull(repo(database).getByPatient(patientIdFor(database, entityId))) { "Anamnese $entityId missing" }

    override fun createHandler(database: AnimallyDatabase) = AnamneseSyncHandler(repo(database), PatientRepositoryImpl(database), database)

    override fun seedLocalRow(
        database: AnimallyDatabase,
        parents: SeededParents,
        updatedAt: Instant,
    ) = repo(database).save(
        Anamnese(
            id = 0L,
            patientId = seededPatientId(database),
            generalHistory = "local history",
            chronicConditions = "local chronic",
            allergies = "none",
            createdAt = EPOCH,
            updatedAt = updatedAt,
        ),
    )

    override fun attachServerId(
        database: AnimallyDatabase,
        entityId: Long,
        serverId: String,
        updatedAt: Instant,
    ) {
        database.anamneseQueries.setServerId(serverId, updatedAt, entityId)
    }

    override fun deactivateLocal(
        database: AnimallyDatabase,
        entityId: Long,
        updatedAt: Instant,
    ) {
        error("Anamnese has no soft delete")
    }

    override fun remotePayload() =
        SyncJson
            .encodeToJsonElement(
                AnamnesePayload.serializer(),
                AnamnesePayload(generalHistory = "remote history", chronicConditions = "remote chronic", allergies = "penicillin"),
            ).jsonObject

    override fun expectedRemoteValues(parents: SeededParents) =
        mapOf(
            "patientId" to patientIdValue(parents),
            "generalHistory" to "remote history",
            "allergies" to "penicillin",
        )

    override fun expectedLocalValues(parents: SeededParents) =
        mapOf(
            "patientId" to patientIdValue(parents),
            "generalHistory" to "local history",
            "allergies" to "none",
        )

    override fun actualRowValues(
        database: AnimallyDatabase,
        entityId: Long,
        parents: SeededParents,
    ) = row(database, entityId).let {
        mapOf(
            "patientId" to it.patientId.toString(),
            "generalHistory" to it.generalHistory,
            "allergies" to it.allergies,
        )
    }

    override fun rowUpdatedAt(
        database: AnimallyDatabase,
        entityId: Long,
    ) = row(database, entityId).updatedAt

    override fun rowIsActive(
        database: AnimallyDatabase,
        entityId: Long,
    ) = true
}

// ---------------------------------------------------------------------------
// Patient-linked clinical records
// ---------------------------------------------------------------------------

private object ConsultationContract : PatientLinkedContract() {
    override val entityType = SyncEntityType.CONSULTATION

    private fun repo(database: AnimallyDatabase) = ConsultationRepositoryImpl(database)

    private fun row(
        database: AnimallyDatabase,
        entityId: Long,
    ): Consultation =
        requireNotNull(
            database.consultationQueries
                .selectRowById(entityId)
                .executeAsOneOrNull()
                ?.toDomain(),
        ) { "Consultation $entityId missing" }

    override fun createHandler(database: AnimallyDatabase) = ConsultationSyncHandler(repo(database), PatientRepositoryImpl(database), database)

    override fun seedLocalRow(
        database: AnimallyDatabase,
        parents: SeededParents,
        updatedAt: Instant,
    ) = repo(database).insert(
        Consultation(
            id = 0L,
            patientId = seededPatientId(database),
            date = LocalDate(2024, 5, 1),
            subjective = "L-subj",
            objective = "L-obj",
            assessment = "L-assess",
            plan = "L-plan",
            createdAt = EPOCH,
            updatedAt = updatedAt,
        ),
    )

    override fun attachServerId(
        database: AnimallyDatabase,
        entityId: Long,
        serverId: String,
        updatedAt: Instant,
    ) {
        database.consultationQueries.setServerId(serverId, updatedAt, entityId)
    }

    override fun deactivateLocal(
        database: AnimallyDatabase,
        entityId: Long,
        updatedAt: Instant,
    ) {
        repo(database).setInactive(entityId, updatedAt)
    }

    override fun remotePayload() =
        SyncJson
            .encodeToJsonElement(
                ConsultationPayload.serializer(),
                ConsultationPayload(
                    date = LocalDate(2024, 7, 1),
                    subjective = "R-subj",
                    objective = "R-obj",
                    assessment = "R-assess",
                    plan = "R-plan",
                    vetName = "Dr. Remote",
                ),
            ).jsonObject

    override fun expectedRemoteValues(parents: SeededParents) =
        mapOf(
            "patientId" to patientIdValue(parents),
            "subjective" to "R-subj",
            "plan" to "R-plan",
            "vetName" to "Dr. Remote",
        )

    override fun expectedLocalValues(parents: SeededParents) =
        mapOf(
            "patientId" to patientIdValue(parents),
            "subjective" to "L-subj",
            "plan" to "L-plan",
            "vetName" to null,
        )

    override fun actualRowValues(
        database: AnimallyDatabase,
        entityId: Long,
        parents: SeededParents,
    ) = row(database, entityId).let {
        mapOf(
            "patientId" to it.patientId.toString(),
            "subjective" to it.subjective,
            "plan" to it.plan,
            "vetName" to it.vetName,
        )
    }

    override fun rowUpdatedAt(
        database: AnimallyDatabase,
        entityId: Long,
    ) = row(database, entityId).updatedAt

    override fun rowIsActive(
        database: AnimallyDatabase,
        entityId: Long,
    ) = row(database, entityId).isActive
}

private object DentistryContract : PatientLinkedContract() {
    override val entityType = SyncEntityType.DENTISTRY

    private fun repo(database: AnimallyDatabase) = DentistryRepositoryImpl(database)

    private fun row(
        database: AnimallyDatabase,
        entityId: Long,
    ) = requireNotNull(
        database.dentistryQueries
            .selectRowById(entityId)
            .executeAsOneOrNull()
            ?.toDomain(),
    ) { "Dentistry $entityId missing" }

    override fun createHandler(database: AnimallyDatabase) = DentistrySyncHandler(repo(database), PatientRepositoryImpl(database), database)

    override fun seedLocalRow(
        database: AnimallyDatabase,
        parents: SeededParents,
        updatedAt: Instant,
    ) = repo(database).insert(
        Dentistry(
            id = 0L,
            patientId = seededPatientId(database),
            date = LocalDate(2024, 5, 1),
            findings = "L-findings",
            treatment = "L-treatment",
            createdAt = EPOCH,
            updatedAt = updatedAt,
        ),
    )

    override fun attachServerId(
        database: AnimallyDatabase,
        entityId: Long,
        serverId: String,
        updatedAt: Instant,
    ) {
        database.dentistryQueries.setServerId(serverId, updatedAt, entityId)
    }

    override fun deactivateLocal(
        database: AnimallyDatabase,
        entityId: Long,
        updatedAt: Instant,
    ) {
        repo(database).setInactive(entityId, updatedAt)
    }

    override fun remotePayload() =
        SyncJson
            .encodeToJsonElement(
                DentistryPayload.serializer(),
                DentistryPayload(date = LocalDate(2024, 7, 1), findings = "R-findings", treatment = "R-treatment"),
            ).jsonObject

    override fun expectedRemoteValues(parents: SeededParents) =
        mapOf(
            "patientId" to patientIdValue(parents),
            "findings" to "R-findings",
            "treatment" to "R-treatment",
        )

    override fun expectedLocalValues(parents: SeededParents) =
        mapOf(
            "patientId" to patientIdValue(parents),
            "findings" to "L-findings",
            "treatment" to "L-treatment",
        )

    override fun actualRowValues(
        database: AnimallyDatabase,
        entityId: Long,
        parents: SeededParents,
    ) = row(database, entityId).let {
        mapOf(
            "patientId" to it.patientId.toString(),
            "findings" to it.findings,
            "treatment" to it.treatment,
        )
    }

    override fun rowUpdatedAt(
        database: AnimallyDatabase,
        entityId: Long,
    ) = row(database, entityId).updatedAt

    override fun rowIsActive(
        database: AnimallyDatabase,
        entityId: Long,
    ) = row(database, entityId).isActive
}

private object DewormingContract : PatientLinkedContract() {
    override val entityType = SyncEntityType.DEWORMING

    private fun repo(database: AnimallyDatabase) = DewormingRepositoryImpl(database)

    private fun row(
        database: AnimallyDatabase,
        entityId: Long,
    ) = requireNotNull(
        database.dewormingQueries
            .selectRowById(entityId)
            .executeAsOneOrNull()
            ?.toDomain(),
    ) { "Deworming $entityId missing" }

    override fun createHandler(database: AnimallyDatabase) = DewormingSyncHandler(repo(database), PatientRepositoryImpl(database), database)

    override fun seedLocalRow(
        database: AnimallyDatabase,
        parents: SeededParents,
        updatedAt: Instant,
    ) = repo(database).insert(
        Deworming(
            id = 0L,
            patientId = seededPatientId(database),
            product = "Ivermectin-L",
            dateAdministered = LocalDate(2024, 5, 1),
            dose = "10ml-L",
            createdAt = EPOCH,
            updatedAt = updatedAt,
        ),
    )

    override fun attachServerId(
        database: AnimallyDatabase,
        entityId: Long,
        serverId: String,
        updatedAt: Instant,
    ) {
        database.dewormingQueries.setServerId(serverId, updatedAt, entityId)
    }

    override fun deactivateLocal(
        database: AnimallyDatabase,
        entityId: Long,
        updatedAt: Instant,
    ) {
        repo(database).setInactive(entityId, updatedAt)
    }

    override fun remotePayload() =
        SyncJson
            .encodeToJsonElement(
                DewormingPayload.serializer(),
                DewormingPayload(product = "Ivermectin-R", dateAdministered = LocalDate(2024, 7, 1), dose = "10ml-R"),
            ).jsonObject

    override fun expectedRemoteValues(parents: SeededParents) =
        mapOf(
            "patientId" to patientIdValue(parents),
            "product" to "Ivermectin-R",
            "dose" to "10ml-R",
        )

    override fun expectedLocalValues(parents: SeededParents) =
        mapOf(
            "patientId" to patientIdValue(parents),
            "product" to "Ivermectin-L",
            "dose" to "10ml-L",
        )

    override fun actualRowValues(
        database: AnimallyDatabase,
        entityId: Long,
        parents: SeededParents,
    ) = row(database, entityId).let {
        mapOf(
            "patientId" to it.patientId.toString(),
            "product" to it.product,
            "dose" to it.dose,
        )
    }

    override fun rowUpdatedAt(
        database: AnimallyDatabase,
        entityId: Long,
    ) = row(database, entityId).updatedAt

    override fun rowIsActive(
        database: AnimallyDatabase,
        entityId: Long,
    ) = row(database, entityId).isActive
}

private object FarrierVisitContract : PatientLinkedContract() {
    override val entityType = SyncEntityType.FARRIER_VISIT

    private fun repo(database: AnimallyDatabase) = FarrierVisitRepositoryImpl(database)

    private fun row(
        database: AnimallyDatabase,
        entityId: Long,
    ) = requireNotNull(
        database.farrierVisitQueries
            .selectRowById(entityId)
            .executeAsOneOrNull()
            ?.toDomain(),
    ) { "FarrierVisit $entityId missing" }

    override fun createHandler(database: AnimallyDatabase) = FarrierVisitSyncHandler(repo(database), PatientRepositoryImpl(database), database)

    override fun seedLocalRow(
        database: AnimallyDatabase,
        parents: SeededParents,
        updatedAt: Instant,
    ) = repo(database).insert(
        FarrierVisit(
            id = 0L,
            patientId = seededPatientId(database),
            date = LocalDate(2024, 5, 1),
            trimOrShoe = "Trim-L",
            farrier = "Farrier-L",
            createdAt = EPOCH,
            updatedAt = updatedAt,
        ),
    )

    override fun attachServerId(
        database: AnimallyDatabase,
        entityId: Long,
        serverId: String,
        updatedAt: Instant,
    ) {
        database.farrierVisitQueries.setServerId(serverId, updatedAt, entityId)
    }

    override fun deactivateLocal(
        database: AnimallyDatabase,
        entityId: Long,
        updatedAt: Instant,
    ) {
        repo(database).setInactive(entityId, updatedAt)
    }

    override fun remotePayload() =
        SyncJson
            .encodeToJsonElement(
                FarrierVisitPayload.serializer(),
                FarrierVisitPayload(date = LocalDate(2024, 7, 1), trimOrShoe = "Shoe-R", shoeType = "Steel-R", farrier = "Farrier-R"),
            ).jsonObject

    override fun expectedRemoteValues(parents: SeededParents) =
        mapOf(
            "patientId" to patientIdValue(parents),
            "trimOrShoe" to "Shoe-R",
            "shoeType" to "Steel-R",
            "farrier" to "Farrier-R",
        )

    override fun expectedLocalValues(parents: SeededParents) =
        mapOf(
            "patientId" to patientIdValue(parents),
            "trimOrShoe" to "Trim-L",
            "shoeType" to null,
            "farrier" to "Farrier-L",
        )

    override fun actualRowValues(
        database: AnimallyDatabase,
        entityId: Long,
        parents: SeededParents,
    ) = row(database, entityId).let {
        mapOf(
            "patientId" to it.patientId.toString(),
            "trimOrShoe" to it.trimOrShoe,
            "shoeType" to it.shoeType,
            "farrier" to it.farrier,
        )
    }

    override fun rowUpdatedAt(
        database: AnimallyDatabase,
        entityId: Long,
    ) = row(database, entityId).updatedAt

    override fun rowIsActive(
        database: AnimallyDatabase,
        entityId: Long,
    ) = row(database, entityId).isActive
}

private object GestationContract : PatientLinkedContract() {
    override val entityType = SyncEntityType.GESTATION

    private fun repo(database: AnimallyDatabase) = GestationRepositoryImpl(database)

    private fun row(
        database: AnimallyDatabase,
        entityId: Long,
    ) = requireNotNull(
        database.gestationQueries
            .selectRowById(entityId)
            .executeAsOneOrNull()
            ?.toDomain(),
    ) { "Gestation $entityId missing" }

    override fun createHandler(database: AnimallyDatabase) = GestationSyncHandler(repo(database), PatientRepositoryImpl(database), database)

    override fun seedLocalRow(
        database: AnimallyDatabase,
        parents: SeededParents,
        updatedAt: Instant,
    ) = repo(database).insert(
        Gestation(
            id = 0L,
            patientId = seededPatientId(database),
            breedingDate = LocalDate(2024, 2, 1),
            expectedDueDate = LocalDate(2025, 1, 6),
            gestationDays = 10,
            status = "InFoal-L",
            createdAt = EPOCH,
            updatedAt = updatedAt,
        ),
    )

    override fun attachServerId(
        database: AnimallyDatabase,
        entityId: Long,
        serverId: String,
        updatedAt: Instant,
    ) {
        database.gestationQueries.setServerId(serverId, updatedAt, entityId)
    }

    override fun deactivateLocal(
        database: AnimallyDatabase,
        entityId: Long,
        updatedAt: Instant,
    ) {
        repo(database).setInactive(entityId, updatedAt)
    }

    override fun remotePayload() =
        SyncJson
            .encodeToJsonElement(
                GestationPayload.serializer(),
                GestationPayload(
                    breedingDate = LocalDate(2024, 3, 1),
                    expectedDueDate = LocalDate(2025, 2, 4),
                    gestationDays = 30,
                    status = "InFoal-R",
                    fetalCount = 1,
                ),
            ).jsonObject

    override fun expectedRemoteValues(parents: SeededParents) =
        mapOf(
            "patientId" to patientIdValue(parents),
            "gestationDays" to "30",
            "status" to "InFoal-R",
            "fetalCount" to "1",
        )

    override fun expectedLocalValues(parents: SeededParents) =
        mapOf(
            "patientId" to patientIdValue(parents),
            "gestationDays" to "10",
            "status" to "InFoal-L",
            "fetalCount" to null,
        )

    override fun actualRowValues(
        database: AnimallyDatabase,
        entityId: Long,
        parents: SeededParents,
    ) = row(database, entityId).let {
        mapOf(
            "patientId" to it.patientId.toString(),
            "gestationDays" to it.gestationDays.toString(),
            "status" to it.status,
            "fetalCount" to it.fetalCount?.toString(),
        )
    }

    override fun rowUpdatedAt(
        database: AnimallyDatabase,
        entityId: Long,
    ) = row(database, entityId).updatedAt

    override fun rowIsActive(
        database: AnimallyDatabase,
        entityId: Long,
    ) = row(database, entityId).isActive
}

private object ImagingContract : PatientLinkedContract() {
    override val entityType = SyncEntityType.IMAGING

    private fun repo(database: AnimallyDatabase) = ImagingRepositoryImpl(database)

    private fun row(
        database: AnimallyDatabase,
        entityId: Long,
    ) = requireNotNull(
        database.imagingQueries
            .selectRowById(entityId)
            .executeAsOneOrNull()
            ?.toDomain(),
    ) { "Imaging $entityId missing" }

    override fun createHandler(database: AnimallyDatabase) = ImagingSyncHandler(repo(database), PatientRepositoryImpl(database), database)

    override fun seedLocalRow(
        database: AnimallyDatabase,
        parents: SeededParents,
        updatedAt: Instant,
    ) = repo(database).insert(
        Imaging(
            id = 0L,
            patientId = seededPatientId(database),
            type = "Radiograph-L",
            date = LocalDate(2024, 5, 1),
            findings = "L-findings",
            createdAt = EPOCH,
            updatedAt = updatedAt,
        ),
    )

    override fun attachServerId(
        database: AnimallyDatabase,
        entityId: Long,
        serverId: String,
        updatedAt: Instant,
    ) {
        database.imagingQueries.setServerId(serverId, updatedAt, entityId)
    }

    override fun deactivateLocal(
        database: AnimallyDatabase,
        entityId: Long,
        updatedAt: Instant,
    ) {
        repo(database).setInactive(entityId, updatedAt)
    }

    override fun remotePayload() =
        SyncJson
            .encodeToJsonElement(
                ImagingPayload.serializer(),
                ImagingPayload(type = "Radiograph-R", date = LocalDate(2024, 7, 1), findings = "R-findings"),
            ).jsonObject

    override fun expectedRemoteValues(parents: SeededParents) =
        mapOf(
            "patientId" to patientIdValue(parents),
            "type" to "Radiograph-R",
            "findings" to "R-findings",
        )

    override fun expectedLocalValues(parents: SeededParents) =
        mapOf(
            "patientId" to patientIdValue(parents),
            "type" to "Radiograph-L",
            "findings" to "L-findings",
        )

    override fun actualRowValues(
        database: AnimallyDatabase,
        entityId: Long,
        parents: SeededParents,
    ) = row(database, entityId).let {
        mapOf(
            "patientId" to it.patientId.toString(),
            "type" to it.type,
            "findings" to it.findings,
        )
    }

    override fun rowUpdatedAt(
        database: AnimallyDatabase,
        entityId: Long,
    ) = row(database, entityId).updatedAt

    override fun rowIsActive(
        database: AnimallyDatabase,
        entityId: Long,
    ) = row(database, entityId).isActive
}

private object LabResultContract : PatientLinkedContract() {
    override val entityType = SyncEntityType.LAB_RESULT

    private fun repo(database: AnimallyDatabase) = LabResultRepositoryImpl(database)

    private fun row(
        database: AnimallyDatabase,
        entityId: Long,
    ) = requireNotNull(
        database.labResultQueries
            .selectRowById(entityId)
            .executeAsOneOrNull()
            ?.toDomain(),
    ) { "LabResult $entityId missing" }

    override fun createHandler(database: AnimallyDatabase) = LabResultSyncHandler(repo(database), PatientRepositoryImpl(database), database)

    override fun seedLocalRow(
        database: AnimallyDatabase,
        parents: SeededParents,
        updatedAt: Instant,
    ) = repo(database).insert(
        LabResult(
            id = 0L,
            patientId = seededPatientId(database),
            testType = "CBC-L",
            date = LocalDate(2024, 5, 1),
            results = "L-results",
            createdAt = EPOCH,
            updatedAt = updatedAt,
        ),
    )

    override fun attachServerId(
        database: AnimallyDatabase,
        entityId: Long,
        serverId: String,
        updatedAt: Instant,
    ) {
        database.labResultQueries.setServerId(serverId, updatedAt, entityId)
    }

    override fun deactivateLocal(
        database: AnimallyDatabase,
        entityId: Long,
        updatedAt: Instant,
    ) {
        repo(database).setInactive(entityId, updatedAt)
    }

    override fun remotePayload() =
        SyncJson
            .encodeToJsonElement(
                LabResultPayload.serializer(),
                LabResultPayload(testType = "CBC-R", date = LocalDate(2024, 7, 1), results = "R-results"),
            ).jsonObject

    override fun expectedRemoteValues(parents: SeededParents) =
        mapOf(
            "patientId" to patientIdValue(parents),
            "testType" to "CBC-R",
            "results" to "R-results",
        )

    override fun expectedLocalValues(parents: SeededParents) =
        mapOf(
            "patientId" to patientIdValue(parents),
            "testType" to "CBC-L",
            "results" to "L-results",
        )

    override fun actualRowValues(
        database: AnimallyDatabase,
        entityId: Long,
        parents: SeededParents,
    ) = row(database, entityId).let {
        mapOf(
            "patientId" to it.patientId.toString(),
            "testType" to it.testType,
            "results" to it.results,
        )
    }

    override fun rowUpdatedAt(
        database: AnimallyDatabase,
        entityId: Long,
    ) = row(database, entityId).updatedAt

    override fun rowIsActive(
        database: AnimallyDatabase,
        entityId: Long,
    ) = row(database, entityId).isActive
}

private object LamenessContract : PatientLinkedContract() {
    override val entityType = SyncEntityType.LAMENESS

    private fun repo(database: AnimallyDatabase) = LamenessRepositoryImpl(database)

    private fun row(
        database: AnimallyDatabase,
        entityId: Long,
    ) = requireNotNull(
        database.lamenessQueries
            .selectRowById(entityId)
            .executeAsOneOrNull()
            ?.toDomain(),
    ) { "Lameness $entityId missing" }

    override fun createHandler(database: AnimallyDatabase) = LamenessSyncHandler(repo(database), PatientRepositoryImpl(database), database)

    override fun seedLocalRow(
        database: AnimallyDatabase,
        parents: SeededParents,
        updatedAt: Instant,
    ) = repo(database).insert(
        Lameness(
            id = 0L,
            patientId = seededPatientId(database),
            date = LocalDate(2024, 5, 1),
            gradeAAEP = 2,
            limbLocation = "RF-L",
            createdAt = EPOCH,
            updatedAt = updatedAt,
        ),
    )

    override fun attachServerId(
        database: AnimallyDatabase,
        entityId: Long,
        serverId: String,
        updatedAt: Instant,
    ) {
        database.lamenessQueries.setServerId(serverId, updatedAt, entityId)
    }

    override fun deactivateLocal(
        database: AnimallyDatabase,
        entityId: Long,
        updatedAt: Instant,
    ) {
        repo(database).setInactive(entityId, updatedAt)
    }

    override fun remotePayload() =
        SyncJson
            .encodeToJsonElement(
                LamenessPayload.serializer(),
                LamenessPayload(date = LocalDate(2024, 7, 1), gradeAAEP = 3, limbLocation = "RF-R", flexionTest = "Positive-R"),
            ).jsonObject

    override fun expectedRemoteValues(parents: SeededParents) =
        mapOf(
            "patientId" to patientIdValue(parents),
            "gradeAAEP" to "3",
            "limbLocation" to "RF-R",
            "flexionTest" to "Positive-R",
        )

    override fun expectedLocalValues(parents: SeededParents) =
        mapOf(
            "patientId" to patientIdValue(parents),
            "gradeAAEP" to "2",
            "limbLocation" to "RF-L",
            "flexionTest" to null,
        )

    override fun actualRowValues(
        database: AnimallyDatabase,
        entityId: Long,
        parents: SeededParents,
    ) = row(database, entityId).let {
        mapOf(
            "patientId" to it.patientId.toString(),
            "gradeAAEP" to it.gradeAAEP.toString(),
            "limbLocation" to it.limbLocation,
            "flexionTest" to it.flexionTest,
        )
    }

    override fun rowUpdatedAt(
        database: AnimallyDatabase,
        entityId: Long,
    ) = row(database, entityId).updatedAt

    override fun rowIsActive(
        database: AnimallyDatabase,
        entityId: Long,
    ) = row(database, entityId).isActive
}

private object MedicationContract : PatientLinkedContract() {
    override val entityType = SyncEntityType.MEDICATION

    private fun repo(database: AnimallyDatabase) = MedicationRepositoryImpl(database)

    private fun row(
        database: AnimallyDatabase,
        entityId: Long,
    ) = requireNotNull(
        database.medicationQueries
            .selectRowById(entityId)
            .executeAsOneOrNull()
            ?.toDomain(),
    ) { "Medication $entityId missing" }

    override fun createHandler(database: AnimallyDatabase) = MedicationSyncHandler(repo(database), PatientRepositoryImpl(database), database)

    override fun seedLocalRow(
        database: AnimallyDatabase,
        parents: SeededParents,
        updatedAt: Instant,
    ) = repo(database).insert(
        Medication(
            id = 0L,
            patientId = seededPatientId(database),
            name = "Bute-L",
            dosage = "5g-L",
            createdAt = EPOCH,
            updatedAt = updatedAt,
        ),
    )

    override fun attachServerId(
        database: AnimallyDatabase,
        entityId: Long,
        serverId: String,
        updatedAt: Instant,
    ) {
        database.medicationQueries.setServerId(serverId, updatedAt, entityId)
    }

    override fun deactivateLocal(
        database: AnimallyDatabase,
        entityId: Long,
        updatedAt: Instant,
    ) {
        repo(database).setInactive(entityId, updatedAt)
    }

    override fun remotePayload() =
        SyncJson
            .encodeToJsonElement(
                MedicationPayload.serializer(),
                MedicationPayload(name = "Bute-R", dosage = "5g-R"),
            ).jsonObject

    override fun expectedRemoteValues(parents: SeededParents) =
        mapOf(
            "patientId" to patientIdValue(parents),
            "name" to "Bute-R",
            "dosage" to "5g-R",
        )

    override fun expectedLocalValues(parents: SeededParents) =
        mapOf(
            "patientId" to patientIdValue(parents),
            "name" to "Bute-L",
            "dosage" to "5g-L",
        )

    override fun actualRowValues(
        database: AnimallyDatabase,
        entityId: Long,
        parents: SeededParents,
    ) = row(database, entityId).let {
        mapOf(
            "patientId" to it.patientId.toString(),
            "name" to it.name,
            "dosage" to it.dosage,
        )
    }

    override fun rowUpdatedAt(
        database: AnimallyDatabase,
        entityId: Long,
    ) = row(database, entityId).updatedAt

    override fun rowIsActive(
        database: AnimallyDatabase,
        entityId: Long,
    ) = row(database, entityId).isActive
}

private object ReproductionContract : PatientLinkedContract() {
    override val entityType = SyncEntityType.REPRODUCTION

    private fun repo(database: AnimallyDatabase) = ReproductionRepositoryImpl(database)

    private fun row(
        database: AnimallyDatabase,
        entityId: Long,
    ) = requireNotNull(
        database.reproductionQueries
            .selectRowById(entityId)
            .executeAsOneOrNull()
            ?.toDomain(),
    ) { "Reproduction $entityId missing" }

    override fun createHandler(database: AnimallyDatabase) = ReproductionSyncHandler(repo(database), PatientRepositoryImpl(database), database)

    override fun seedLocalRow(
        database: AnimallyDatabase,
        parents: SeededParents,
        updatedAt: Instant,
    ) = repo(database).insert(
        ReproductionEvent(
            id = 0L,
            patientId = seededPatientId(database),
            eventType = "Heat-L",
            date = LocalDate(2024, 5, 1),
            details = "L-details",
            createdAt = EPOCH,
            updatedAt = updatedAt,
        ),
    )

    override fun attachServerId(
        database: AnimallyDatabase,
        entityId: Long,
        serverId: String,
        updatedAt: Instant,
    ) {
        database.reproductionQueries.setServerId(serverId, updatedAt, entityId)
    }

    override fun deactivateLocal(
        database: AnimallyDatabase,
        entityId: Long,
        updatedAt: Instant,
    ) {
        repo(database).setInactive(entityId, updatedAt)
    }

    override fun remotePayload() =
        SyncJson
            .encodeToJsonElement(
                ReproductionPayload.serializer(),
                ReproductionPayload(eventType = "Heat-R", date = LocalDate(2024, 7, 1), details = "R-details"),
            ).jsonObject

    override fun expectedRemoteValues(parents: SeededParents) =
        mapOf(
            "patientId" to patientIdValue(parents),
            "eventType" to "Heat-R",
            "details" to "R-details",
        )

    override fun expectedLocalValues(parents: SeededParents) =
        mapOf(
            "patientId" to patientIdValue(parents),
            "eventType" to "Heat-L",
            "details" to "L-details",
        )

    override fun actualRowValues(
        database: AnimallyDatabase,
        entityId: Long,
        parents: SeededParents,
    ) = row(database, entityId).let {
        mapOf(
            "patientId" to it.patientId.toString(),
            "eventType" to it.eventType,
            "details" to it.details,
        )
    }

    override fun rowUpdatedAt(
        database: AnimallyDatabase,
        entityId: Long,
    ) = row(database, entityId).updatedAt

    override fun rowIsActive(
        database: AnimallyDatabase,
        entityId: Long,
    ) = row(database, entityId).isActive
}

private object ReproMedicationContract : PatientLinkedContract() {
    override val entityType = SyncEntityType.REPRO_MEDICATION

    private fun repo(database: AnimallyDatabase) = ReproMedicationRepositoryImpl(database)

    private fun row(
        database: AnimallyDatabase,
        entityId: Long,
    ) = requireNotNull(
        database.reproMedicationQueries
            .selectRowById(entityId)
            .executeAsOneOrNull()
            ?.toDomain(),
    ) { "ReproMedication $entityId missing" }

    override fun createHandler(database: AnimallyDatabase) = ReproMedicationSyncHandler(repo(database), PatientRepositoryImpl(database), database)

    override fun seedLocalRow(
        database: AnimallyDatabase,
        parents: SeededParents,
        updatedAt: Instant,
    ) = repo(database).insert(
        ReproMedication(
            id = 0L,
            patientId = seededPatientId(database),
            medication = "Oxytocin-L",
            dateAdministered = LocalDate(2024, 5, 1),
            dosage = "2ml-L",
            createdAt = EPOCH,
            updatedAt = updatedAt,
        ),
    )

    override fun attachServerId(
        database: AnimallyDatabase,
        entityId: Long,
        serverId: String,
        updatedAt: Instant,
    ) {
        database.reproMedicationQueries.setServerId(serverId, updatedAt, entityId)
    }

    override fun deactivateLocal(
        database: AnimallyDatabase,
        entityId: Long,
        updatedAt: Instant,
    ) {
        repo(database).setInactive(entityId, updatedAt)
    }

    override fun remotePayload() =
        SyncJson
            .encodeToJsonElement(
                ReproMedicationPayload.serializer(),
                ReproMedicationPayload(medication = "Oxytocin-R", dateAdministered = LocalDate(2024, 7, 1), dosage = "2ml-R"),
            ).jsonObject

    override fun expectedRemoteValues(parents: SeededParents) =
        mapOf(
            "patientId" to patientIdValue(parents),
            "medication" to "Oxytocin-R",
            "dosage" to "2ml-R",
        )

    override fun expectedLocalValues(parents: SeededParents) =
        mapOf(
            "patientId" to patientIdValue(parents),
            "medication" to "Oxytocin-L",
            "dosage" to "2ml-L",
        )

    override fun actualRowValues(
        database: AnimallyDatabase,
        entityId: Long,
        parents: SeededParents,
    ) = row(database, entityId).let {
        mapOf(
            "patientId" to it.patientId.toString(),
            "medication" to it.medication,
            "dosage" to it.dosage,
        )
    }

    override fun rowUpdatedAt(
        database: AnimallyDatabase,
        entityId: Long,
    ) = row(database, entityId).updatedAt

    override fun rowIsActive(
        database: AnimallyDatabase,
        entityId: Long,
    ) = row(database, entityId).isActive
}

private object SubstanceContract : PatientLinkedContract() {
    override val entityType = SyncEntityType.SUBSTANCE

    private fun repo(database: AnimallyDatabase) = ControlledSubstanceRepositoryImpl(database)

    private fun row(
        database: AnimallyDatabase,
        entityId: Long,
    ) = requireNotNull(
        database.substanceQueries
            .selectRowById(entityId)
            .executeAsOneOrNull()
            ?.toDomain(),
    ) { "Substance $entityId missing" }

    override fun createHandler(database: AnimallyDatabase) = SubstanceSyncHandler(repo(database), PatientRepositoryImpl(database), database)

    override fun seedLocalRow(
        database: AnimallyDatabase,
        parents: SeededParents,
        updatedAt: Instant,
    ) = repo(database).insert(
        ControlledSubstance(
            id = 0L,
            patientId = seededPatientId(database),
            drugName = "Detomidine-L",
            dose = "1ml-L",
            date = LocalDate(2024, 5, 1),
            createdAt = EPOCH,
            updatedAt = updatedAt,
        ),
    )

    override fun attachServerId(
        database: AnimallyDatabase,
        entityId: Long,
        serverId: String,
        updatedAt: Instant,
    ) {
        database.substanceQueries.setServerId(serverId, updatedAt, entityId)
    }

    override fun deactivateLocal(
        database: AnimallyDatabase,
        entityId: Long,
        updatedAt: Instant,
    ) {
        repo(database).setInactive(entityId, updatedAt)
    }

    override fun remotePayload() =
        SyncJson
            .encodeToJsonElement(
                SubstancePayload.serializer(),
                SubstancePayload(drugName = "Detomidine-R", dose = "1ml-R", date = LocalDate(2024, 7, 1), witness = "Witness-R"),
            ).jsonObject

    override fun expectedRemoteValues(parents: SeededParents) =
        mapOf(
            "patientId" to patientIdValue(parents),
            "drugName" to "Detomidine-R",
            "witness" to "Witness-R",
        )

    override fun expectedLocalValues(parents: SeededParents) =
        mapOf(
            "patientId" to patientIdValue(parents),
            "drugName" to "Detomidine-L",
            "witness" to null,
        )

    override fun actualRowValues(
        database: AnimallyDatabase,
        entityId: Long,
        parents: SeededParents,
    ) = row(database, entityId).let {
        mapOf(
            "patientId" to it.patientId.toString(),
            "drugName" to it.drugName,
            "witness" to it.witness,
        )
    }

    override fun rowUpdatedAt(
        database: AnimallyDatabase,
        entityId: Long,
    ) = row(database, entityId).updatedAt

    override fun rowIsActive(
        database: AnimallyDatabase,
        entityId: Long,
    ) = row(database, entityId).isActive
}

private object SurgeryContract : PatientLinkedContract() {
    override val entityType = SyncEntityType.SURGERY

    private fun repo(database: AnimallyDatabase) = SurgeryRepositoryImpl(database)

    private fun row(
        database: AnimallyDatabase,
        entityId: Long,
    ) = requireNotNull(
        database.surgeryQueries
            .selectRowById(entityId)
            .executeAsOneOrNull()
            ?.toDomain(),
    ) { "Surgery $entityId missing" }

    override fun createHandler(database: AnimallyDatabase) = SurgerySyncHandler(repo(database), PatientRepositoryImpl(database), database)

    override fun seedLocalRow(
        database: AnimallyDatabase,
        parents: SeededParents,
        updatedAt: Instant,
    ) = repo(database).insert(
        Surgery(
            id = 0L,
            patientId = seededPatientId(database),
            date = LocalDate(2024, 5, 1),
            createdAt = EPOCH,
            updatedAt = updatedAt,
        ),
    )

    override fun attachServerId(
        database: AnimallyDatabase,
        entityId: Long,
        serverId: String,
        updatedAt: Instant,
    ) {
        database.surgeryQueries.setServerId(serverId, updatedAt, entityId)
    }

    override fun deactivateLocal(
        database: AnimallyDatabase,
        entityId: Long,
        updatedAt: Instant,
    ) {
        repo(database).setInactive(entityId, updatedAt)
    }

    override fun remotePayload() =
        SyncJson
            .encodeToJsonElement(
                SurgeryPayload.serializer(),
                SurgeryPayload(date = LocalDate(2024, 7, 1), type = "Colic-R", surgeon = "Dr. Cut-R"),
            ).jsonObject

    override fun expectedRemoteValues(parents: SeededParents) =
        mapOf(
            "patientId" to patientIdValue(parents),
            "type" to "Colic-R",
            "surgeon" to "Dr. Cut-R",
        )

    override fun expectedLocalValues(parents: SeededParents) =
        mapOf(
            "patientId" to patientIdValue(parents),
            "type" to null,
            "surgeon" to null,
        )

    override fun actualRowValues(
        database: AnimallyDatabase,
        entityId: Long,
        parents: SeededParents,
    ) = row(database, entityId).let {
        mapOf(
            "patientId" to it.patientId.toString(),
            "type" to it.type,
            "surgeon" to it.surgeon,
        )
    }

    override fun rowUpdatedAt(
        database: AnimallyDatabase,
        entityId: Long,
    ) = row(database, entityId).updatedAt

    override fun rowIsActive(
        database: AnimallyDatabase,
        entityId: Long,
    ) = row(database, entityId).isActive
}

private object UltrasoundContract : PatientLinkedContract() {
    override val entityType = SyncEntityType.ULTRASOUND

    private fun repo(database: AnimallyDatabase) = UltrasoundRepositoryImpl(database)

    private fun row(
        database: AnimallyDatabase,
        entityId: Long,
    ) = requireNotNull(
        database.ultrasoundQueries
            .selectRowById(entityId)
            .executeAsOneOrNull()
            ?.toDomain(),
    ) { "Ultrasound $entityId missing" }

    override fun createHandler(database: AnimallyDatabase) = UltrasoundSyncHandler(repo(database), PatientRepositoryImpl(database), database)

    override fun seedLocalRow(
        database: AnimallyDatabase,
        parents: SeededParents,
        updatedAt: Instant,
    ) = repo(database).insert(
        Ultrasound(
            id = 0L,
            patientId = seededPatientId(database),
            date = LocalDate(2024, 5, 1),
            createdAt = EPOCH,
            updatedAt = updatedAt,
        ),
    )

    override fun attachServerId(
        database: AnimallyDatabase,
        entityId: Long,
        serverId: String,
        updatedAt: Instant,
    ) {
        database.ultrasoundQueries.setServerId(serverId, updatedAt, entityId)
    }

    override fun deactivateLocal(
        database: AnimallyDatabase,
        entityId: Long,
        updatedAt: Instant,
    ) {
        repo(database).setInactive(entityId, updatedAt)
    }

    override fun remotePayload() =
        SyncJson
            .encodeToJsonElement(
                UltrasoundPayload.serializer(),
                UltrasoundPayload(date = LocalDate(2024, 7, 1), ovaryStatus = "Active-R", follicleSizeMm = 35.5),
            ).jsonObject

    override fun expectedRemoteValues(parents: SeededParents) =
        mapOf(
            "patientId" to patientIdValue(parents),
            "ovaryStatus" to "Active-R",
            "follicleSizeMm" to "35.5",
        )

    override fun expectedLocalValues(parents: SeededParents) =
        mapOf(
            "patientId" to patientIdValue(parents),
            "ovaryStatus" to null,
            "follicleSizeMm" to null,
        )

    override fun actualRowValues(
        database: AnimallyDatabase,
        entityId: Long,
        parents: SeededParents,
    ) = row(database, entityId).let {
        mapOf(
            "patientId" to it.patientId.toString(),
            "ovaryStatus" to it.ovaryStatus,
            "follicleSizeMm" to it.follicleSizeMm?.toString(),
        )
    }

    override fun rowUpdatedAt(
        database: AnimallyDatabase,
        entityId: Long,
    ) = row(database, entityId).updatedAt

    override fun rowIsActive(
        database: AnimallyDatabase,
        entityId: Long,
    ) = row(database, entityId).isActive
}

private object VaccinationContract : PatientLinkedContract() {
    override val entityType = SyncEntityType.VACCINATION

    private fun repo(database: AnimallyDatabase) = VaccinationRepositoryImpl(database)

    private fun row(
        database: AnimallyDatabase,
        entityId: Long,
    ) = requireNotNull(
        database.vaccinationQueries
            .selectRowById(entityId)
            .executeAsOneOrNull()
            ?.toDomain(),
    ) { "Vaccination $entityId missing" }

    override fun createHandler(database: AnimallyDatabase) = VaccinationSyncHandler(repo(database), PatientRepositoryImpl(database), database)

    override fun seedLocalRow(
        database: AnimallyDatabase,
        parents: SeededParents,
        updatedAt: Instant,
    ) = repo(database).insert(
        Vaccination(
            id = 0L,
            patientId = seededPatientId(database),
            vaccineName = "Tetanus-L",
            dateAdministered = LocalDate(2024, 5, 1),
            createdAt = EPOCH,
            updatedAt = updatedAt,
        ),
    )

    override fun attachServerId(
        database: AnimallyDatabase,
        entityId: Long,
        serverId: String,
        updatedAt: Instant,
    ) {
        database.vaccinationQueries.setServerId(serverId, updatedAt, entityId)
    }

    override fun deactivateLocal(
        database: AnimallyDatabase,
        entityId: Long,
        updatedAt: Instant,
    ) {
        repo(database).setInactive(entityId, updatedAt)
    }

    override fun remotePayload() =
        SyncJson
            .encodeToJsonElement(
                VaccinationPayload.serializer(),
                VaccinationPayload(vaccineName = "Flu-R", dateAdministered = LocalDate(2024, 7, 1), vetName = "Dr. Remote"),
            ).jsonObject

    override fun expectedRemoteValues(parents: SeededParents) =
        mapOf(
            "patientId" to patientIdValue(parents),
            "vaccineName" to "Flu-R",
            "vetName" to "Dr. Remote",
        )

    override fun expectedLocalValues(parents: SeededParents) =
        mapOf(
            "patientId" to patientIdValue(parents),
            "vaccineName" to "Tetanus-L",
            "vetName" to null,
        )

    override fun actualRowValues(
        database: AnimallyDatabase,
        entityId: Long,
        parents: SeededParents,
    ) = row(database, entityId).let {
        mapOf(
            "patientId" to it.patientId.toString(),
            "vaccineName" to it.vaccineName,
            "vetName" to it.vetName,
        )
    }

    override fun rowUpdatedAt(
        database: AnimallyDatabase,
        entityId: Long,
    ) = row(database, entityId).updatedAt

    override fun rowIsActive(
        database: AnimallyDatabase,
        entityId: Long,
    ) = row(database, entityId).isActive
}

private object WeightContract : PatientLinkedContract() {
    override val entityType = SyncEntityType.WEIGHT

    private fun repo(database: AnimallyDatabase) = WeightRepositoryImpl(database)

    private fun row(
        database: AnimallyDatabase,
        entityId: Long,
    ) = requireNotNull(
        database.weightQueries
            .selectRowById(entityId)
            .executeAsOneOrNull()
            ?.toDomain(),
    ) { "Weight $entityId missing" }

    override fun createHandler(database: AnimallyDatabase) = WeightSyncHandler(repo(database), PatientRepositoryImpl(database), database)

    override fun seedLocalRow(
        database: AnimallyDatabase,
        parents: SeededParents,
        updatedAt: Instant,
    ) = repo(database).insert(
        Weight(
            id = 0L,
            patientId = seededPatientId(database),
            weightKg = 500.0,
            date = LocalDate(2024, 5, 1),
            notes = "L-notes",
            createdAt = EPOCH,
            updatedAt = updatedAt,
        ),
    )

    override fun attachServerId(
        database: AnimallyDatabase,
        entityId: Long,
        serverId: String,
        updatedAt: Instant,
    ) {
        database.weightQueries.setServerId(serverId, updatedAt, entityId)
    }

    override fun deactivateLocal(
        database: AnimallyDatabase,
        entityId: Long,
        updatedAt: Instant,
    ) {
        repo(database).setInactive(entityId, updatedAt)
    }

    override fun remotePayload() =
        SyncJson
            .encodeToJsonElement(
                WeightPayload.serializer(),
                WeightPayload(weightKg = 620.5, date = LocalDate(2024, 7, 1), notes = "R-notes"),
            ).jsonObject

    override fun expectedRemoteValues(parents: SeededParents) =
        mapOf(
            "patientId" to patientIdValue(parents),
            "weightKg" to "620.5",
            "notes" to "R-notes",
        )

    override fun expectedLocalValues(parents: SeededParents) =
        mapOf(
            "patientId" to patientIdValue(parents),
            "weightKg" to "500.0",
            "notes" to "L-notes",
        )

    override fun actualRowValues(
        database: AnimallyDatabase,
        entityId: Long,
        parents: SeededParents,
    ) = row(database, entityId).let {
        mapOf(
            "patientId" to it.patientId.toString(),
            "weightKg" to it.weightKg.toString(),
            "notes" to it.notes,
        )
    }

    override fun rowUpdatedAt(
        database: AnimallyDatabase,
        entityId: Long,
    ) = row(database, entityId).updatedAt

    override fun rowIsActive(
        database: AnimallyDatabase,
        entityId: Long,
    ) = row(database, entityId).isActive
}

private object CustomReminderContract : PatientLinkedContract() {
    override val entityType = SyncEntityType.CUSTOM_REMINDER

    private fun repo(database: AnimallyDatabase) = CustomReminderRepositoryImpl(database)

    private fun row(
        database: AnimallyDatabase,
        entityId: Long,
    ) = requireNotNull(
        database.customReminderQueries
            .selectRowById(entityId)
            .executeAsOneOrNull()
            ?.toDomain(),
    ) { "CustomReminder $entityId missing" }

    override fun createHandler(database: AnimallyDatabase) = CustomReminderSyncHandler(repo(database), PatientRepositoryImpl(database), database)

    override fun seedLocalRow(
        database: AnimallyDatabase,
        parents: SeededParents,
        updatedAt: Instant,
    ) = repo(database).insert(
        CustomReminder(
            id = 0L,
            patientId = seededPatientId(database),
            title = "Float teeth-L",
            dueDate = LocalDate(2024, 9, 1),
            createdAt = EPOCH,
            updatedAt = updatedAt,
        ),
    )

    override fun attachServerId(
        database: AnimallyDatabase,
        entityId: Long,
        serverId: String,
        updatedAt: Instant,
    ) {
        database.customReminderQueries.setServerId(serverId, updatedAt, entityId)
    }

    override fun deactivateLocal(
        database: AnimallyDatabase,
        entityId: Long,
        updatedAt: Instant,
    ) {
        repo(database).setInactive(entityId, updatedAt)
    }

    override fun remotePayload() =
        SyncJson
            .encodeToJsonElement(
                CustomReminderPayload.serializer(),
                CustomReminderPayload(
                    title = "Float teeth-R",
                    dueDate = LocalDate(2024, 10, 1),
                    linkedRecordType = "Dentistry",
                    linkedRecordId = 77L,
                ),
            ).jsonObject

    override fun expectedRemoteValues(parents: SeededParents) =
        mapOf(
            "patientId" to patientIdValue(parents),
            "title" to "Float teeth-R",
            "linkedRecordType" to "Dentistry",
            "linkedRecordId" to "77",
        )

    override fun expectedLocalValues(parents: SeededParents) =
        mapOf(
            "patientId" to patientIdValue(parents),
            "title" to "Float teeth-L",
            "linkedRecordType" to null,
            "linkedRecordId" to null,
        )

    override fun actualRowValues(
        database: AnimallyDatabase,
        entityId: Long,
        parents: SeededParents,
    ) = row(database, entityId).let {
        mapOf(
            "patientId" to it.patientId.toString(),
            "title" to it.title,
            "linkedRecordType" to it.linkedRecordType,
            "linkedRecordId" to it.linkedRecordId?.toString(),
        )
    }

    override fun rowUpdatedAt(
        database: AnimallyDatabase,
        entityId: Long,
    ) = row(database, entityId).updatedAt

    override fun rowIsActive(
        database: AnimallyDatabase,
        entityId: Long,
    ) = row(database, entityId).isActive
}

private object EmbryoTransferContract : PatientLinkedContract() {
    override val entityType = SyncEntityType.EMBRYO_TRANSFER

    private fun repo(database: AnimallyDatabase) = EmbryoTransferRepositoryImpl(database)

    private fun row(
        database: AnimallyDatabase,
        entityId: Long,
    ) = requireNotNull(
        database.embryoTransferQueries
            .selectRowById(entityId)
            .executeAsOneOrNull()
            ?.toDomain(),
    ) { "EmbryoTransfer $entityId missing" }

    override fun createHandler(database: AnimallyDatabase) = EmbryoTransferSyncHandler(repo(database), PatientRepositoryImpl(database), database)

    override fun seedLocalRow(
        database: AnimallyDatabase,
        parents: SeededParents,
        updatedAt: Instant,
    ) = repo(database).insert(
        EmbryoTransfer(
            id = 0L,
            patientId = seededPatientId(database),
            date = LocalDate(2024, 5, 1),
            embryoCount = 1,
            createdAt = EPOCH,
            updatedAt = updatedAt,
        ),
    )

    override fun attachServerId(
        database: AnimallyDatabase,
        entityId: Long,
        serverId: String,
        updatedAt: Instant,
    ) {
        database.embryoTransferQueries.setServerId(serverId, updatedAt, entityId)
    }

    override fun deactivateLocal(
        database: AnimallyDatabase,
        entityId: Long,
        updatedAt: Instant,
    ) {
        repo(database).setInactive(entityId, updatedAt)
    }

    override fun remotePayload() =
        SyncJson
            .encodeToJsonElement(
                EmbryoTransferPayload.serializer(),
                EmbryoTransferPayload(date = LocalDate(2024, 7, 1), embryoCount = 2, recipientMares = "Mare-R"),
            ).jsonObject

    override fun expectedRemoteValues(parents: SeededParents) =
        mapOf(
            "patientId" to patientIdValue(parents),
            "embryoCount" to "2",
            "recipientMares" to "Mare-R",
        )

    override fun expectedLocalValues(parents: SeededParents) =
        mapOf(
            "patientId" to patientIdValue(parents),
            "embryoCount" to "1",
            "recipientMares" to null,
        )

    override fun actualRowValues(
        database: AnimallyDatabase,
        entityId: Long,
        parents: SeededParents,
    ) = row(database, entityId).let {
        mapOf(
            "patientId" to it.patientId.toString(),
            "embryoCount" to it.embryoCount.toString(),
            "recipientMares" to it.recipientMares,
        )
    }

    override fun rowUpdatedAt(
        database: AnimallyDatabase,
        entityId: Long,
    ) = row(database, entityId).updatedAt

    override fun rowIsActive(
        database: AnimallyDatabase,
        entityId: Long,
    ) = row(database, entityId).isActive
}

private object IcsiContract : PatientLinkedContract() {
    override val entityType = SyncEntityType.ICSI

    private fun repo(database: AnimallyDatabase) = IcsiRepositoryImpl(database)

    private fun row(
        database: AnimallyDatabase,
        entityId: Long,
    ) = requireNotNull(
        database.icsiQueries
            .selectRowById(entityId)
            .executeAsOneOrNull()
            ?.toDomain(),
    ) { "Icsi $entityId missing" }

    override fun createHandler(database: AnimallyDatabase) = IcsiSyncHandler(repo(database), PatientRepositoryImpl(database), database)

    override fun seedLocalRow(
        database: AnimallyDatabase,
        parents: SeededParents,
        updatedAt: Instant,
    ) = repo(database).insert(
        Icsi(
            id = 0L,
            patientId = seededPatientId(database),
            date = LocalDate(2024, 5, 1),
            folliclesRecovered = 3,
            createdAt = EPOCH,
            updatedAt = updatedAt,
        ),
    )

    override fun attachServerId(
        database: AnimallyDatabase,
        entityId: Long,
        serverId: String,
        updatedAt: Instant,
    ) {
        database.icsiQueries.setServerId(serverId, updatedAt, entityId)
    }

    override fun deactivateLocal(
        database: AnimallyDatabase,
        entityId: Long,
        updatedAt: Instant,
    ) {
        repo(database).setInactive(entityId, updatedAt)
    }

    override fun remotePayload() =
        SyncJson
            .encodeToJsonElement(
                IcsiPayload.serializer(),
                IcsiPayload(date = LocalDate(2024, 7, 1), folliclesRecovered = 8, vetName = "Dr. Remote"),
            ).jsonObject

    override fun expectedRemoteValues(parents: SeededParents) =
        mapOf(
            "patientId" to patientIdValue(parents),
            "folliclesRecovered" to "8",
            "vetName" to "Dr. Remote",
        )

    override fun expectedLocalValues(parents: SeededParents) =
        mapOf(
            "patientId" to patientIdValue(parents),
            "folliclesRecovered" to "3",
            "vetName" to null,
        )

    override fun actualRowValues(
        database: AnimallyDatabase,
        entityId: Long,
        parents: SeededParents,
    ) = row(database, entityId).let {
        mapOf(
            "patientId" to it.patientId.toString(),
            "folliclesRecovered" to it.folliclesRecovered.toString(),
            "vetName" to it.vetName,
        )
    }

    override fun rowUpdatedAt(
        database: AnimallyDatabase,
        entityId: Long,
    ) = row(database, entityId).updatedAt

    override fun rowIsActive(
        database: AnimallyDatabase,
        entityId: Long,
    ) = row(database, entityId).isActive
}
