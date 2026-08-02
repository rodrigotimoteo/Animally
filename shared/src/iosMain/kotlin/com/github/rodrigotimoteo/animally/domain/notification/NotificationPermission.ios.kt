package com.github.rodrigotimoteo.animally.domain.notification

import com.mmk.kmpnotifier.KMPNotifier
import com.mmk.kmpnotifier.local.LocalNotifications
import com.mmk.kmpnotifier.notification.configuration.NotificationPlatformConfiguration
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * iOS notification permission delegated to KMPNotifier's permission util, which wraps
 * `UNUserNotificationCenter`. The SDK exposes authorization only through callbacks, so
 * [isGranted] bridges the callback into a blocking call — safe here because the view model
 * reads permission off the main dispatcher.
 */
actual class NotificationPermission {
    actual fun isGranted(): Boolean {
        ensureKmpNotifierInitialized()
        return runBlocking {
            suspendCancellableCoroutine { continuation ->
                KMPNotifier.permissionUtil.hasNotificationPermission { granted ->
                    if (continuation.isActive) continuation.resume(granted)
                }
            }
        }
    }

    actual suspend fun request(): Boolean {
        ensureKmpNotifierInitialized()
        return suspendCancellableCoroutine { continuation ->
            KMPNotifier.permissionUtil.askNotificationPermission { granted ->
                if (continuation.isActive) continuation.resume(granted)
            }
        }
    }
}

/**
 * Initializes KMPNotifier once for local notifications.
 *
 * Permission is requested on start through [NotificationPlatformConfiguration.Ios.askNotificationPermissionOnStart].
 */
internal fun ensureKmpNotifierInitialized() {
    if (KMPNotifier.isInitialized) return
    KMPNotifier.initialize(
        NotificationPlatformConfiguration.Ios(
            showPushNotification = true,
            askNotificationPermissionOnStart = true,
            notificationSoundName = null,
        ),
        LocalNotifications,
    )
}
