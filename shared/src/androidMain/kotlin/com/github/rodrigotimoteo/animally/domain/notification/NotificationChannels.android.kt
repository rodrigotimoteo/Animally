package com.github.rodrigotimoteo.animally.domain.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

/**
 * Ensures the [REMINDER_CHANNEL_ID] notification channel exists with low importance.
 *
 * Android 8.0+ (API 26) requires a channel before notifications can be posted. KMPNotifier
 * creates its channel lazily on the first `notify`, so this guarantees the channel at app
 * startup. Note that KMPNotifier's channel factory re-creates the same channel at high
 * importance when the first notification is posted.
 */
fun ensureReminderChannel(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val channel =
        NotificationChannel(REMINDER_CHANNEL_ID, "Reminders", NotificationManager.IMPORTANCE_LOW)
    channel.description = "Vaccination and dentistry due-date reminders"
    manager.createNotificationChannel(channel)
}
