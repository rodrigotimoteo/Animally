package com.github.rodrigotimoteo.animally

import com.github.rodrigotimoteo.animally.bridge.NativeFlow
import com.github.rodrigotimoteo.animally.di.database.QueriesModule
import com.github.rodrigotimoteo.animally.di.database.databaseTestModules
import com.github.rodrigotimoteo.animally.di.dispatchers.IO_DISPATCHER
import com.github.rodrigotimoteo.animally.di.infra.AppModule
import com.github.rodrigotimoteo.animally.di.infra.IosAppBridge
import com.github.rodrigotimoteo.animally.di.presentation.PresentationModule
import com.github.rodrigotimoteo.animally.presentation.patientList.PatientListUiState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.koin.core.Koin
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.qualifier.named
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import com.github.rodrigotimoteo.animally.di.infra.module as appModule

/**
 * Tests for the Kotlin/Native facade consumed by SwiftUI.
 *
 * Koin is booted with an in-memory database and test dispatchers so the view
 * model work is deterministic under [runTest].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class IosAppBridgeTest {
    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
        stopKoin()
    }

    private fun startTestKoin(scheduler: TestCoroutineScheduler): Koin {
        val ioDispatcher = UnconfinedTestDispatcher(scheduler)
        return startKoin {
            modules(
                buildList {
                    addAll(databaseTestModules())
                    add(AppModule().appModule())
                    add(QueriesModule().provide())
                    add(PresentationModule().provide())
                    add(
                        module {
                            single<CoroutineDispatcher>(named(IO_DISPATCHER)) { ioDispatcher }
                        },
                    )
                },
            )
        }.koin
    }

    @Test
    fun startBootsKoinWithoutCrashing() =
        runTest {
            Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
            try {
                IosAppBridge.start()
                assertNotNull(IosAppBridge.patientListStore())
            } finally {
                Dispatchers.resetMain()
                stopKoin()
            }
        }

    @Test
    fun patientListStoreEmitsStateAfterLoad() =
        runTest {
            Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
            try {
                IosAppBridge.start(startTestKoin(testScheduler))
                val store = IosAppBridge.patientListStore()

                val received = mutableListOf<PatientListUiState>()
                val cancellable = store.state.subscribe { received += it }

                assertEquals(emptyList(), store.state.current.patients)
                assertFalse(store.state.current.isLoading)
                assertNotNull(received.firstOrNull())
                cancellable.cancel()
            } finally {
                Dispatchers.resetMain()
                stopKoin()
            }
        }

    @Test
    fun patientDetailStoreResolvesWithParameter() =
        runTest {
            Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
            try {
                IosAppBridge.start(startTestKoin(testScheduler))
                val store = IosAppBridge.patientDetailStore(patientId = 42L)

                assertNull(store.state.current.patient)
                assertFalse(store.state.current.isLoading)
                store.dismissError()
                store.load()
            } finally {
                Dispatchers.resetMain()
                stopKoin()
            }
        }

    @Test
    fun nativeFlowSubscribeEmitsCurrentAndSubsequentValues() =
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
    fun nativeFlowCancelStopsEmissions() =
        runTest {
            val flow = MutableStateFlow(0)
            val nativeFlow = NativeFlow(flow, this)
            val received = mutableListOf<Int>()
            val cancellable = nativeFlow.subscribe { received += it }
            advanceUntilIdle()
            assertEquals(listOf(0), received)

            cancellable.cancel()
            flow.value = 1
            advanceUntilIdle()

            assertEquals(listOf(0), received)
        }
}
