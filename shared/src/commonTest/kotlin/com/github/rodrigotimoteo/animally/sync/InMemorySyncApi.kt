package com.github.rodrigotimoteo.animally.sync

import com.github.rodrigotimoteo.animally.domain.sync.SyncAccepted
import com.github.rodrigotimoteo.animally.domain.sync.SyncApi
import com.github.rodrigotimoteo.animally.domain.sync.SyncPullResponse
import com.github.rodrigotimoteo.animally.domain.sync.SyncPushRequest
import com.github.rodrigotimoteo.animally.domain.sync.SyncPushResponse
import com.github.rodrigotimoteo.animally.domain.sync.SyncRecord
import com.github.rodrigotimoteo.animally.domain.sync.SyncRejected
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * In-memory [SyncApi] double modelling the sync server semantics.
 *
 * Records are stored keyed by server id. Push assigns `srv-<type>-<clientId>`
 * (or a `srv-<type>-<n>` counter id when the client id is absent) to new
 * records — the type prefix keeps ids unique across tables, since client ids
 * are only unique per table. Updates replace the stored record when incoming
 * `updatedAt` is newer or equal — the server wins ties. Stale pushes (older
 * `updatedAt`) are rejected with reason `"stale"`. Pull returns stored records
 * strictly newer than `since`.
 */
class InMemorySyncApi(
    private val serverClock: () -> Instant = { Clock.System.now() },
) : SyncApi {
    private val mutex = Mutex()
    private val records = mutableMapOf<String, SyncRecord>()
    private var nextId = 1L

    /** Pre-seeds a stored record (must carry a [SyncRecord.serverId]). */
    fun seed(record: SyncRecord) {
        val serverId = requireNotNull(record.serverId) { "seeded records must carry a serverId" }
        records[serverId] = record
    }

    /** Snapshot of every stored record, in insertion order. */
    fun storedRecords(): List<SyncRecord> = records.values.toList()

    override suspend fun pull(since: Instant): SyncPullResponse =
        mutex.withLock {
            SyncPullResponse(
                records = records.values.filter { it.updatedAt > since },
                serverTimestamp = serverClock(),
            )
        }

    override suspend fun push(request: SyncPushRequest): SyncPushResponse =
        mutex.withLock {
            val accepted = mutableListOf<SyncAccepted>()
            val rejected = mutableListOf<SyncRejected>()
            for (record in request.records) {
                val serverId = record.serverId
                if (serverId == null) {
                    val clientId = record.clientId ?: nextId++
                    // Client ids are per-table local, so namespace the assigned id by
                    // entity type to keep server ids globally unique across tables.
                    val assigned = "srv-${record.type}-$clientId"
                    records[assigned] = record.copy(serverId = assigned)
                    accepted += SyncAccepted(record.type, clientId, assigned, record.updatedAt)
                } else {
                    val stored = records[serverId]
                    if (stored == null || record.updatedAt >= stored.updatedAt) {
                        records[serverId] = record
                        accepted += SyncAccepted(record.type, record.clientId ?: 0L, serverId, record.updatedAt)
                    } else {
                        rejected += SyncRejected(record.type, record.clientId ?: 0L, "stale")
                    }
                }
            }
            SyncPushResponse(
                accepted = accepted,
                rejected = rejected,
                serverTimestamp = serverClock(),
            )
        }
}
