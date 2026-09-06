package com.github.rodrigotimoteo.animally.sync.cloudkit

import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.domain.search.ISearchRepository
import com.github.rodrigotimoteo.animally.domain.sync.ChangedRecord
import com.github.rodrigotimoteo.animally.domain.sync.ENTITY_NOT_APPLIED
import com.github.rodrigotimoteo.animally.domain.sync.SyncChangeTracker
import com.github.rodrigotimoteo.animally.domain.sync.SyncEngine
import com.github.rodrigotimoteo.animally.domain.sync.SyncEntityHandlerRegistry
import com.github.rodrigotimoteo.animally.domain.sync.SyncEntityType
import com.github.rodrigotimoteo.animally.domain.sync.SyncResult
import com.github.rodrigotimoteo.animally.domain.sync.hasUnresolvedParent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.builtins.ListSerializer
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/** Max envelopes per [SyncCloudBridge.stageRecords] call. */
private const val EXPORT_CHUNK_SIZE = 200
private const val BRIDGE_EVENT_TIMEOUT_MS = 30_000L

/** Raised internally when the iCloud account disappears mid-cycle. */
private class AccountLostException : Exception("iCloud account unavailable")

/** Raised when a native callback never arrives for the current sync step. */
private class BridgeEventTimeoutException : Exception("CloudKit callback timed out")

/** Running tallies for one sync cycle. */
private class Counters {
    var pushed: Int = 0
    var pulled: Int = 0
    var deferred: Int = 0
}

/** One envelope staged for export plus its source row timestamp for cursor math. */
private class Staged(
    val envelope: CloudKitEnvelope,
    val updatedAtMs: Long,
)

/**
 * Buffers shim events so the engine can consume them synchronously mid-cycle.
 */
private class EventPump {
    private val channel = Channel<SyncBridgeEvent>(Channel.UNLIMITED)

    /** Offers an event without suspending; drops nothing (unlimited buffer). */
    fun offer(event: SyncBridgeEvent) {
        channel.trySend(event)
    }

    /** Suspends until the next event arrives. */
    suspend fun receive(): SyncBridgeEvent = channel.receive()

    /** Discards buffered events (used after an account reset). */
    @Suppress("UNUSED_EXPRESSION")
    fun drain() {
        while (channel.tryReceive().isSuccess) Unit
    }
}

/**
 * Writes client-generated CloudKit record names onto never-synced rows.
 *
 * The assignment happens immediately at stage time — before the cloud
 * confirms — so a failed export retries under the same record name instead of
 * duplicating the row. Each entity's generated `setServerId` query is used;
 * the interface-level handlers expose no setter, and this lane must not
 * modify them.
 */
@OptIn(ExperimentalUuidApi::class)
private class RecordNameAssigner(
    private val database: AnimallyDatabase,
) {
    /** One setter per entity type; keeps [assign] branch-free. */
    private val setters: Map<SyncEntityType, (String, Instant, Long) -> Unit> =
        mapOf(
            SyncEntityType.OWNER to { name, updatedAt, id ->
                database.ownerQueries.setServerId(name, updatedAt, id)
            },
            SyncEntityType.PATIENT to { name, updatedAt, id ->
                database.patientQueries.setServerId(name, updatedAt, id)
            },
            SyncEntityType.ANAMNESE to { name, updatedAt, id ->
                database.anamneseQueries.setServerId(name, updatedAt, id)
            },
            SyncEntityType.CONSULTATION to { name, updatedAt, id ->
                database.consultationQueries.setServerId(name, updatedAt, id)
            },
            SyncEntityType.DENTISTRY to { name, updatedAt, id ->
                database.dentistryQueries.setServerId(name, updatedAt, id)
            },
            SyncEntityType.DEWORMING to { name, updatedAt, id ->
                database.dewormingQueries.setServerId(name, updatedAt, id)
            },
            SyncEntityType.FARRIER_VISIT to { name, updatedAt, id ->
                database.farrierVisitQueries.setServerId(name, updatedAt, id)
            },
            SyncEntityType.GESTATION to { name, updatedAt, id ->
                database.gestationQueries.setServerId(name, updatedAt, id)
            },
            SyncEntityType.IMAGING to { name, updatedAt, id ->
                database.imagingQueries.setServerId(name, updatedAt, id)
            },
            SyncEntityType.LAB_RESULT to { name, updatedAt, id ->
                database.labResultQueries.setServerId(name, updatedAt, id)
            },
            SyncEntityType.LAMENESS to { name, updatedAt, id ->
                database.lamenessQueries.setServerId(name, updatedAt, id)
            },
            SyncEntityType.MEDICATION to { name, updatedAt, id ->
                database.medicationQueries.setServerId(name, updatedAt, id)
            },
            SyncEntityType.REPRODUCTION to { name, updatedAt, id ->
                database.reproductionQueries.setServerId(name, updatedAt, id)
            },
            SyncEntityType.REPRO_MEDICATION to { name, updatedAt, id ->
                database.reproMedicationQueries.setServerId(name, updatedAt, id)
            },
            SyncEntityType.SUBSTANCE to { name, updatedAt, id ->
                database.substanceQueries.setServerId(name, updatedAt, id)
            },
            SyncEntityType.SURGERY to { name, updatedAt, id ->
                database.surgeryQueries.setServerId(name, updatedAt, id)
            },
            SyncEntityType.ULTRASOUND to { name, updatedAt, id ->
                database.ultrasoundQueries.setServerId(name, updatedAt, id)
            },
            SyncEntityType.FOLLICLE to { name, updatedAt, id ->
                database.follicleQueries.setServerId(name, updatedAt, id)
            },
            SyncEntityType.VACCINATION to { name, updatedAt, id ->
                database.vaccinationQueries.setServerId(name, updatedAt, id)
            },
            SyncEntityType.WEIGHT to { name, updatedAt, id ->
                database.weightQueries.setServerId(name, updatedAt, id)
            },
            SyncEntityType.CUSTOM_REMINDER to { name, updatedAt, id ->
                database.customReminderQueries.setServerId(name, updatedAt, id)
            },
            SyncEntityType.EMBRYO_TRANSFER to { name, updatedAt, id ->
                database.embryoTransferQueries.setServerId(name, updatedAt, id)
            },
            SyncEntityType.ICSI to { name, updatedAt, id ->
                database.icsiQueries.setServerId(name, updatedAt, id)
            },
        )

    /** Generates a UUID record name, persists it on row [entityId], returns it. */
    fun assign(
        type: SyncEntityType,
        entityId: Long,
        updatedAt: Instant,
    ): String {
        val recordName = Uuid.random().toString()
        setters[type]?.invoke(recordName, updatedAt, entityId)
        return recordName
    }
}

/**
 * CloudKit-backed [SyncEngine]: exports changed rows as envelopes through the
 * platform bridge, imports remote envelopes through the existing entity
 * handlers (LWW lives there), and persists progress in the SyncState table.
 *
 * No polling timers live here — callers drive cycles via [sync]/[syncNow].
 * The first enabled cycle installs the callback and starts the native bridge;
 * while [CloudKitSyncKeys.ENABLED] is off, every cycle is a no-op.
 */
public class CloudKitSyncEngineImpl(
    private val bridge: SyncCloudBridge,
    private val changeTracker: SyncChangeTracker,
    private val registry: SyncEntityHandlerRegistry,
    private val database: AnimallyDatabase,
    private val settings: CloudKitSyncSettings,
    private val searchRepository: ISearchRepository,
) : SyncEngine {
    private val mutex = Mutex()
    private val pump = EventPump()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val envelopesSerializer = ListSerializer(CloudKitEnvelope.serializer())
    private val nameAssigner = RecordNameAssigner(database)

    private var started: Boolean = false

    /** Orphan envelopes whose parents were unknown at apply time; retried next cycle. */
    private val pendingOrphans = ArrayDeque<CloudKitEnvelope>()

    override suspend fun sync(): SyncResult =
        try {
            if (!settings.isEnabled()) {
                SyncResult(success = true)
            } else {
                mutex.withLock {
                    ensureStarted()
                    runSyncCycle()
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: AccountLostException) {
            mutex.withLock { handleAccountLostLocked() }
            SyncResult.failure(e.message ?: "iCloud account unavailable")
        } catch (t: Throwable) {
            SyncResult.failure(t.message ?: "CloudKit sync failed")
        }

    /** Manual trigger: flushes staged exports, retries orphans, fetches changes. */
    public suspend fun syncNow(): SyncResult = sync()

    private suspend fun ensureStarted() {
        if (!settings.isEnabled() || started) return
        bridge.setEventHandler { json -> handleEvent(json) }
        bridge.start()
        started = true
    }

    private suspend fun runSyncCycle(): SyncResult {
        val counters = Counters()
        if (pendingOrphans.isNotEmpty()) {
            val orphans = pendingOrphans.toList()
            pendingOrphans.clear()
            applyEnvelopes(orphans, counters)
        }
        exportPendingChanges(counters)
        fetchAndApplyRemote(counters)
        return SyncResult(
            success = true,
            pushedCount = counters.pushed,
            pulledCount = counters.pulled,
            rejectedCount = 0,
            deferredCount = counters.deferred,
            serverTimestamp = Clock.System.now(),
        )
    }

    private fun handleEvent(json: String) {
        when (val event = parseSyncBridgeEvent(json)) {
            is SyncBridgeEvent.AccountChange ->
                scope.launch {
                    if (!event.available) {
                        mutex.withLock { handleAccountLostLocked() }
                    }
                }

            null -> Unit
            else -> pump.offer(event)
        }
    }

    private fun handleAccountLostLocked() {
        bridge.stop()
        started = false
        pendingOrphans.clear()
        pump.drain()
        // Local data is kept; clearing state makes the next start() full re-fetch.
        settings.clearEngineState()
    }

    private suspend fun exportPendingChanges(counters: Counters) {
        val cursorMs = settings.exportCursorMs()
        val changed = changeTracker.recordsChangedSince(Instant.fromEpochMilliseconds(cursorMs))
        val staged =
            buildList {
                for (entry in changed.sortedBy(ChangedRecord::updatedAt)) {
                    stageEntry(entry, counters)?.let(::add)
                }
            }
        if (staged.isEmpty()) return

        staged.chunked(EXPORT_CHUNK_SIZE).forEach { chunk ->
            bridge.stageRecords(BridgeEventJson.encodeToString(envelopesSerializer, chunk.map(Staged::envelope)))
        }
        advanceCursorOnConfirmation(staged, cursorMs, counters)
    }

    private suspend fun stageEntry(
        entry: ChangedRecord,
        counters: Counters,
    ): Staged? {
        val type = SyncEntityType.fromWireName(entry.entityType)
        val record =
            type?.let { t ->
                try {
                    registry.handlerFor(t).buildRecord(entry.id)
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    null
                }
            }
        // Parent ids with a null serverId mean the parent hasn't been exported yet.
        val parents = record?.takeIf { !it.hasUnresolvedParent() }?.parentServerIds

        if (type == null || record == null || parents == null) {
            // Unknown entity or parent not yet exported — retry on a later cycle;
            // cursor stays behind.
            counters.deferred++
            return null
        }

        // Tombstones (isActive=0) are exported too: deletion must propagate.
        val recordName = record.serverId ?: nameAssigner.assign(type, entry.id, entry.updatedAt)
        val envelope =
            CloudKitEnvelope.from(
                recordType = entry.entityType,
                recordName = recordName,
                updatedAtMs = entry.updatedAt.toEpochMilliseconds(),
                isActive = entry.isActive,
                parents = parents,
                payload = record.payload,
            )
        return Staged(
            envelope = envelope,
            updatedAtMs = entry.updatedAt.toEpochMilliseconds(),
        )
    }

    private suspend fun advanceCursorOnConfirmation(
        staged: List<Staged>,
        cursorMs: Long,
        counters: Counters,
    ) {
        val pending = staged.associateBy { it.envelope.recordName }.toMutableMap()
        val confirmedNames = mutableSetOf<String>()
        val failedNames = mutableSetOf<String>()
        while (pending.isNotEmpty()) {
            when (val event = receiveEvent()) {
                is SyncBridgeEvent.Exported ->
                    confirmExported(event.names, pending, counters, confirmedNames)

                // Cursor not advanced past failures: re-staged next cycle.
                is SyncBridgeEvent.ExportFailed -> deferFailed(event.names, pending, counters, failedNames)

                is SyncBridgeEvent.Imported -> applyEnvelopes(event.records, counters)

                is SyncBridgeEvent.AccountChange -> throw AccountLostException()
            }
        }
        settings.setExportCursorMs(
            safeExportCursor(
                staged = staged.map { it.envelope.recordName to it.updatedAtMs },
                cursorMs = cursorMs,
                confirmedNames = confirmedNames,
                failedNames = failedNames,
            ),
        )
    }

    /** Confirms exports by name and remembers exactly which rows were acknowledged. */
    private fun confirmExported(
        names: List<String>,
        pending: MutableMap<String, Staged>,
        counters: Counters,
        confirmedNames: MutableSet<String>,
    ) {
        names.forEach { name ->
            pending.remove(name)?.let { staged ->
                counters.pushed++
                confirmedNames += staged.envelope.recordName
            }
        }
    }

    /** Drops failed exports from [pending] so the loop can drain without advancing the cursor. */
    private fun deferFailed(
        names: List<String>,
        pending: MutableMap<String, Staged>,
        counters: Counters,
        failedNames: MutableSet<String>,
    ) {
        names.forEach { name ->
            pending.remove(name)?.let {
                failedNames += it.envelope.recordName
                counters.deferred++
            }
        }
    }

    private suspend fun fetchAndApplyRemote(counters: Counters) {
        bridge.fetchChanges()
        // One imported event per fetchChanges call; stray export events stay buffered.
        while (true) {
            when (val event = receiveEvent()) {
                is SyncBridgeEvent.Imported -> {
                    applyEnvelopes(event.records, counters)
                    return
                }

                is SyncBridgeEvent.AccountChange -> throw AccountLostException()

                else -> Unit
            }
        }
    }

    private suspend fun receiveEvent(): SyncBridgeEvent =
        try {
            withTimeout(BRIDGE_EVENT_TIMEOUT_MS) { pump.receive() }
        } catch (_: TimeoutCancellationException) {
            throw BridgeEventTimeoutException()
        }

    private suspend fun applyEnvelopes(
        envelopes: List<CloudKitEnvelope>,
        counters: Counters,
    ) {
        // Each handler's apply path performs its own repository writes; SQLDelight's
        // non-suspend transaction block cannot host the suspend applyRecord calls.
        var appliedAny = false
        for (envelope in envelopes) {
            val type = SyncEntityType.fromWireName(envelope.recordType) ?: continue
            val applied = registry.handlerFor(type).applyRecord(envelope.toSyncRecord())
            if (applied == ENTITY_NOT_APPLIED) {
                pendingOrphans.addLast(envelope)
            } else {
                counters.pulled++
                appliedAny = true
            }
        }
        if (appliedAny) {
            searchRepository.markIndexDirty()
            searchRepository.reindexIfNeeded(ISearchRepository.SEARCH_INDEX_VERSION)
        }
    }
}

/**
 * Returns the furthest safe export cursor. A failed row pins the cursor before
 * its timestamp; this deliberately retries successful rows at that timestamp
 * because the wire cursor has no tie-breaker beyond milliseconds.
 */
internal fun safeExportCursor(
    staged: List<Pair<String, Long>>,
    cursorMs: Long,
    confirmedNames: Set<String>,
    failedNames: Set<String>,
): Long {
    val firstFailedTimestamp = staged.filter { it.first in failedNames }.minOfOrNull { it.second }
    return staged
        .asSequence()
        .filter { it.first in confirmedNames }
        .filter { firstFailedTimestamp == null || it.second < firstFailedTimestamp }
        .map { it.second }
        .maxOrNull()
        ?: cursorMs
}
