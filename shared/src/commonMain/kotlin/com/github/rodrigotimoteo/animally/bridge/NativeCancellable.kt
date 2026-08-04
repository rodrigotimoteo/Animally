@file:OptIn(ExperimentalObjCName::class)

package com.github.rodrigotimoteo.animally.bridge

import kotlinx.coroutines.Job
import kotlin.experimental.ExperimentalObjCName
import kotlin.native.ObjCName

/**
 * Swift-facing cancellation handle returned by [NativeFlow.subscribe].
 */
@ObjCName("NativeCancellable")
class NativeCancellable(
    private val job: Job,
) {
    /** Cancels the underlying collection job. */
    fun cancel() {
        job.cancel()
    }
}
