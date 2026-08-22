package com.github.rodrigotimoteo.animally.data.sync

import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
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
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single
import kotlin.time.Clock
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
 * Soft-deleted local rows are not pushed: the handlers read rows through
 * `isActive = 1` filters, so a tombstone cannot be serialized. Remote
 * soft-deletes (a pulled record with `isActive = false`) do propagate.
 */
@Single(binds = [SyncEngine::class])
class SyncEngineImpl(
    @Provided private val api: SyncApi,
    @Provided private val metadataRepository: SyncMetadataRepository,
    @Provided private val changeTracker: SyncChangeTracker,
    @Provided private val handlerRegistry: SyncEntityHandlerRegistry,
    @Provided private val database: AnimallyDatabase,
) : SyncEngine {
    /** Push wave order: every parent precedes its children. */
    private val pushOrder: List<SyncEntityType> =
        listOf(
            SyncEntityType.OWNER,
            SyncEntityType.PATIENT,
            SyncEntityType.ANAMNESE,
            SyncEntityType.CUSTOM_REMINDER,
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
            SyncEntityType.VACCINATION,
            SyncEntityType.WEIGHT,
        )

    private val typeOrder: Map<SyncEntityType, Int> =
        pushOrder.mapIndexed { index, type -> type to index }.toMap()

    override suspend fun sync(): SyncResult {
        val now = Clock.System.now()
        return try {
            val deviceId = metadataRepository.getDeviceId()
            val lastSyncAt = metadataRepository.getOrCreateLastSyncAt(deviceId)
            val pushed = pushChanges(deviceId, lastSyncAt, now)
            val pulled = pullChanges(lastSyncAt)
            metadataRepository.updateLastSyncAt(now)
            SyncResult.success(
                pushedCount = pushed.accepted,
                pulledCount = pulled.applied,
                rejectedCount = pushed.rejected,
                deferredCount = pushed.deferred,
                serverTimestamp = pulled.serverTimestamp,
            )
        } catch (error: Exception) {
            SyncResult.failure(error.message ?: error::class.simpleName.orEmpty())
        }
    }

    private suspend fun pushChanges(
        deviceId: String,
        lastSyncAt: Instant,
        now: Instant,
    ): PushOutcome {
        val changedByType = changeTracker.recordsChangedSince(lastSyncAt).groupBy { it.entityType }
        var accepted = 0
        var rejected = 0
        var deferred = 0
        for (type in pushOrder) {
            val wave = changedByType[type.wireName].orEmpty().filter { it.isActive }
            val outcome = pushWave(type, wave, deviceId, now)
            accepted += outcome.accepted
            rejected += outcome.rejected
            deferred += outcome.deferred
        }
        return PushOutcome(accepted, rejected, deferred)
    }

    private suspend fun pushWave(
        type: SyncEntityType,
        wave: List<ChangedRecord>,
        deviceId: String,
        now: Instant,
    ): WaveOutcome {
        if (wave.isEmpty()) return WaveOutcome()
        val ready = buildReadyRecords(handlerRegistry.handlerFor(type), wave)
        if (ready.isEmpty()) return WaveOutcome(deferred = wave.size)
        val response = api.push(SyncPushRequest(deviceId, ready))
        writeAcceptedServerIds(response.accepted, now)
        return WaveOutcome(
            accepted = response.accepted.size,
            rejected = response.rejected.size,
            deferred = wave.size - ready.size,
        )
    }

    private suspend fun buildReadyRecords(
        handler: SyncEntityHandler,
        wave: List<ChangedRecord>,
    ): List<SyncRecord> =
        buildList {
            for (changed in wave) {
                val record = handler.buildRecord(changed.id)
                if (hasUnresolvedParent(record)) continue
                add(record)
            }
        }

    private fun hasUnresolvedParent(record: SyncRecord): Boolean = record.parentServerIds.values.any { it == null }

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

    private fun writeAcceptedServerIds(
        accepted: List<SyncAccepted>,
        now: Instant,
    ) {
        for (verdict in accepted) {
            val type = entityTypeOf(verdict.type) ?: continue
            writeServerId(type, verdict.serverId, verdict.clientId, now)
        }
    }

    private fun writeServerId(
        type: SyncEntityType,
        serverId: String,
        clientId: Long,
        updatedAt: Instant,
    ) {
        when (type) {
            SyncEntityType.LAMENESS,
            SyncEntityType.MEDICATION,
            SyncEntityType.REPRODUCTION,
            SyncEntityType.REPRO_MEDICATION,
            SyncEntityType.SUBSTANCE,
            SyncEntityType.SURGERY,
            SyncEntityType.ULTRASOUND,
            SyncEntityType.VACCINATION,
            SyncEntityType.WEIGHT,
            SyncEntityType.CUSTOM_REMINDER,
            SyncEntityType.EMBRYO_TRANSFER,
            SyncEntityType.ICSI,
            -> writeRemainingServerId(type, serverId, clientId, updatedAt)
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
            -> writeCoreServerId(type, serverId, clientId, updatedAt)
        }
    }

    private fun writeCoreServerId(
        type: SyncEntityType,
        serverId: String,
        clientId: Long,
        updatedAt: Instant,
    ) {
        when (type) {
            SyncEntityType.OWNER -> database.ownerQueries.setServerId(serverId, updatedAt, clientId)
            SyncEntityType.PATIENT -> database.patientQueries.setServerId(serverId, updatedAt, clientId)
            SyncEntityType.ANAMNESE -> database.anamneseQueries.setServerId(serverId, updatedAt, clientId)
            SyncEntityType.CONSULTATION -> database.consultationQueries.setServerId(serverId, updatedAt, clientId)
            SyncEntityType.DENTISTRY -> database.dentistryQueries.setServerId(serverId, updatedAt, clientId)
            SyncEntityType.DEWORMING -> database.dewormingQueries.setServerId(serverId, updatedAt, clientId)
            SyncEntityType.FARRIER_VISIT -> database.farrierVisitQueries.setServerId(serverId, updatedAt, clientId)
            SyncEntityType.GESTATION -> database.gestationQueries.setServerId(serverId, updatedAt, clientId)
            SyncEntityType.IMAGING -> database.imagingQueries.setServerId(serverId, updatedAt, clientId)
            SyncEntityType.LAB_RESULT -> database.labResultQueries.setServerId(serverId, updatedAt, clientId)
            else -> Unit
        }
    }

    private fun writeRemainingServerId(
        type: SyncEntityType,
        serverId: String,
        clientId: Long,
        updatedAt: Instant,
    ) {
        when (type) {
            SyncEntityType.LAMENESS -> database.lamenessQueries.setServerId(serverId, updatedAt, clientId)
            SyncEntityType.MEDICATION -> database.medicationQueries.setServerId(serverId, updatedAt, clientId)
            SyncEntityType.REPRODUCTION -> database.reproductionQueries.setServerId(serverId, updatedAt, clientId)
            SyncEntityType.REPRO_MEDICATION ->
                database.reproMedicationQueries.setServerId(serverId, updatedAt, clientId)
            SyncEntityType.SUBSTANCE -> database.substanceQueries.setServerId(serverId, updatedAt, clientId)
            SyncEntityType.SURGERY -> database.surgeryQueries.setServerId(serverId, updatedAt, clientId)
            SyncEntityType.ULTRASOUND -> database.ultrasoundQueries.setServerId(serverId, updatedAt, clientId)
            SyncEntityType.VACCINATION -> database.vaccinationQueries.setServerId(serverId, updatedAt, clientId)
            SyncEntityType.WEIGHT -> database.weightQueries.setServerId(serverId, updatedAt, clientId)
            SyncEntityType.CUSTOM_REMINDER ->
                database.customReminderQueries.setServerId(serverId, updatedAt, clientId)
            else -> Unit
        }
    }

    private data class PushOutcome(
        val accepted: Int,
        val rejected: Int,
        val deferred: Int,
    )

    private data class WaveOutcome(
        val accepted: Int = 0,
        val rejected: Int = 0,
        val deferred: Int = 0,
    )

    private data class PullOutcome(
        val applied: Int,
        val serverTimestamp: Instant,
    )
}
