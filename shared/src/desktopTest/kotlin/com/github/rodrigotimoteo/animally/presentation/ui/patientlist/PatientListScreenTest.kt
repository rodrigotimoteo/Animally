package com.github.rodrigotimoteo.animally.presentation.ui.patientlist

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.data.patient.PatientRepositoryImpl
import com.github.rodrigotimoteo.animally.di.database.createTestDatabase
import com.github.rodrigotimoteo.animally.domain.patient.usecase.DeletePatientUseCase
import com.github.rodrigotimoteo.animally.domain.patient.usecase.GetPatientListUseCase
import com.github.rodrigotimoteo.animally.domain.search.ISearchRepository
import com.github.rodrigotimoteo.animally.presentation.navigation.AnimallyNavigator
import com.github.rodrigotimoteo.animally.presentation.patientList.PatientListViewModel
import com.github.rodrigotimoteo.animally.presentation.patientList.view.PatientListScreen
import com.github.rodrigotimoteo.animally.presentation.ui.ProvideTestLifecycle
import com.github.rodrigotimoteo.animally.presentation.ui.installMainDispatcher
import com.github.rodrigotimoteo.animally.presentation.ui.restoreMainDispatcher
import com.github.rodrigotimoteo.animally.presentation.ui.seedPatient
import com.github.rodrigotimoteo.animally.presentation.ui.uiTestIoDispatcher
import dev.mokkery.MockMode
import dev.mokkery.mock
import kotlin.test.AfterTest
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class PatientListScreenTest {
    @AfterTest
    fun tearDownMainDispatcher() {
        restoreMainDispatcher()
    }

    private fun createViewModel(database: AnimallyDatabase): PatientListViewModel {
        val patientRepository = PatientRepositoryImpl(database)
        val searchRepository: ISearchRepository = mock(MockMode.autoUnit)
        return PatientListViewModel(
            getPatientListUseCase = GetPatientListUseCase(patientRepository),
            deletePatientUseCase = DeletePatientUseCase(patientRepository, searchRepository),
            animallyNavigator = AnimallyNavigator(),
            ioDispatcher = uiTestIoDispatcher(),
        )
    }

    @Test
    fun `when patient exists then patient name is shown`() =
        runComposeUiTest {
            installMainDispatcher()
            val database = createTestDatabase()
            database.seedPatient(name = "Midnight")
            val viewModel = createViewModel(database)

            setContent {
                ProvideTestLifecycle {
                    PatientListScreen(viewModel = viewModel)
                }
            }

            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText("Midnight").fetchSemanticsNodes().isNotEmpty()
            }
            onNodeWithText("Midnight").assertIsDisplayed()
        }

    @Test
    fun `when patient exists then add button is available`() =
        runComposeUiTest {
            installMainDispatcher()
            val database = createTestDatabase()
            database.seedPatient(name = "Midnight")
            val viewModel = createViewModel(database)

            setContent {
                ProvideTestLifecycle {
                    PatientListScreen(viewModel = viewModel)
                }
            }

            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithContentDescription("Add patient").fetchSemanticsNodes().isNotEmpty()
            }
            onNodeWithContentDescription("Add patient").assertIsDisplayed()
        }

    @Test
    fun `when patient exists then delete button is shown for patient`() =
        runComposeUiTest {
            installMainDispatcher()
            val database = createTestDatabase()
            database.seedPatient(name = "Midnight")
            val viewModel = createViewModel(database)

            setContent {
                ProvideTestLifecycle {
                    PatientListScreen(viewModel = viewModel)
                }
            }

            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithContentDescription("Delete Midnight").fetchSemanticsNodes().isNotEmpty()
            }
            onNodeWithContentDescription("Delete Midnight").assertIsDisplayed()
        }

    @Test
    fun `when database is empty then empty state is shown`() =
        runComposeUiTest {
            installMainDispatcher()
            val database = createTestDatabase()
            val viewModel = createViewModel(database)

            setContent {
                ProvideTestLifecycle {
                    PatientListScreen(viewModel = viewModel)
                }
            }

            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText("No patients yet").fetchSemanticsNodes().isNotEmpty()
            }
            onNodeWithText("No patients yet").assertIsDisplayed()
            onNodeWithText("Add your first horse to start tracking care.").assertIsDisplayed()
        }
}
