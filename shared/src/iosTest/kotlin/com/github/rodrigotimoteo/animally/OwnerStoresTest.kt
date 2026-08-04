package com.github.rodrigotimoteo.animally

import com.github.rodrigotimoteo.animally.di.infra.IosAppBridge
import com.github.rodrigotimoteo.animally.domain.owner.IOwnerRepository
import com.github.rodrigotimoteo.animally.domain.patient.IPatientRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.koin.core.context.stopKoin
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for the Swift-facing owner stores ([OwnerListStore], [OwnerDetailStore]
 * and [OwnerEditStore]) against a real in-memory database.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class OwnerStoresTest {
    private val scheduler = TestCoroutineScheduler()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher(scheduler))
        IosAppBridge.start(StoreTestSupport.startKoinWithInMemoryDb(scheduler))
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
        stopKoin()
    }

    @Test
    fun `owner list store emits persisted owners after load`() =
        runTest(scheduler) {
            val ownerId = IosAppBridge.koin.get<IOwnerRepository>().insertOwner(testOwner(name = "Alice"))
            val store = IosAppBridge.ownerListStore()

            advanceUntilIdle()

            assertEquals(
                listOf("Alice"),
                store.state.current.owners
                    .map { it.name },
            )
            assertTrue(
                store.state.current.owners
                    .single()
                    .id == ownerId,
            )
            assertEquals(false, store.state.current.isLoading)
        }

    @Test
    fun `owner list store delete removes owner from state`() =
        runTest(scheduler) {
            val ownerId = IosAppBridge.koin.get<IOwnerRepository>().insertOwner(testOwner(name = "Alice"))
            val store = IosAppBridge.ownerListStore()
            advanceUntilIdle()
            assertEquals(1, store.state.current.owners.size)

            store.deleteOwner(ownerId)
            advanceUntilIdle()

            assertEquals(emptyList(), store.state.current.owners)
            assertNull(store.state.current.errorMessage)
        }

    @Test
    fun `owner list store delete blocked surfaces error and dismisses it`() =
        runTest(scheduler) {
            val ownerId = IosAppBridge.koin.get<IOwnerRepository>().insertOwner(testOwner(name = "Alice"))
            IosAppBridge.koin.get<IPatientRepository>().insertPatient(testPatient(ownerId = ownerId))
            val store = IosAppBridge.ownerListStore()
            advanceUntilIdle()

            store.deleteOwner(ownerId)
            advanceUntilIdle()

            assertNotNull(store.state.current.errorMessage)
            assertTrue(
                store.state.current.errorMessage!!
                    .contains("Unassign first."),
            )

            store.dismissError()

            assertNull(store.state.current.errorMessage)
        }

    @Test
    fun `owner detail store loads owner and linked patients`() =
        runTest(scheduler) {
            val ownerId = IosAppBridge.koin.get<IOwnerRepository>().insertOwner(testOwner(name = "Alice"))
            IosAppBridge.koin.get<IPatientRepository>().insertPatient(testPatient(name = "Thunder", ownerId = ownerId))
            val store = IosAppBridge.ownerDetailStore(ownerId)

            advanceUntilIdle()

            assertEquals(
                "Alice",
                store.state.current.owner
                    ?.name,
            )
            assertEquals(
                listOf("Thunder"),
                store.state.current.patients
                    .map { it.name },
            )
            assertEquals(false, store.state.current.isLoading)
        }

    @Test
    fun `owner detail store unknown owner stays empty after load`() =
        runTest(scheduler) {
            val store = IosAppBridge.ownerDetailStore(999L)

            store.load()
            advanceUntilIdle()

            assertNull(store.state.current.owner)
            assertEquals(emptyList(), store.state.current.patients)
        }

    @Test
    fun `owner edit store loads existing owner form`() =
        runTest(scheduler) {
            val ownerId = IosAppBridge.koin.get<IOwnerRepository>().insertOwner(testOwner(name = "Alice"))
            val store = IosAppBridge.ownerEditStore(ownerId)

            advanceUntilIdle()

            assertEquals(
                "Alice",
                store.state.current.form
                    ?.name,
            )
            assertEquals(
                false,
                store.state.current.form
                    ?.isLoading,
            )
        }

    @Test
    fun `owner edit store name change updates state`() =
        runTest(scheduler) {
            val store = IosAppBridge.ownerEditStore(ownerId = null)

            advanceUntilIdle()
            store.onNameChange("Bob")
            store.onPhoneChange("555-0100")

            assertEquals(
                "Bob",
                store.state.current.form
                    ?.name,
            )
            assertEquals(
                "555-0100",
                store.state.current.form
                    ?.phone,
            )
        }

    @Test
    fun `owner edit store save persists new owner`() =
        runTest(scheduler) {
            val store = IosAppBridge.ownerEditStore(ownerId = null)
            advanceUntilIdle()

            store.onNameChange("Carol")
            store.save()
            advanceUntilIdle()

            val owners = IosAppBridge.koin.get<IOwnerRepository>().getOwnerList()
            assertEquals(listOf("Carol"), owners.map { it.name })
        }
}
