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
    private fun unsupported(): Nothing = throw UnsupportedOperationException(CLOUDKIT_IOS_ONLY_MESSAGE)

    public actual suspend fun accountStatus(): String {
        throw unsupported()
    }

    // The shared expect API is suspend for the real iOS bridge; Android must
    // keep the same signature even though its CloudKit stub throws immediately.
    @Suppress("kotlin:S6318")
    public actual suspend fun start() {
        throw unsupported()
    }

    public actual fun setEventHandler(onEvent: (String) -> Unit) {
        throw unsupported()
    }

    public actual suspend fun stageRecords(json: String) {
        throw unsupported()
    }

    @Suppress("kotlin:S6318")
    public actual suspend fun fetchChanges() {
        throw unsupported()
    }

    public actual fun stop() {
        throw unsupported()
    }
}

public actual fun createSyncCloudBridge(): SyncCloudBridge = SyncCloudBridge()

private const val CLOUDKIT_IOS_ONLY_MESSAGE = "CloudKit is iOS-only"
