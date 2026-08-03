@file:Suppress("ktlint:standard:filename")

package com.github.rodrigotimoteo.animally.domain.notification

/**
 * Desktop has no notification permission model — always granted.
 */
actual class NotificationPermission {
    actual fun isGranted(): Boolean = true

    actual suspend fun request(): Boolean = true
}
