package com.github.rodrigotimoteo.animally

import com.github.rodrigotimoteo.animally.di.database.QueriesModule
import com.github.rodrigotimoteo.animally.di.database.databaseTestModules
import com.github.rodrigotimoteo.animally.di.dispatchers.IO_DISPATCHER
import com.github.rodrigotimoteo.animally.di.infra.AppModule
import com.github.rodrigotimoteo.animally.di.presentation.PresentationModule
import com.github.rodrigotimoteo.animally.domain.notification.NotificationPermissionController
import com.github.rodrigotimoteo.animally.domain.owner.model.Owner
import com.github.rodrigotimoteo.animally.domain.patient.model.Patient
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.koin.core.Koin
import org.koin.core.context.startKoin
import org.koin.core.qualifier.named
import org.koin.dsl.module
import kotlin.time.Instant
import com.github.rodrigotimoteo.animally.di.infra.module as appModule

/**
 * Shared bootstrapping and fixtures for the iOS store tests.
 *
 * Boots the same production module graph the app uses, swapping only the
 * SQLDelight driver for a true in-memory one.
 */
@OptIn(ExperimentalCoroutinesApi::class)
object StoreTestSupport {
    /** Boots Koin over an in-memory database with test dispatchers. */
    fun startKoinWithInMemoryDb(scheduler: TestCoroutineScheduler): Koin {
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
}

/** An [Owner] fixture with deterministic timestamps. */
fun testOwner(
    id: Long = 1L,
    name: String = "Alice",
): Owner =
    Owner(
        id = id,
        name = name,
        email = null,
        phone = null,
        address = null,
        createdAt = Instant.fromEpochMilliseconds(0L),
        updatedAt = Instant.fromEpochMilliseconds(0L),
    )

/** A [Patient] fixture with deterministic timestamps. */
fun testPatient(
    id: Long = 1L,
    name: String = "Thunder",
    ownerId: Long? = null,
): Patient =
    Patient(
        id = id,
        name = name,
        species = "Equine",
        breed = null,
        dateOfBirth = null,
        gender = null,
        microchipId = null,
        ueln = null,
        registrationNumber = null,
        stableLocation = null,
        photoUri = null,
        notes = null,
        cogginsTestDate = null,
        cogginsResult = null,
        cogginsExpiryDate = null,
        ownerId = ownerId,
        isActive = true,
        createdAt = Instant.fromEpochMilliseconds(0L),
        updatedAt = Instant.fromEpochMilliseconds(0L),
    )

/**
 * Deterministic [NotificationPermissionController] fake that never touches the
 * platform notification stack.
 *
 * @param granted The permission result to report.
 */
class FakeNotificationPermissionController(
    private val granted: Boolean,
) : NotificationPermissionController {
    override fun isGranted(): Boolean = granted

    override suspend fun request(): Boolean = granted
}
