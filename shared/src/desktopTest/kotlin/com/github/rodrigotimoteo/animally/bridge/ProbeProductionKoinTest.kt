package com.github.rodrigotimoteo.animally.bridge

import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.di.database.QueriesModule
import com.github.rodrigotimoteo.animally.di.database.databaseTestModules
import com.github.rodrigotimoteo.animally.di.dispatchers.DispatchersModule
import com.github.rodrigotimoteo.animally.di.dispatchers.IO_DISPATCHER
import com.github.rodrigotimoteo.animally.di.infra.AppModule
import com.github.rodrigotimoteo.animally.di.presentation.PresentationModule
import com.github.rodrigotimoteo.animally.presentation.navigation.AnimallyNavigator
import com.github.rodrigotimoteo.animally.presentation.patientDetail.PatientDetailViewModel
import com.github.rodrigotimoteo.animally.presentation.patientList.PatientListViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.parameter.parametersOf
import org.koin.core.qualifier.named
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertNotNull
import com.github.rodrigotimoteo.animally.di.dispatchers.module as dispatchersModule
import com.github.rodrigotimoteo.animally.di.infra.module as appModule

/**
 * Runtime probe: verifies that loading the generated annotation module makes
 * `@KoinViewModel` and parameterized view models resolvable, alongside the
 * hand-wired modules.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ProbeProductionKoinTest {
    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
        stopKoin()
    }

    @Test
    fun generatedModuleEnablesViewModelResolution() =
        runTest {
            Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
            startKoin {
                modules(
                    buildList {
                        addAll(databaseTestModules())
                        add(AppModule().appModule())
                        add(DispatchersModule().dispatchersModule())
                        add(QueriesModule().provide())
                        add(PresentationModule().provide())
                    },
                )
            }
            val koin = GlobalContext.get()
            assertNotNull(koin.getOrNull<AnimallyDatabase>())
            assertNotNull(koin.getOrNull<AnimallyNavigator>())
            assertNotNull(koin.getOrNull<CoroutineDispatcher>(named(IO_DISPATCHER)))
            assertNotNull(koin.getOrNull<PatientListViewModel>())
            assertNotNull(koin.getOrNull<PatientDetailViewModel> { parametersOf(42L) })
        }
}
