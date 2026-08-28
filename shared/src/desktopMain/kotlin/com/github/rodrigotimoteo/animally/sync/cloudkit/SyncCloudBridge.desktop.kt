package com.github.rodrigotimoteo.animally.sync.cloudkit

/**
 * Desktop stub — CloudKit is iOS-only.
 */
public actual class SyncCloudBridge public constructor() {
    public actual suspend fun accountStatus(): String = unsupportedString()

    public actual suspend fun start(): Unit = unsupportedUnit()

    public actual fun setEventHandler(onEvent: (String) -> Unit): Unit = unsupportedUnit()

    public actual suspend fun stageRecords(json: String): Unit = unsupportedUnit()

    public actual suspend fun fetchChanges(): Unit = unsupportedUnit()

    public actual fun stop(): Unit = unsupportedUnit()

    private fun unsupportedString(): String = throw UnsupportedOperationException(CLOUDKIT_IOS_ONLY_MESSAGE)

    private fun unsupportedUnit(): Unit = throw UnsupportedOperationException(CLOUDKIT_IOS_ONLY_MESSAGE)
}

public actual fun createSyncCloudBridge(): SyncCloudBridge = throw UnsupportedOperationException(CLOUDKIT_IOS_ONLY_MESSAGE)

private const val CLOUDKIT_IOS_ONLY_MESSAGE = "CloudKit is iOS-only"
