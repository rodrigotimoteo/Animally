package com.github.rodrigotimoteo.animally.presentation.ui.ownerlist

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.data.owner.OwnerRepositoryImpl
import com.github.rodrigotimoteo.animally.data.patient.PatientRepositoryImpl
import com.github.rodrigotimoteo.animally.di.database.createTestDatabase
import com.github.rodrigotimoteo.animally.domain.owner.usecase.DeleteOwnerUseCase
import com.github.rodrigotimoteo.animally.domain.owner.usecase.GetOwnerListUseCase
import com.github.rodrigotimoteo.animally.presentation.navigation.AnimallyNavigator
import com.github.rodrigotimoteo.animally.presentation.ownerList.OwnerListViewModel
import com.github.rodrigotimoteo.animally.presentation.ownerList.view.OwnerListScreen
import com.github.rodrigotimoteo.animally.presentation.ui.ProvideTestLifecycle
import com.github.rodrigotimoteo.animally.presentation.ui.installMainDispatcher
import com.github.rodrigotimoteo.animally.presentation.ui.restoreMainDispatcher
import com.github.rodrigotimoteo.animally.presentation.ui.seedOwner
import com.github.rodrigotimoteo.animally.presentation.ui.uiTestIoDispatcher
import kotlin.test.AfterTest
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class OwnerListScreenTest {
    @AfterTest
    fun tearDownMainDispatcher() {
        restoreMainDispatcher()
    }

    private fun createViewModel(database: AnimallyDatabase): OwnerListViewModel {
        val ownerRepository = OwnerRepositoryImpl(database.ownerQueries)
        val patientRepository = PatientRepositoryImpl(database)
        return OwnerListViewModel(
            getOwnerListUseCase = GetOwnerListUseCase(ownerRepository),
            deleteOwnerUseCase = DeleteOwnerUseCase(ownerRepository, patientRepository),
            animallyNavigator = AnimallyNavigator(),
            ioDispatcher = uiTestIoDispatcher(),
        )
    }

    private fun ComposeUiTest.showOwnerList(
        viewModel: OwnerListViewModel,
        block: ComposeUiTest.() -> Unit,
    ) {
        setContent {
            ProvideTestLifecycle {
                OwnerListScreen(viewModel = viewModel)
            }
        }
        waitUntil(timeoutMillis = 5_000) {
            onAllNodesWithText("Owners").fetchSemanticsNodes().isNotEmpty()
        }
        block()
    }

    @Test
    fun `when owner exists then owner name is shown`() =
        runComposeUiTest {
            installMainDispatcher()
            val database = createTestDatabase()
            database.seedOwner(name = "Alice Brown")
            val viewModel = createViewModel(database)

            showOwnerList(viewModel) {
                waitUntil(timeoutMillis = 5_000) {
                    onAllNodesWithText("Alice Brown").fetchSemanticsNodes().isNotEmpty()
                }
                onNodeWithText("Alice Brown").assertIsDisplayed()
            }
        }

    @Test
    fun `when owner exists then add owner button is available`() =
        runComposeUiTest {
            installMainDispatcher()
            val database = createTestDatabase()
            database.seedOwner(name = "Alice Brown")
            val viewModel = createViewModel(database)

            showOwnerList(viewModel) {
                waitUntil(timeoutMillis = 5_000) {
                    onAllNodesWithContentDescription("Add owner").fetchSemanticsNodes().isNotEmpty()
                }
                onNodeWithContentDescription("Add owner").assertIsDisplayed()
            }
        }

    @Test
    fun `when database has no owners then empty state is shown`() =
        runComposeUiTest {
            installMainDispatcher()
            val database = createTestDatabase()
            val viewModel = createViewModel(database)

            showOwnerList(viewModel) {
                waitUntil(timeoutMillis = 5_000) {
                    onAllNodesWithText("No owners yet").fetchSemanticsNodes().isNotEmpty()
                }
                onNodeWithText("No owners yet").assertIsDisplayed()
            }
        }
}
