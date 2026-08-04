package com.github.rodrigotimoteo.animally.bridge

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class NativeFlowTest {
    @Test
    fun subscribeEmitsCurrentAndSubsequentValues() =
        runTest {
            val flow = MutableStateFlow(1)
            val nativeFlow = NativeFlow(flow, this)
            val received = mutableListOf<Int>()
            val cancellable = nativeFlow.subscribe { received += it }
            advanceUntilIdle()

            assertEquals(1, nativeFlow.current)
            flow.value = 2
            advanceUntilIdle()
            flow.value = 3
            advanceUntilIdle()

            assertEquals(listOf(1, 2, 3), received)
            cancellable.cancel()
        }

    @Test
    fun cancelStopsEmissions() =
        runTest {
            val flow = MutableStateFlow(0)
            val nativeFlow = NativeFlow(flow, this)
            val received = mutableListOf<Int>()
            val cancellable = nativeFlow.subscribe { received += it }
            advanceUntilIdle()
            assertEquals(listOf(0), received)

            cancellable.cancel()
            flow.value = 1
            flow.value = 2
            advanceUntilIdle()

            assertEquals(listOf(0), received)
        }
}
