package com.github.rodrigotimoteo.animally.sync.cloudkit

/**
 * Desktop stub — CloudKit is iOS-only.
 */
public actual class SyncCloudBridge public constructor() {
    public actual suspend fun accountStatus(): String = throw unsupported()

    public actual suspend fun start() = throw unsupported()

    public actual fun setEventHandler(onEvent: (String) -> Unit) = throw unsupported()

    public actual suspend fun stageRecords(json: String) = throw unsupported()

    public actual suspend fun fetchChanges() = throw unsupported()

    public actual fun stop() = throw unsupported()

    private fun unsupported() = UnsupportedOperationException("CloudKit is iOS-only")
}

public actual fun createSyncCloudBridge(): SyncCloudBridge = throw UnsupportedOperationException("CloudKit is iOS-only")
