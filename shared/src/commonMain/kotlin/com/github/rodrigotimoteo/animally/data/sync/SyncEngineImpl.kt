package com.github.rodrigotimoteo.animally.data.sync

import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.domain.search.ISearchRepository
import com.github.rodrigotimoteo.animally.domain.sync.ChangedRecord
import com.github.rodrigotimoteo.animally.domain.sync.ENTITY_NOT_APPLIED
import com.github.rodrigotimoteo.animally.domain.sync.SyncAccepted
import com.github.rodrigotimoteo.animally.domain.sync.SyncApi
import com.github.rodrigotimoteo.animally.domain.sync.SyncChangeTracker
import com.github.rodrigotimoteo.animally.domain.sync.SyncEngine
import com.github.rodrigotimoteo.animally.domain.sync.SyncEntityHandler
import com.github.rodrigotimoteo.animally.domain.sync.SyncEntityHandlerRegistry
import com.github.rodrigotimoteo.animally.domain.sync.SyncEntityType
import com.github.rodrigotimoteo.animally.domain.sync.SyncMetadataRepository
import com.github.rodrigotimoteo.animally.domain.sync.SyncPushRequest
import com.github.rodrigotimoteo.animally.domain.sync.SyncRecord
import com.github.rodrigotimoteo.animally.domain.sync.SyncResult
import com.github.rodrigotimoteo.animally.domain.sync.hasUnresolvedParent
import kotlinx.coroutines.CancellationException
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant

/**
 * SQLDelight + [SyncApi] implementation of [SyncEngine].
 *
 * Push runs in dependency waves (owner before patient, patient before every
 * patient-linked child) so parent server ids resolve mid-run; children whose
 * parent server id is still unknown are deferred to the next sync. Pull applies
 * remote records in the same dependency order. Only when both directions
 * complete is the last-synced marker advanced to the sync start instant.
 *
 * Changed local rows, including soft-deleted rows, are offered to their
 * handlers so the wire contract can carry tombstones. A handler that cannot
 * serialize a row defers it for a later cycle; remote soft-deletes are applied
 * through the same LWW path as active records. Successful pulls advance to the
 * server watermark, while deferred/rejected rows pin the cursor before their
 * earliest retry timestamp.
 */
@Single(binds = [SyncEngine::class])
class SyncEngineImpl(
    @Provided private val api: SyncApi,
    @Provided private val metadataRepository: SyncMetadataRepository,
    @Provided private val changeTracker: SyncChangeTracker,
    @Provided private val handlerRegistry: SyncEntityHandlerRegistry,
    @Provided private val database: AnimallyDatabase,
    @Provided private val searchRepository: ISearchRepository,
) : SyncEngine {
    /**
     * Push wave order: every parent precedes its children.
     * Matches [SyncEntityType] enum and [SyncEntityHandlerRegistry] /
     * CloudKit [RecordNameAssigner] order.
     */
    private val pushOrder: List<SyncEntityType> =
        listOf(
            SyncEntityType.OWNER,
            SyncEntityType.PATIENT,
            SyncEntityType.ANAMNESE,
            SyncEntityType.CONSULTATION,
            SyncEntityType.DENTISTRY,
            SyncEntityType.DEWORMING,
            SyncEntityType.FARRIER_VISIT,
            SyncEntityType.GESTATION,
            SyncEntityType.IMAGING,
            SyncEntityType.LAB_RESULT,
            SyncEntityType.LAMENESS,
            SyncEntityType.MEDICATION,
            SyncEntityType.REPRODUCTION,
            SyncEntityType.REPRO_MEDICATION,
            SyncEntityType.SUBSTANCE,
            SyncEntityType.SURGERY,
            SyncEntityType.ULTRASOUND,
            SyncEntityType.FOLLICLE,
            SyncEntityType.VACCINATION,
            SyncEntityType.WEIGHT,
            SyncEntityType.CUSTOM_REMINDER,
            SyncEntityType.EMBRYO_TRANSFER,
            SyncEntityType.ICSI,
        )

    private val typeOrder: Map<SyncEntityType, Int> =
        pushOrder.mapIndexed { index, type -> type to index }.toMap()

    private val serverIdWriters: Map<SyncEntityType, (String, Instant, Long) -> Unit> =
        mapOf(
            SyncEntityType.OWNER to { serverId, updatedAt, clientId ->
                database.ownerQueries.setServerId(serverId, updatedAt, clientId)
            },
            SyncEntityType.PATIENT to { serverId, updatedAt, clientId ->
                database.patientQueries.setServerId(serverId, updatedAt, clientId)
            },
            SyncEntityType.ANAMNESE to { serverId, updatedAt, clientId ->
                database.anamneseQueries.setServerId(serverId, updatedAt, clientId)
            },
            SyncEntityType.CONSULTATION to { serverId, updatedAt, clientId ->
                database.consultationQueries.setServerId(serverId, updatedAt, clientId)
            },
            SyncEntityType.DENTISTRY to { serverId, updatedAt, clientId ->
                database.dentistryQueries.setServerId(serverId, updatedAt, clientId)
            },
            SyncEntityType.DEWORMING to { serverId, updatedAt, clientId ->
                database.dewormingQueries.setServerId(serverId, updatedAt, clientId)
            },
            SyncEntityType.FARRIER_VISIT to { serverId, updatedAt, clientId ->
                database.farrierVisitQueries.setServerId(serverId, updatedAt, clientId)
            },
            SyncEntityType.GESTATION to { serverId, updatedAt, clientId ->
                database.gestationQueries.setServerId(serverId, updatedAt, clientId)
            },
            SyncEntityType.IMAGING to { serverId, updatedAt, clientId ->
                database.imagingQueries.setServerId(serverId, updatedAt, clientId)
            },
            SyncEntityType.LAB_RESULT to { serverId, updatedAt, clientId ->
                database.labResultQueries.setServerId(serverId, updatedAt, clientId)
            },
            SyncEntityType.LAMENESS to { serverId, updatedAt, clientId ->
                database.lamenessQueries.setServerId(serverId, updatedAt, clientId)
            },
            SyncEntityType.MEDICATION to { serverId, updatedAt, clientId ->
                database.medicationQueries.setServerId(serverId, updatedAt, clientId)
            },
            SyncEntityType.REPRODUCTION to { serverId, updatedAt, clientId ->
                database.reproductionQueries.setServerId(serverId, updatedAt, clientId)
            },
            SyncEntityType.REPRO_MEDICATION to { serverId, updatedAt, clientId ->
                database.reproMedicationQueries.setServerId(serverId, updatedAt, clientId)
            },
            SyncEntityType.SUBSTANCE to { serverId, updatedAt, clientId ->
                database.substanceQueries.setServerId(serverId, updatedAt, clientId)
            },
            SyncEntityType.SURGERY to { serverId, updatedAt, clientId ->
                database.surgeryQueries.setServerId(serverId, updatedAt, clientId)
            },
            SyncEntityType.ULTRASOUND to { serverId, updatedAt, clientId ->
                database.ultrasoundQueries.setServerId(serverId, updatedAt, clientId)
            },
            SyncEntityType.FOLLICLE to { serverId, updatedAt, clientId ->
                database.follicleQueries.setServerId(serverId, updatedAt, clientId)
            },
            SyncEntityType.VACCINATION to { serverId, updatedAt, clientId ->
                database.vaccinationQueries.setServerId(serverId, updatedAt, clientId)
            },
            SyncEntityType.WEIGHT to { serverId, updatedAt, clientId ->
                database.weightQueries.setServerId(serverId, updatedAt, clientId)
            },
            SyncEntityType.CUSTOM_REMINDER to { serverId, updatedAt, clientId ->
                database.customReminderQueries.setServerId(serverId, updatedAt, clientId)
            },
            SyncEntityType.EMBRYO_TRANSFER to { serverId, updatedAt, clientId ->
                database.embryoTransferQueries.setServerId(serverId, updatedAt, clientId)
            },
            SyncEntityType.ICSI to { serverId, updatedAt, clientId ->
                database.icsiQueries.setServerId(serverId, updatedAt, clientId)
            },
        )

    override suspend fun sync(): SyncResult =
        try {
            val deviceId = metadataRepository.getDeviceId()
            val lastSyncAt = metadataRepository.getOrCreateLastSyncAt(deviceId)
            val pushed = pushChanges(deviceId, lastSyncAt)
            val pulled = pullChanges(lastSyncAt)
            if (pulled.applied > 0) {
                searchRepository.markIndexDirty()
                searchRepository.reindexIfNeeded(ISearchRepository.SEARCH_INDEX_VERSION)
            }
            // The server watermark, not the device clock, defines the next
            // pull boundary. Keep the cursor just before the oldest retryable
            // local row so deferred/rejected work remains eligible.
            val nextSyncAt =
                pushed.retryAt?.let { retryAt ->
                    minOf(pulled.serverTimestamp, retryAt - 1.milliseconds)
                } ?: pulled.serverTimestamp
            metadataRepository.updateLastSyncAt(nextSyncAt)
            SyncResult.success(
                pushedCount = pushed.accepted,
                pulledCount = pulled.applied,
                rejectedCount = pushed.rejected,
                deferredCount = pushed.deferred,
                serverTimestamp = pulled.serverTimestamp,
            )
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            SyncResult.failure(error.message ?: error::class.simpleName.orEmpty())
        }

    private suspend fun pushChanges(
        deviceId: String,
        lastSyncAt: Instant,
    ): PushOutcome {
        val changedByType = changeTracker.recordsChangedSince(lastSyncAt).groupBy { it.entityType }
        var accepted = 0
        var rejected = 0
        var deferred = 0
        var retryAt: Instant? = null
        for (type in pushOrder) {
            val wave = changedByType[type.wireName].orEmpty()
            val outcome = pushWave(type, wave, deviceId)
            accepted += outcome.accepted
            rejected += outcome.rejected
            deferred += outcome.deferred
            retryAt = minOfNullable(retryAt, outcome.retryAt)
        }
        return PushOutcome(accepted, rejected, deferred, retryAt)
    }

    private suspend fun pushWave(
        type: SyncEntityType,
        wave: List<ChangedRecord>,
        deviceId: String,
    ): WaveOutcome {
        if (wave.isEmpty()) return WaveOutcome()
        val ready = buildReadyRecords(handlerRegistry.handlerFor(type), wave)
        if (ready.isEmpty()) {
            return WaveOutcome(deferred = wave.size, retryAt = wave.minOfOrNull(ChangedRecord::updatedAt))
        }
        val response = api.push(SyncPushRequest(deviceId, ready))
        writeAcceptedServerIds(response.accepted)
        val rejectedKeys = response.rejected.map { it.type to it.clientId }.toSet()
        val readyKeys = ready.mapNotNull { record -> record.clientId?.let { record.type to it } }.toSet()
        val retryAt =
            wave
                .filter { changed ->
                    (changed.entityType to changed.id) in rejectedKeys ||
                        (changed.entityType to changed.id) !in readyKeys
                }.minOfOrNull(ChangedRecord::updatedAt)
        return WaveOutcome(
            accepted = response.accepted.size,
            rejected = response.rejected.size,
            deferred = wave.size - ready.size,
            retryAt = retryAt,
        )
    }

    private suspend fun buildReadyRecords(
        handler: SyncEntityHandler,
        wave: List<ChangedRecord>,
    ): List<SyncRecord> =
        buildList {
            for (changed in wave) {
                val record = handler.buildRecord(changed.id)
                if (record.hasUnresolvedParent()) continue
                add(record)
            }
        }

    private suspend fun pullChanges(lastSyncAt: Instant): PullOutcome {
        val response = api.pull(lastSyncAt)
        var applied = 0
        val orderedRecords =
            response.records.sortedBy { record -> orderIndex(record.type) }
        for (record in orderedRecords) {
            val type = entityTypeOf(record.type) ?: continue
            val result = handlerRegistry.handlerFor(type).applyRecord(record)
            if (result != ENTITY_NOT_APPLIED) applied++
        }
        return PullOutcome(applied, response.serverTimestamp)
    }

    private fun orderIndex(type: String): Int = entityTypeOf(type)?.let { typeOrder[it] } ?: typeOrder.size

    private fun entityTypeOf(type: String): SyncEntityType? = SyncEntityType.fromWireName(type)

    private fun writeAcceptedServerIds(accepted: List<SyncAccepted>) {
        for (verdict in accepted) {
            val type = entityTypeOf(verdict.type) ?: continue
            writeServerId(type, verdict.serverId, verdict.clientId, verdict.updatedAt)
        }
    }

    private fun writeServerId(
        type: SyncEntityType,
        serverId: String,
        clientId: Long,
        updatedAt: Instant,
    ) {
        serverIdWriters[type]?.invoke(serverId, updatedAt, clientId)
    }

    private data class PushOutcome(
        val accepted: Int,
        val rejected: Int,
        val deferred: Int,
        val retryAt: Instant?,
    )

    private data class WaveOutcome(
        val accepted: Int = 0,
        val rejected: Int = 0,
        val deferred: Int = 0,
        val retryAt: Instant? = null,
    )

    private data class PullOutcome(
        val applied: Int,
        val serverTimestamp: Instant,
    )

    private fun minOfNullable(
        first: Instant?,
        second: Instant?,
    ): Instant? =
        when {
            first == null -> second
            second == null -> first
            first <= second -> first
            else -> second
        }
}
