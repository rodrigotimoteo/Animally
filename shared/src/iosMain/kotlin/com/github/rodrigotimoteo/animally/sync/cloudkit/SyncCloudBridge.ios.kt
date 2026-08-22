package com.github.rodrigotimoteo.animally.sync.cloudkit

import kotlinx.cinterop.ExperimentalForeignApi

/**
 * iOS actual of [SyncCloudBridge], backed by the AnimallySyncShim Swift class
 * via the CloudKitShim header-only cinterop.
 *
 * Shim completion errors are surfaced as exceptions rather than swallowed:
 * the engine awaits exported/exportFailed confirmations after staging, so a
 * silently-failed stage call would hang the export lane.
 */
@OptIn(ExperimentalForeignApi::class)
actual class SyncCloudBridge {
    private val shim = AnimallySyncShim()

    actual suspend fun accountStatus(): String = awaitShim { onDone -> shim.accountStatus(onDone) }

    actual suspend fun start() {
        awaitShim { onDone -> shim.start(onDone) }
    }

    actual fun setEventHandler(onEvent: (String) -> Unit) {
        shim.setEventHandler { eventJson -> onEvent(eventJson ?: "") }
    }

    actual suspend fun stageRecords(json: String) {
        awaitShim { onDone -> shim.stageRecords(json, onDone) }
    }

    actual suspend fun fetchChanges() {
        awaitShim { onDone -> shim.fetchChanges(onDone) }
    }

    actual fun stop() {
        shim.stop()
    }

    /**
     * Awaits a shim completion of the form (result, error); throws when the
     * shim reports an error.
     */
    private suspend fun awaitShim(invoke: (onDone: (String?, String?) -> Unit) -> Unit): String =
        kotlinx.coroutines.suspendCancellableCoroutine { continuation ->
            invoke { result, error ->
                if (error != null) {
                    continuation.resumeWith(Result.failure(IllegalStateException(error)))
                } else {
                    continuation.resumeWith(Result.success(result ?: ""))
                }
            }
        }
}

actual fun createSyncCloudBridge(): SyncCloudBridge = SyncCloudBridge()
