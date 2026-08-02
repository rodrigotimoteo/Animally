package com.github.rodrigotimoteo.animally.domain.notification

import org.koin.core.annotation.Single

/**
 * Platform notification permission.
 *
 * [isGranted] reports whether notifications are authorized on the current platform and
 * [request] asks the user for authorization, returning the resulting state. The Android
 * actual answers from the platform notification manager directly; the iOS actual delegates
 * to KMPNotifier's permission util.
 */
expect class NotificationPermission() {
    /**
     * Whether the platform has authorized notifications.
     */
    fun isGranted(): Boolean

    /**
     * Requests notification authorization and returns the resulting state.
     */
    suspend fun request(): Boolean
}

/**
 * Abstraction over [NotificationPermission] used by presentation layers.
 *
 * Decouples view models from the platform permission implementation so it can be
 * substituted in tests. The default implementation delegates to [NotificationPermission].
 */
interface NotificationPermissionController {
    /**
     * Whether the platform has authorized notifications.
     */
    fun isGranted(): Boolean

    /**
     * Requests notification authorization and returns the resulting state.
     */
    suspend fun request(): Boolean
}

/**
 * Default [NotificationPermissionController] backed by the platform [NotificationPermission].
 */
@Single
class NotificationPermissionControllerImpl(
    private val delegate: NotificationPermission = NotificationPermission(),
) : NotificationPermissionController {
    override fun isGranted(): Boolean = delegate.isGranted()

    override suspend fun request(): Boolean = delegate.request()
}
