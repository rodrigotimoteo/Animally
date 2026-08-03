package com.github.rodrigotimoteo.animally.presentation.ui.timeline

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.di.database.createTestDatabase
import com.github.rodrigotimoteo.animally.domain.timeline.usecase.GetTimelineUseCase
import com.github.rodrigotimoteo.animally.presentation.navigation.AnimallyNavigator
import com.github.rodrigotimoteo.animally.presentation.timeline.TimelineViewModel
import com.github.rodrigotimoteo.animally.presentation.timeline.view.TimelineScreen
import com.github.rodrigotimoteo.animally.presentation.ui.ProvideTestLifecycle
import com.github.rodrigotimoteo.animally.presentation.ui.installMainDispatcher
import com.github.rodrigotimoteo.animally.presentation.ui.restoreMainDispatcher
import com.github.rodrigotimoteo.animally.presentation.ui.seedPatient
import com.github.rodrigotimoteo.animally.presentation.ui.seedVaccination
import com.github.rodrigotimoteo.animally.presentation.ui.seedWeight
import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.LocalDate
import kotlin.test.AfterTest
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class TimelineScreenTest {
    @AfterTest
    fun tearDownMainDispatcher() {
        restoreMainDispatcher()
    }

    private fun createViewModel(
        database: AnimallyDatabase,
        patientId: Long?,
    ): TimelineViewModel =
        TimelineViewModel(
            patientId = patientId,
            getTimelineUseCase = GetTimelineUseCase(database),
            animallyNavigator = AnimallyNavigator(),
            ioDispatcher = Dispatchers.IO,
        )

    private fun ComposeUiTest.showTimeline(
        viewModel: TimelineViewModel,
        expectedTitle: String,
        block: ComposeUiTest.() -> Unit,
    ) {
        setContent {
            ProvideTestLifecycle {
                TimelineScreen(viewModel = viewModel)
            }
        }
        waitUntil(timeoutMillis = 5_000) {
            onAllNodesWithText(expectedTitle).fetchSemanticsNodes().isNotEmpty()
        }
        block()
    }

    @Test
    fun `when patient has dated records then entries and date groups are shown`() =
        runComposeUiTest {
            installMainDispatcher()
            val database = createTestDatabase()
            val patient = database.seedPatient(name = "Midnight")
            database.seedVaccination(patientId = patient.id, vaccineName = "Tetanus", date = LocalDate(2024, 5, 1))
            database.seedWeight(patientId = patient.id, weightKg = 520.0, date = LocalDate(2024, 5, 2))
            val viewModel = createViewModel(database, patient.id)

            showTimeline(viewModel, "Timeline") {
                waitUntil(timeoutMillis = 5_000) {
                    onAllNodesWithText("Vaccination").fetchSemanticsNodes().isNotEmpty()
                }
                onNodeWithText("Vaccination").assertIsDisplayed()
                onNodeWithText("Tetanus").assertIsDisplayed()
                onNodeWithText("Weight").assertIsDisplayed()
                onAllNodesWithText("2024-05-01").get(0).assertIsDisplayed()
                onAllNodesWithText("2024-05-02").get(0).assertIsDisplayed()
            }
        }

    @Test
    fun `when patient has no records then empty state is shown`() =
        runComposeUiTest {
            installMainDispatcher()
            val database = createTestDatabase()
            val patient = database.seedPatient(name = "Midnight")
            val viewModel = createViewModel(database, patient.id)

            showTimeline(viewModel, "Timeline") {
                waitUntil(timeoutMillis = 5_000) {
                    onAllNodesWithText("No events yet").fetchSemanticsNodes().isNotEmpty()
                }
                onNodeWithText("No events yet").assertIsDisplayed()
            }
        }

    @Test
    fun `when timeline is global then title reads all patients`() =
        runComposeUiTest {
            installMainDispatcher()
            val database = createTestDatabase()
            val viewModel = createViewModel(database, patientId = null)

            showTimeline(viewModel, "All Patients") {
                onNodeWithText("All Patients").assertIsDisplayed()
                waitUntil(timeoutMillis = 5_000) {
                    onAllNodesWithText("No events yet").fetchSemanticsNodes().isNotEmpty()
                }
                onNodeWithText("No events yet").assertIsDisplayed()
            }
        }
}
