package com.github.rodrigotimoteo.animally.domain.notification

import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Activity-owned bridge for Android's runtime notification permission prompt.
 *
 * Shared code cannot launch an Activity Result contract. [register] is called by the
 * Activity, while [request] suspends until the Activity reports the result.
 */
object AndroidNotificationPermissionBridge {
    private val lock = Any()
    private var launchRequest: (() -> Unit)? = null
    private var pendingRequest: CancellableContinuation<Boolean>? = null

    /** Installs the Activity Result launcher used by [request]. */
    fun register(launchRequest: () -> Unit) {
        synchronized(lock) {
            this.launchRequest = launchRequest
        }
    }

    /** Clears the launcher and resolves an in-flight request as denied. */
    fun unregister() {
        val continuation =
            synchronized(lock) {
                launchRequest = null
                pendingRequest.also { pendingRequest = null }
            }
        continuation?.takeIf { it.isActive }?.resume(false)
    }

    /** Suspends until the registered Activity reports the permission result. */
    suspend fun request(): Boolean =
        suspendCancellableCoroutine { continuation ->
            var alreadyPending = false
            val launcher =
                synchronized(lock) {
                    if (pendingRequest != null) {
                        alreadyPending = true
                        null
                    } else {
                        pendingRequest = continuation
                        launchRequest
                    }
                }
            if (alreadyPending) {
                continuation.resume(false)
                return@suspendCancellableCoroutine
            }
            continuation.invokeOnCancellation {
                synchronized(lock) {
                    if (pendingRequest === continuation) {
                        pendingRequest = null
                    }
                }
            }
            if (launcher == null) {
                complete(false)
                return@suspendCancellableCoroutine
            }
            Handler(Looper.getMainLooper()).post {
                val stillPending = synchronized(lock) { pendingRequest === continuation }
                if (stillPending) {
                    runCatching { launcher() }
                        .onFailure {
                            complete(false)
                        }
                }
            }
        }

    /** Completes the request from the Activity Result callback. */
    fun complete(granted: Boolean) {
        val continuation = synchronized(lock) { pendingRequest.also { pendingRequest = null } }
        continuation?.takeIf { it.isActive }?.resume(granted)
    }
}
