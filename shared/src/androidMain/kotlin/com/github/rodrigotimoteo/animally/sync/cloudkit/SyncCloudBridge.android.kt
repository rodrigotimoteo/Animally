package com.github.rodrigotimoteo.animally.sync.cloudkit

/**
 * Android stub — CloudKit is iOS-only.
 *
 * Block-bodied throws are deliberate: this Kotlin Gradle plugin version
 * rejects expression-bodied `actual`s whose expression type is [Nothing]
 * ("Return type 'Nothing' needs to be specified explicitly"), so the
 * ExpressionBodyEncoding style rule cannot be satisfied here.
 */
@Suppress("ExpressionBodyEncoding")
public actual class SyncCloudBridge public constructor() {
    private fun unsupported(): Nothing = throw UnsupportedOperationException("CloudKit is iOS-only")

    public actual suspend fun accountStatus(): String {
        throw unsupported()
    }

    public actual suspend fun start() {
        throw unsupported()
    }

    public actual fun setEventHandler(onEvent: (String) -> Unit) {
        throw unsupported()
    }

    public actual suspend fun stageRecords(json: String) {
        throw unsupported()
    }

    public actual suspend fun fetchChanges() {
        throw unsupported()
    }

    public actual fun stop() {
        throw unsupported()
    }
}

public actual fun createSyncCloudBridge(): SyncCloudBridge = SyncCloudBridge()
