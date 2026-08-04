@file:OptIn(ExperimentalObjCName::class)

package com.github.rodrigotimoteo.animally.bridge

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlin.experimental.ExperimentalObjCName
import kotlin.native.ObjCName

/**
 * Swift-facing wrapper around a [StateFlow].
 *
 * Exposes the current value synchronously and lets the caller subscribe to
 * subsequent emissions. Collection runs on the supplied [scope], so the
 * subscription lifecycle is tied to the owner of that scope.
 */
@ObjCName("NativeFlow")
class NativeFlow<T : Any>(
    private val stateFlow: StateFlow<T>,
    private val scope: CoroutineScope,
) {
    /** The latest emitted value. */
    val current: T
        get() = stateFlow.value

    /**
     * Starts collecting [stateFlow], invoking [onEach] for every emission
     * (including the current value). Returns a [NativeCancellable] that stops
     * collection.
     */
    fun subscribe(onEach: (T) -> Unit): NativeCancellable {
        val job = stateFlow.onEach(onEach).launchIn(scope)
        return NativeCancellable(job)
    }
}
