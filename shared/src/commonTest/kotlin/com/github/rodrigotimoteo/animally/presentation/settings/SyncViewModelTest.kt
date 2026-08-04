package com.github.rodrigotimoteo.animally.presentation.settings

import com.github.rodrigotimoteo.animally.domain.sync.SyncEngine
import com.github.rodrigotimoteo.animally.domain.sync.SyncMetadataRepository
import com.github.rodrigotimoteo.animally.domain.sync.SyncResult
import com.github.rodrigotimoteo.animally.domain.sync.SyncUseCase
import dev.mokkery.answering.returns
import dev.mokkery.answering.throws
import dev.mokkery.everySuspend
import dev.mokkery.mock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class SyncViewModelTest {
    private val syncEngineMock: SyncEngine = mock()

    private val syncMetadataRepositoryMock: SyncMetadataRepository = mock()

    private val syncUseCase = SyncUseCase(syncEngineMock)

    private val serverTimestamp = Instant.fromEpochMilliseconds(1_700_000_000_000L)

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(dispatcher: TestDispatcher) =
        SyncViewModel(
            syncUseCase = syncUseCase,
            syncMetadataRepository = syncMetadataRepositoryMock,
            ioDispatcher = dispatcher,
        )

    private fun setupInitMocks(lastSyncAt: Instant) {
        everySuspend { syncMetadataRepositoryMock.getDeviceId() } returns "device-1"
        everySuspend { syncMetadataRepositoryMock.getOrCreateLastSyncAt("device-1") } returns lastSyncAt
    }

    @Test
    fun `init loads last sync timestamp from repository`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            setupInitMocks(serverTimestamp)
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            advanceUntilIdle()

            val state = vm.uiState.value
            assertEquals(serverTimestamp, state.lastSyncAt)
            assertFalse(state.isSyncing)
            assertNull(state.errorMessage)
        }

    @Test
    fun `init sets lastSyncAt to null when epoch zero returned`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            setupInitMocks(Instant.DISTANT_PAST)
            val vm = createViewModel(StandardTestDispatcher(testScheduler))

            advanceUntilIdle()

            assertNull(vm.uiState.value.lastSyncAt)
        }

    @Test
    fun `sync success updates lastSyncAt and clears isSyncing`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            setupInitMocks(Instant.DISTANT_PAST)
            everySuspend { syncEngineMock.sync() } returns
                SyncResult.success(
                    pushedCount = 3,
                    pulledCount = 2,
                    rejectedCount = 0,
                    deferredCount = 0,
                    serverTimestamp = serverTimestamp,
                )
            val vm = createViewModel(StandardTestDispatcher(testScheduler))
            advanceUntilIdle()

            vm.syncNow()
            advanceUntilIdle()

            val state = vm.uiState.value
            assertFalse(state.isSyncing)
            assertEquals(serverTimestamp, state.lastSyncAt)
            assertNotNull(state.lastResult)
            assertTrue(state.lastResult!!.success)
            assertNull(state.errorMessage)
        }

    @Test
    fun `sync failure surfaces error message and clears isSyncing`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            setupInitMocks(Instant.DISTANT_PAST)
            everySuspend { syncEngineMock.sync() } returns
                SyncResult.failure("Network timeout")
            val vm = createViewModel(StandardTestDispatcher(testScheduler))
            advanceUntilIdle()

            vm.syncNow()
            advanceUntilIdle()

            val state = vm.uiState.value
            assertFalse(state.isSyncing)
            assertEquals("Network timeout", state.errorMessage)
            assertNotNull(state.lastResult)
            assertFalse(state.lastResult!!.success)
        }

    @Test
    fun `sync exception surfaces fallback error message`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            setupInitMocks(Instant.DISTANT_PAST)
            everySuspend { syncEngineMock.sync() } throws RuntimeException("Connection refused")
            val vm = createViewModel(StandardTestDispatcher(testScheduler))
            advanceUntilIdle()

            vm.syncNow()
            advanceUntilIdle()

            val state = vm.uiState.value
            assertFalse(state.isSyncing)
            assertEquals("Connection refused", state.errorMessage)
        }

    @Test
    fun `onDismissError clears error message`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            setupInitMocks(Instant.DISTANT_PAST)
            everySuspend { syncEngineMock.sync() } returns SyncResult.failure("Server error")
            val vm = createViewModel(StandardTestDispatcher(testScheduler))
            advanceUntilIdle()

            vm.syncNow()
            advanceUntilIdle()
            assertEquals("Server error", vm.uiState.value.errorMessage)

            vm.onDismissError()

            assertNull(vm.uiState.value.errorMessage)
        }

    @Test
    fun `syncNow while already syncing is ignored`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            setupInitMocks(Instant.DISTANT_PAST)
            everySuspend { syncEngineMock.sync() } returns
                SyncResult.success(
                    pushedCount = 1,
                    pulledCount = 1,
                    rejectedCount = 0,
                    deferredCount = 0,
                    serverTimestamp = serverTimestamp,
                )
            val vm = createViewModel(StandardTestDispatcher(testScheduler))
            advanceUntilIdle()

            vm.syncNow()
            vm.syncNow()
            advanceUntilIdle()

            val state = vm.uiState.value
            assertFalse(state.isSyncing)
            assertEquals(serverTimestamp, state.lastSyncAt)
        }
}
