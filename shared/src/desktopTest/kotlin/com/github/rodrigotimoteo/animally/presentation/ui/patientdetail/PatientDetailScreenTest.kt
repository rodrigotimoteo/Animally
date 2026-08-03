package com.github.rodrigotimoteo.animally.presentation.ui.patientdetail

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.data.owner.OwnerRepositoryImpl
import com.github.rodrigotimoteo.animally.data.patient.PatientRepositoryImpl
import com.github.rodrigotimoteo.animally.domain.owner.usecase.GetOwnerDetailUseCase
import com.github.rodrigotimoteo.animally.domain.patient.usecase.GetPatientDetailUseCase
import com.github.rodrigotimoteo.animally.presentation.navigation.AnimallyNavigator
import com.github.rodrigotimoteo.animally.presentation.patientDetail.PatientDetailViewModel
import com.github.rodrigotimoteo.animally.presentation.patientDetail.view.PatientDetailScreen
import com.github.rodrigotimoteo.animally.presentation.ui.ProvideTestLifecycle
import com.github.rodrigotimoteo.animally.presentation.ui.installMainDispatcher
import com.github.rodrigotimoteo.animally.presentation.ui.restoreMainDispatcher
import com.github.rodrigotimoteo.animally.presentation.ui.seedPatient
import com.github.rodrigotimoteo.animally.presentation.ui.seedVaccination
import com.github.rodrigotimoteo.animally.presentation.ui.seedWeight
import com.github.rodrigotimoteo.animally.presentation.ui.uiTestKoinModules
import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.LocalDate
import org.koin.compose.KoinContext
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import kotlin.test.AfterTest
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class PatientDetailScreenTest {
    @AfterTest
    fun tearDown() {
        stopKoin()
        restoreMainDispatcher()
    }

    private fun startTestKoin(): AnimallyDatabase {
        startKoin { modules(uiTestKoinModules()) }
        return GlobalContext.get().get(AnimallyDatabase::class)
    }

    private fun createViewModel(
        database: AnimallyDatabase,
        patientId: Long,
    ): PatientDetailViewModel {
        val patientRepository = PatientRepositoryImpl(database)
        val ownerRepository = OwnerRepositoryImpl(database.ownerQueries)
        return PatientDetailViewModel(
            patientId = patientId,
            getPatientDetailUseCase = GetPatientDetailUseCase(patientRepository),
            getOwnerDetailUseCase = GetOwnerDetailUseCase(ownerRepository),
            animallyNavigator = AnimallyNavigator(),
            ioDispatcher = Dispatchers.IO,
        )
    }

    private fun ComposeUiTest.showDetailScreen(
        viewModel: PatientDetailViewModel,
        block: ComposeUiTest.() -> Unit,
    ) {
        setContent {
            KoinContext {
                ProvideTestLifecycle {
                    PatientDetailScreen(viewModel = viewModel)
                }
            }
        }
        waitUntil(timeoutMillis = 5_000) {
            onAllNodesWithText("Overview").fetchSemanticsNodes().isNotEmpty()
        }
        block()
    }

    @Test
    fun `when patient loaded then tab bar shows all five tabs`() =
        runComposeUiTest {
            installMainDispatcher()
            val database = startTestKoin()
            val patient = database.seedPatient(name = "Midnight")
            val viewModel = createViewModel(database, patient.id)

            showDetailScreen(viewModel) {
                onAllNodesWithText("Midnight").get(0).assertIsDisplayed()
                listOf("Overview", "Medical", "Preventive", "Reproduction", "Diagnostics/Files").forEach { tab ->
                    onNodeWithText(tab).assertIsDisplayed()
                }
            }
        }

    @Test
    fun `when preventive tab selected then vaccination entry is shown`() =
        runComposeUiTest {
            installMainDispatcher()
            val database = startTestKoin()
            val patient = database.seedPatient(name = "Midnight")
            database.seedVaccination(patientId = patient.id, vaccineName = "Tetanus", date = LocalDate(2024, 5, 1))
            val viewModel = createViewModel(database, patient.id)

            showDetailScreen(viewModel) {
                onNodeWithText("Preventive").performClick()
                waitUntil(timeoutMillis = 5_000) {
                    onAllNodesWithText("Tetanus").fetchSemanticsNodes().isNotEmpty()
                }
                onNodeWithText("Tetanus").assertIsDisplayed()
            }
        }

    @Test
    fun `when overview tab is open then weight entry is shown`() =
        runComposeUiTest {
            installMainDispatcher()
            val database = startTestKoin()
            val patient = database.seedPatient(name = "Midnight")
            database.seedWeight(patientId = patient.id, weightKg = 520.0, date = LocalDate(2024, 5, 2))
            val viewModel = createViewModel(database, patient.id)

            showDetailScreen(viewModel) {
                waitUntil(timeoutMillis = 5_000) {
                    onAllNodesWithText("520.0 kg", substring = true).fetchSemanticsNodes().isNotEmpty()
                }
                onNodeWithText("520.0 kg", substring = true).assertIsDisplayed()
            }
        }
}
