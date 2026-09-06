package com.github.rodrigotimoteo.animally.domain.notification

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.github.rodrigotimoteo.animally.di.infra.appContext

/**
 * Android notification permission backed by the platform notification manager.
 *
 * From Android 13 (API 33) notifications require the runtime POST_NOTIFICATIONS permission,
 * checked via [ContextCompat.checkSelfPermission]. On older versions the permission is granted
 * by default and [NotificationManagerCompat.areNotificationsEnabled] reflects whether the user
 * has turned notifications off for the app.
 *
 * [request] is a best-effort report of the current state: KMPNotifier's Android permission
 * util cannot show the system dialog from shared code — Android runtime permissions must be
 * launched from an Activity. [AndroidNotificationPermissionBridge] connects that Activity
 * Result callback to the shared state machine. The reminder channel is ensured up front so the
 * UI degrades gracefully instead of crashing when permission is missing.
 */
actual class NotificationPermission {
    actual fun isGranted(): Boolean {
        val runtimeGranted =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(appContext, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        return runtimeGranted && NotificationManagerCompat.from(appContext).areNotificationsEnabled()
    }

    actual suspend fun request(): Boolean {
        ensureReminderChannel(appContext)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || isGranted()) {
            return isGranted()
        }
        return AndroidNotificationPermissionBridge.request() && isGranted()
    }
}
